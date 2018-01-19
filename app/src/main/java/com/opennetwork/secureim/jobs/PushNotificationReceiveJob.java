package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageReceiver;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceEnvelope;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class PushNotificationReceiveJob extends PushReceivedJob implements InjectableType {

  private static final String TAG = PushNotificationReceiveJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageReceiver receiver;

  public PushNotificationReceiveJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withGroupId("__notification_received")
                                .withWakeLock(true, 30, TimeUnit.SECONDS).create());
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    receiver.retrieveMessages(new OpenNetworkServiceMessageReceiver.MessageReceivedCallback() {
      @Override
      public void onMessage(OpenNetworkServiceEnvelope envelope) {
        handle(envelope);
      }
    });
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    Log.w(TAG, e);
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "***** Failed to download pending message!");
//    MessageNotifier.notifyMessagesPending(getContext());
  }
}
