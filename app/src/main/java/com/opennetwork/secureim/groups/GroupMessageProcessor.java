package com.opennetwork.secureim.groups;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.ByteString;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.crypto.MasterSecretUnion;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.EncryptingSmsDatabase;
import com.opennetwork.secureim.database.GroupDatabase;
import com.opennetwork.secureim.database.MessagingDatabase.InsertResult;
import com.opennetwork.secureim.database.MmsDatabase;
import com.opennetwork.secureim.jobs.AvatarDownloadJob;
import com.opennetwork.secureim.jobs.PushGroupUpdateJob;
import com.opennetwork.secureim.mms.MmsException;
import com.opennetwork.secureim.mms.OutgoingGroupMediaMessage;
import com.opennetwork.secureim.notifications.MessageNotifier;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.sms.IncomingGroupMessage;
import com.opennetwork.secureim.sms.IncomingTextMessage;
import com.opennetwork.secureim.util.Base64;
import com.opennetwork.secureim.util.GroupUtil;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceDataMessage;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceEnvelope;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup.Type;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.opennetwork.secureim.database.GroupDatabase.GroupRecord;
import static com.opennetwork.imservice.internal.push.OpenNetworkServiceProtos.AttachmentPointer;
import static com.opennetwork.imservice.internal.push.OpenNetworkServiceProtos.GroupContext;

public class GroupMessageProcessor {

  private static final String TAG = GroupMessageProcessor.class.getSimpleName();

  public static @Nullable Long process(@NonNull Context context,
                                       @NonNull MasterSecretUnion masterSecret,
                                       @NonNull OpenNetworkServiceEnvelope envelope,
                                       @NonNull OpenNetworkServiceDataMessage message,
                                       boolean outgoing)
  {
    if (!message.getGroupInfo().isPresent() || message.getGroupInfo().get().getGroupId() == null) {
      Log.w(TAG, "Received group message with no id! Ignoring...");
      return null;
    }

    GroupDatabase         database = DatabaseFactory.getGroupDatabase(context);
    OpenNetworkServiceGroup    group    = message.getGroupInfo().get();
    String                id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    Optional<GroupRecord> record   = database.getGroup(id);

    if (record.isPresent() && group.getType() == Type.UPDATE) {
      return handleGroupUpdate(context, masterSecret, envelope, group, record.get(), outgoing);
    } else if (!record.isPresent() && group.getType() == Type.UPDATE) {
      return handleGroupCreate(context, masterSecret, envelope, group, outgoing);
    } else if (record.isPresent() && group.getType() == Type.QUIT) {
      return handleGroupLeave(context, masterSecret, envelope, group, record.get(), outgoing);
    } else if (record.isPresent() && group.getType() == Type.REQUEST_INFO) {
      return handleGroupInfoRequest(context, envelope, group, record.get());
    } else {
      Log.w(TAG, "Received unknown type, ignoring...");
      return null;
    }
  }

  private static @Nullable Long handleGroupCreate(@NonNull Context context,
                                                  @NonNull MasterSecretUnion masterSecret,
                                                  @NonNull OpenNetworkServiceEnvelope envelope,
                                                  @NonNull OpenNetworkServiceGroup group,
                                                  boolean outgoing)
  {
    GroupDatabase        database = DatabaseFactory.getGroupDatabase(context);
    String               id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    GroupContext.Builder builder  = createGroupContext(group);
    builder.setType(GroupContext.Type.UPDATE);

    OpenNetworkServiceAttachment avatar  = group.getAvatar().orNull();
    List<Address>           members = group.getMembers().isPresent() ? new LinkedList<Address>() : null;

    if (group.getMembers().isPresent()) {
      for (String member : group.getMembers().get()) {
        members.add(Address.fromExternal(context, member));
      }
    }

    database.create(id, group.getName().orNull(), members,
                    avatar != null && avatar.isPointer() ? avatar.asPointer() : null,
                    envelope.getRelay());

    return storeMessage(context, masterSecret, envelope, group, builder.build(), outgoing);
  }

  private static @Nullable Long handleGroupUpdate(@NonNull Context context,
                                                  @NonNull MasterSecretUnion masterSecret,
                                                  @NonNull OpenNetworkServiceEnvelope envelope,
                                                  @NonNull OpenNetworkServiceGroup group,
                                                  @NonNull GroupRecord groupRecord,
                                                  boolean outgoing)
  {

    GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
    String        id       = GroupUtil.getEncodedId(group.getGroupId(), false);

    Set<Address> recordMembers = new HashSet<>(groupRecord.getMembers());
    Set<Address> messageMembers = new HashSet<>();

    for (String messageMember : group.getMembers().get()) {
      messageMembers.add(Address.fromExternal(context, messageMember));
    }

    Set<Address> addedMembers = new HashSet<>(messageMembers);
    addedMembers.removeAll(recordMembers);

    Set<Address> missingMembers = new HashSet<>(recordMembers);
    missingMembers.removeAll(messageMembers);

    GroupContext.Builder builder = createGroupContext(group);
    builder.setType(GroupContext.Type.UPDATE);

    if (addedMembers.size() > 0) {
      Set<Address> unionMembers = new HashSet<>(recordMembers);
      unionMembers.addAll(messageMembers);
      database.updateMembers(id, new LinkedList<>(unionMembers));

      builder.clearMembers();

      for (Address addedMember : addedMembers) {
        builder.addMembers(addedMember.serialize());
      }
    } else {
      builder.clearMembers();
    }

    if (missingMembers.size() > 0) {
      // TODO We should tell added and missing about each-other.
    }

    if (group.getName().isPresent() || group.getAvatar().isPresent()) {
      OpenNetworkServiceAttachment avatar = group.getAvatar().orNull();
      database.update(id, group.getName().orNull(), avatar != null ? avatar.asPointer() : null);
    }

    if (group.getName().isPresent() && group.getName().get().equals(groupRecord.getTitle())) {
      builder.clearName();
    }

    if (!groupRecord.isActive()) database.setActive(id, true);

    return storeMessage(context, masterSecret, envelope, group, builder.build(), outgoing);
  }

