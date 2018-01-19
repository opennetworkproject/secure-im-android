package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceReceiptMessage;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class SendReadReceiptJob extends ContextJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = SendReadReceiptJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final String     address;
  private final List<Long> messageIds;
  private final long       timestamp;

  public SendReadReceiptJob(Context context, Address address, List<Long> messageIds) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.address    = address.serialize();
    this.messageIds = messageIds;
    this.timestamp  = System.currentTimeMillis();
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    if (!TextSecurePreferences.isReadReceiptsEnabled(context)) return;

    OpenNetworkServiceAddress        remoteAddress  = new OpenNetworkServiceAddress(address);
    OpenNetworkServiceReceiptMessage receiptMessage = new OpenNetworkServiceReceiptMessage(OpenNetworkServiceReceiptMessage.Type.READ, messageIds, timestamp);

    messageSender.sendReceipt(remoteAddress, receiptMessage);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    if (e instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "Failed to send read receipts to: " + address);
  }
}
