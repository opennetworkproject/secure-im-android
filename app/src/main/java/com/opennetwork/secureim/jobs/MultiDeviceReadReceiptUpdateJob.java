package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.multidevice.ConfigurationMessage;
import com.opennetwork.imservice.api.messages.multidevice.OpenNetworkServiceSyncMessage;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

public class MultiDeviceReadReceiptUpdateJob extends ContextJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = MultiDeviceReadReceiptUpdateJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final boolean enabled;

  public MultiDeviceReadReceiptUpdateJob(Context context, boolean enabled) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withGroupId("__MULTI_DEVICE_READ_RECEIPT_UPDATE_JOB__")
                                .withRequirement(new NetworkRequirement(context))
                                .create());

    this.enabled = enabled;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    messageSender.sendMessage(OpenNetworkServiceSyncMessage.forConfiguration(new ConfigurationMessage(Optional.of(enabled))));
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "**** Failed to synchronize read receipts state!");
  }
}