  private static Long handleGroupInfoRequest(@NonNull Context context,
                                             @NonNull OpenNetworkServiceEnvelope envelope,
                                             @NonNull OpenNetworkServiceGroup group,
                                             @NonNull GroupRecord record)
  {
    if (record.getMembers().contains(Address.fromExternal(context, envelope.getSource()))) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new PushGroupUpdateJob(context, envelope.getSource(), group.getGroupId()));
    }

    return null;
  }

  private static Long handleGroupLeave(@NonNull Context               context,
                                       @NonNull MasterSecretUnion     masterSecret,
                                       @NonNull OpenNetworkServiceEnvelope envelope,
                                       @NonNull OpenNetworkServiceGroup    group,
                                       @NonNull GroupRecord           record,
                                       boolean  outgoing)
  {
    GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
    String        id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    List<Address> members  = record.getMembers();

    GroupContext.Builder builder = createGroupContext(group);
    builder.setType(GroupContext.Type.QUIT);

    if (members.contains(Address.fromExternal(context, envelope.getSource()))) {
      database.remove(id, Address.fromExternal(context, envelope.getSource()));
      if (outgoing) database.setActive(id, false);

      return storeMessage(context, masterSecret, envelope, group, builder.build(), outgoing);
    }

    return null;
  }


  private static @Nullable Long storeMessage(@NonNull Context context,
                                             @NonNull MasterSecretUnion masterSecret,
                                             @NonNull OpenNetworkServiceEnvelope envelope,
                                             @NonNull OpenNetworkServiceGroup group,
                                             @NonNull GroupContext storage,
                                             boolean  outgoing)
  {
    if (group.getAvatar().isPresent()) {
      ApplicationContext.getInstance(context).getJobManager()
                        .add(new AvatarDownloadJob(context, group.getGroupId()));
    }

    try {
      if (outgoing) {
        MmsDatabase               mmsDatabase     = DatabaseFactory.getMmsDatabase(context);
        Address                   addres          = Address.fromExternal(context, GroupUtil.getEncodedId(group.getGroupId(), false));
        Recipient                 recipient       = Recipient.from(context, addres, false);
        OutgoingGroupMediaMessage outgoingMessage = new OutgoingGroupMediaMessage(recipient, storage, null, envelope.getTimestamp(), 0);
        long                      threadId        = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
        long                      messageId       = mmsDatabase.insertMessageOutbox(masterSecret, outgoingMessage, threadId, false, null);

        mmsDatabase.markAsSent(messageId, true);

        return threadId;
      } else {
        EncryptingSmsDatabase smsDatabase  = DatabaseFactory.getEncryptingSmsDatabase(context);
        String                body         = Base64.encodeBytes(storage.toByteArray());
        IncomingTextMessage   incoming     = new IncomingTextMessage(Address.fromExternal(context, envelope.getSource()), envelope.getSourceDevice(), envelope.getTimestamp(), body, Optional.of(group), 0);
        IncomingGroupMessage  groupMessage = new IncomingGroupMessage(incoming, storage, body);

        Optional<InsertResult> insertResult = smsDatabase.insertMessageInbox(masterSecret, groupMessage);

        if (insertResult.isPresent()) {
          MessageNotifier.updateNotification(context, masterSecret.getMasterSecret().orNull(), insertResult.get().getThreadId());
          return insertResult.get().getThreadId();
        } else {
          return null;
        }
      }
    } catch (MmsException e) {
      Log.w(TAG, e);
    }

    return null;
  }

  private static GroupContext.Builder createGroupContext(OpenNetworkServiceGroup group) {
    GroupContext.Builder builder = GroupContext.newBuilder();
    builder.setId(ByteString.copyFrom(group.getGroupId()));

    if (group.getAvatar().isPresent() && group.getAvatar().get().isPointer()) {
      builder.setAvatar(AttachmentPointer.newBuilder()
                                         .setId(group.getAvatar().get().asPointer().getId())
                                         .setKey(ByteString.copyFrom(group.getAvatar().get().asPointer().getKey()))
                                         .setContentType(group.getAvatar().get().getContentType()));
    }

    if (group.getName().isPresent()) {
      builder.setName(group.getName().get());
    }

    if (group.getMembers().isPresent()) {
      builder.addAllMembers(group.getMembers().get());
    }

    return builder;
  }

}
