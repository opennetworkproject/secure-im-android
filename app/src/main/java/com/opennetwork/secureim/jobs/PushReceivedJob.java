package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.MessagingDatabase.SyncMessageId;
import com.opennetwork.secureim.database.RecipientDatabase;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.service.KeyCachingService;
import com.opennetwork.jobqueue.JobManager;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceEnvelope;

public abstract class PushReceivedJob extends ContextJob {

  private static final String TAG = PushReceivedJob.class.getSimpleName();

  protected PushReceivedJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  public void handle(OpenNetworkServiceEnvelope envelope) {
    Address   source    = Address.fromExternal(context, envelope.getSource());
    Recipient recipient = Recipient.from(context, source, false);

    if (!isActiveNumber(recipient)) {
      DatabaseFactory.getRecipientDatabase(context).setRegistered(recipient, RecipientDatabase.RegisteredState.REGISTERED);
      ApplicationContext.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, KeyCachingService.getMasterSecret(context), recipient, false));
    }

    if (envelope.isReceipt()) {
      handleReceipt(envelope);
    } else if (envelope.isPreKeyOpenNetworkMessage() || envelope.isOpenNetworkMessage()) {
      handleMessage(envelope, source);
    } else {
      Log.w(TAG, "Received envelope of unknown type: " + envelope.getType());
    }
  }

  private void handleMessage(OpenNetworkServiceEnvelope envelope, Address source) {
    Recipient  recipients = Recipient.from(context, source, false);
    JobManager jobManager = ApplicationContext.getInstance(context).getJobManager();

    if (!recipients.isBlocked()) {
      long messageId = DatabaseFactory.getPushDatabase(context).insert(envelope);
      jobManager.add(new PushDecryptJob(context, messageId));
    } else {
      Log.w(TAG, "*** Received blocked push message, ignoring...");
    }
  }

  private void handleReceipt(OpenNetworkServiceEnvelope envelope) {
    Log.w(TAG, String.format("Received receipt: (XXXXX, %d)", envelope.getTimestamp()));
    DatabaseFactory.getMmsSmsDatabase(context).incrementDeliveryReceiptCount(new SyncMessageId(Address.fromExternal(context, envelope.getSource()),
                                                                                               envelope.getTimestamp()), System.currentTimeMillis());
  }

  private boolean isActiveNumber(@NonNull Recipient recipient) {
    return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
  }


}
