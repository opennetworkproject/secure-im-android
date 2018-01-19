package com.opennetwork.secureim.jobs;

import android.content.Context;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.RecipientDatabase;
import com.opennetwork.secureim.database.RecipientDatabase.BlockedReader;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.multidevice.BlockedListMessage;
import com.opennetwork.imservice.api.messages.multidevice.OpenNetworkServiceSyncMessage;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class MultiDeviceBlockedUpdateJob extends MasterSecretJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = MultiDeviceBlockedUpdateJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  public MultiDeviceBlockedUpdateJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withRequirement(new MasterSecretRequirement(context))
                                .withGroupId(MultiDeviceBlockedUpdateJob.class.getSimpleName())
                                .withPersistence()
                                .create());
  }

  @Override
  public void onRun(MasterSecret masterSecret)
      throws IOException, UntrustedIdentityException
  {
    RecipientDatabase database = DatabaseFactory.getRecipientDatabase(context);
    BlockedReader     reader   = database.readerForBlocked(database.getBlocked());
    List<String>      blocked  = new LinkedList<>();

    Recipient recipient;

    while ((recipient = reader.getNext()) != null) {
      if (!recipient.isGroupRecipient()) {
        blocked.add(recipient.getAddress().serialize());
      }
    }

    messageSender.sendMessage(OpenNetworkServiceSyncMessage.forBlocked(new BlockedListMessage(blocked)));
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {

  }
}
