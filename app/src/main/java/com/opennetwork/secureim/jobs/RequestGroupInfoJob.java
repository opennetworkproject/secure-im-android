package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceDataMessage;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup.Type;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

public class RequestGroupInfoJob extends ContextJob implements InjectableType {

  private static final String TAG = RequestGroupInfoJob.class.getSimpleName();

  private static final long serialVersionUID = 0L;

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final String source;
  private final byte[] groupId;

  public RequestGroupInfoJob(@NonNull Context context, @NonNull String source, @NonNull byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .withRetryCount(50)
                                .create());

    this.source  = source;
    this.groupId = groupId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    OpenNetworkServiceGroup       group   = OpenNetworkServiceGroup.newBuilder(Type.REQUEST_INFO)
                                                         .withId(groupId)
                                                         .build();

    OpenNetworkServiceDataMessage message = OpenNetworkServiceDataMessage.newBuilder()
                                                               .asGroupMessage(group)
                                                               .withTimestamp(System.currentTimeMillis())
                                                               .build();

    messageSender.sendMessage(new OpenNetworkServiceAddress(source), message);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {

  }
}
