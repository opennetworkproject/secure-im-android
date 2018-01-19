package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.EncryptingSmsDatabase;
import com.opennetwork.secureim.database.NoSuchMessageException;
import com.opennetwork.secureim.database.model.SmsMessageRecord;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.notifications.MessageNotifier;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.service.ExpiringMessageManager;
import com.opennetwork.secureim.transport.InsecureFallbackApprovalException;
import com.opennetwork.secureim.transport.RetryLaterException;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceDataMessage;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;
import com.opennetwork.imservice.api.push.exceptions.UnregisteredUserException;

import java.io.IOException;

import javax.inject.Inject;

public class PushTextSendJob extends PushSendJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = PushTextSendJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  private final long messageId;

  public PushTextSendJob(Context context, long messageId, Address destination) {
    super(context, constructParameters(context, destination));
    this.messageId = messageId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onPushSend(MasterSecret masterSecret) throws NoSuchMessageException, RetryLaterException {
    ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();
    EncryptingSmsDatabase  database          = DatabaseFactory.getEncryptingSmsDatabase(context);
    SmsMessageRecord       record            = database.getMessage(masterSecret, messageId);

    try {
      Log.w(TAG, "Sending message: " + messageId);

      deliver(record);
      database.markAsSent(messageId, true);

      if (record.getExpiresIn() > 0) {
        database.markExpireStarted(messageId);
        expirationManager.scheduleDeletion(record.getId(), record.isMms(), record.getExpiresIn());
      }

    } catch (InsecureFallbackApprovalException e) {
      Log.w(TAG, e);
      database.markAsPendingInsecureSmsFallback(record.getId());
      MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
      ApplicationContext.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, false));
    } catch (UntrustedIdentityException e) {
      Log.w(TAG, e);
      database.addMismatchedIdentity(record.getId(), Address.fromSerialized(e.getE164Number()), e.getIdentityKey());
      database.markAsSentFailed(record.getId());
      database.markAsPush(record.getId());
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof RetryLaterException) return true;

    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);

    long      threadId  = DatabaseFactory.getSmsDatabase(context).getThreadIdForMessage(messageId);
    Recipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

    if (threadId != -1 && recipient != null) {
      MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
    }
  }

  private void deliver(SmsMessageRecord message)
      throws UntrustedIdentityException, InsecureFallbackApprovalException, RetryLaterException
  {
    try {
      OpenNetworkServiceAddress       address           = getPushAddress(message.getIndividualRecipient().getAddress());
      Optional<byte[]>           profileKey        = getProfileKey(message.getIndividualRecipient());
      OpenNetworkServiceDataMessage   textSecureMessage = OpenNetworkServiceDataMessage.newBuilder()
                                                                             .withTimestamp(message.getDateSent())
                                                                             .withBody(message.getBody().getBody())
                                                                             .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                             .withProfileKey(profileKey.orNull())
                                                                             .asEndSessionMessage(message.isEndSession())
                                                                             .build();


      messageSender.sendMessage(address, textSecureMessage);
    } catch (UnregisteredUserException e) {
      Log.w(TAG, e);
      throw new InsecureFallbackApprovalException(e);
    } catch (IOException e) {
      Log.w(TAG, e);
      throw new RetryLaterException(e);
    }
  }
}
