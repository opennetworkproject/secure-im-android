package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.annimon.stream.Stream;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.opennetwork.secureim.database.MmsDatabase;
import com.opennetwork.secureim.database.NoSuchMessageException;
import com.opennetwork.secureim.database.documents.NetworkFailure;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.mms.MediaConstraints;
import com.opennetwork.secureim.mms.MmsException;
import com.opennetwork.secureim.mms.OutgoingGroupMediaMessage;
import com.opennetwork.secureim.mms.OutgoingMediaMessage;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.recipients.RecipientFormattingException;
import com.opennetwork.secureim.transport.UndeliverableMessageException;
import com.opennetwork.secureim.util.GroupUtil;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceDataMessage;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;
import com.opennetwork.imservice.api.push.exceptions.EncapsulatedExceptions;
import com.opennetwork.imservice.api.push.exceptions.NetworkFailureException;
import com.opennetwork.imservice.api.util.InvalidNumberException;
import com.opennetwork.imservice.internal.push.OpenNetworkServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class PushGroupSendJob extends PushSendJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = PushGroupSendJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final long   messageId;
  private final long   filterRecipientId; // Deprecated
  private final String filterAddress;

  public PushGroupSendJob(Context context, long messageId, @NonNull Address destination, @Nullable Address filterAddress) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withGroupId(destination.toGroupString())
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withRetryCount(5)
                                .create());

    this.messageId         = messageId;
    this.filterAddress     = filterAddress == null ? null :filterAddress.toPhoneString();
    this.filterRecipientId = -1;
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onPushSend(MasterSecret masterSecret)
      throws MmsException, IOException, NoSuchMessageException
  {
    MmsDatabase          database = DatabaseFactory.getMmsDatabase(context);
    OutgoingMediaMessage message  = database.getOutgoingMessage(masterSecret, messageId);

    try {
      deliver(masterSecret, message, filterAddress == null ? null : Address.fromSerialized(filterAddress));

      database.markAsSent(messageId, true);
      markAttachmentsUploaded(messageId, message.getAttachments());

      if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
        database.markExpireStarted(messageId);
        ApplicationContext.getInstance(context)
                          .getExpiringMessageManager()
                          .scheduleDeletion(messageId, true, message.getExpiresIn());
      }
    } catch (InvalidNumberException | RecipientFormattingException | UndeliverableMessageException e) {
      Log.w(TAG, e);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    } catch (EncapsulatedExceptions e) {
      Log.w(TAG, e);
      List<NetworkFailure> failures = new LinkedList<>();

      for (NetworkFailureException nfe : e.getNetworkExceptions()) {
        failures.add(new NetworkFailure(Address.fromSerialized(nfe.getE164number())));
      }

      for (UntrustedIdentityException uie : e.getUntrustedIdentityExceptions()) {
        database.addMismatchedIdentity(messageId, Address.fromSerialized(uie.getE164Number()), uie.getIdentityKey());
      }

      database.addFailures(messageId, failures);

      if (e.getNetworkExceptions().isEmpty() && e.getUntrustedIdentityExceptions().isEmpty()) {
        database.markAsSent(messageId, true);
        markAttachmentsUploaded(messageId, message.getAttachments());
      } else {
        database.markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
      }
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
  }

  private void deliver(MasterSecret masterSecret, OutgoingMediaMessage message, @Nullable Address filterAddress)
      throws IOException, RecipientFormattingException, InvalidNumberException,
      EncapsulatedExceptions, UndeliverableMessageException
  {
    String                        groupId           = message.getRecipient().getAddress().toGroupString();
    Optional<byte[]>              profileKey        = getProfileKey(message.getRecipient());
    List<Address>                 recipients        = getGroupMessageRecipients(groupId, messageId);
    MediaConstraints              mediaConstraints  = MediaConstraints.getPushMediaConstraints();
    List<Attachment>              scaledAttachments = scaleAttachments(masterSecret, mediaConstraints, message.getAttachments());
    List<OpenNetworkServiceAttachment> attachmentStreams = getAttachmentsFor(masterSecret, scaledAttachments);

    List<OpenNetworkServiceAddress>    addresses;

    if (filterAddress != null) addresses = getPushAddresses(filterAddress);
    else                       addresses = getPushAddresses(recipients);

    if (message.isGroup()) {
      OutgoingGroupMediaMessage groupMessage     = (OutgoingGroupMediaMessage) message;
      GroupContext              groupContext     = groupMessage.getGroupContext();
      OpenNetworkServiceAttachment   avatar           = attachmentStreams.isEmpty() ? null : attachmentStreams.get(0);
      OpenNetworkServiceGroup.Type   type             = groupMessage.isGroupQuit() ? OpenNetworkServiceGroup.Type.QUIT : OpenNetworkServiceGroup.Type.UPDATE;
      OpenNetworkServiceGroup        group            = new OpenNetworkServiceGroup(type, GroupUtil.getDecodedId(groupId), groupContext.getName(), groupContext.getMembersList(), avatar);
      OpenNetworkServiceDataMessage  groupDataMessage = OpenNetworkServiceDataMessage.newBuilder()
                                                                           .withTimestamp(message.getSentTimeMillis())
                                                                           .asGroupMessage(group)
                                                                           .build();

      messageSender.sendMessage(addresses, groupDataMessage);
    } else {
      OpenNetworkServiceGroup       group        = new OpenNetworkServiceGroup(GroupUtil.getDecodedId(groupId));
      OpenNetworkServiceDataMessage groupMessage = OpenNetworkServiceDataMessage.newBuilder()
                                                                      .withTimestamp(message.getSentTimeMillis())
                                                                      .asGroupMessage(group)
                                                                      .withAttachments(attachmentStreams)
                                                                      .withBody(message.getBody())
                                                                      .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                      .asExpirationUpdate(message.isExpirationUpdate())
                                                                      .withProfileKey(profileKey.orNull())
                                                                      .build();

      messageSender.sendMessage(addresses, groupMessage);
    }
  }

  private List<OpenNetworkServiceAddress> getPushAddresses(Address address) {
    List<OpenNetworkServiceAddress> addresses = new LinkedList<>();
    addresses.add(getPushAddress(address));
    return addresses;
  }

  private List<OpenNetworkServiceAddress> getPushAddresses(List<Address> addresses) {
    return Stream.of(addresses).map(this::getPushAddress).toList();
  }

  private @NonNull List<Address> getGroupMessageRecipients(String groupId, long messageId) {
    List<GroupReceiptInfo> destinations = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageId);
    if (!destinations.isEmpty()) return Stream.of(destinations).map(GroupReceiptInfo::getAddress).toList();

    List<Recipient> members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(groupId, false);
    return Stream.of(members).map(Recipient::getAddress).toList();
  }
}
