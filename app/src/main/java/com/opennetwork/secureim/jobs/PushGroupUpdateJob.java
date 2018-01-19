package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupDatabase;
import com.opennetwork.secureim.database.GroupDatabase.GroupRecord;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.util.GroupUtil;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachmentStream;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceDataMessage;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup.Type;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class PushGroupUpdateJob extends ContextJob implements InjectableType {

  private static final String TAG = PushGroupUpdateJob.class.getSimpleName();

  private static final long serialVersionUID = 0L;

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final String source;
  private final byte[] groupId;


  public PushGroupUpdateJob(Context context, String source, byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new NetworkRequirement(context))
                                .withRetryCount(50)
                                .create());

    this.source  = source;
    this.groupId = groupId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    GroupDatabase           groupDatabase = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupRecord>   record        = groupDatabase.getGroup(GroupUtil.getEncodedId(groupId, false));
    OpenNetworkServiceAttachment avatar        = null;

    if (record == null) {
      Log.w(TAG, "No information for group record info request: " + new String(groupId));
      return;
    }

    if (record.get().getAvatar() != null) {
      avatar = OpenNetworkServiceAttachmentStream.newStreamBuilder()
                                            .withContentType("image/jpeg")
                                            .withStream(new ByteArrayInputStream(record.get().getAvatar()))
                                            .withLength(record.get().getAvatar().length)
                                            .build();
    }

    List<String> members = new LinkedList<>();

    for (Address member : record.get().getMembers()) {
      members.add(member.serialize());
    }

    OpenNetworkServiceGroup groupContext = OpenNetworkServiceGroup.newBuilder(Type.UPDATE)
                                                        .withAvatar(avatar)
                                                        .withId(groupId)
                                                        .withMembers(members)
                                                        .withName(record.get().getTitle())
                                                        .build();

    OpenNetworkServiceDataMessage message = OpenNetworkServiceDataMessage.newBuilder()
                                                               .asGroupMessage(groupContext)
                                                               .withTimestamp(System.currentTimeMillis())
                                                               .build();

    messageSender.sendMessage(new OpenNetworkServiceAddress(source), message);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    Log.w(TAG, e);
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {

  }
}
