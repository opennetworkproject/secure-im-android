package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.RetrieveConf;

import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.attachments.UriAttachment;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.MasterSecretUnion;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.AttachmentDatabase;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.MessagingDatabase.InsertResult;
import com.opennetwork.secureim.database.MmsDatabase;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.mms.ApnUnavailableException;
import com.opennetwork.secureim.mms.CompatMmsConnection;
import com.opennetwork.secureim.mms.IncomingMediaMessage;
import com.opennetwork.secureim.mms.MmsException;
import com.opennetwork.secureim.mms.MmsRadioException;
import com.opennetwork.secureim.mms.PartParser;
import com.opennetwork.secureim.notifications.MessageNotifier;
import com.opennetwork.secureim.providers.SingleUseBlobProvider;
import com.opennetwork.secureim.service.KeyCachingService;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.DuplicateMessageException;
import com.opennetwork.libim.InvalidMessageException;
import com.opennetwork.libim.LegacyMessageException;
import com.opennetwork.libim.NoSessionException;
import com.opennetwork.libim.util.guava.Optional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MmsDownloadJob extends MasterSecretJob {

  private static final String TAG = MmsDownloadJob.class.getSimpleName();

  private final long    messageId;
  private final long    threadId;
  private final boolean automatic;

  public MmsDownloadJob(Context context, long messageId, long threadId, boolean automatic) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withGroupId("mms-operation")
                                .withWakeLock(true, 30, TimeUnit.SECONDS)
                                .create());

    this.messageId = messageId;
    this.threadId  = threadId;
    this.automatic = automatic;
  }

  @Override
  public void onAdded() {
    if (automatic && KeyCachingService.getMasterSecret(context) == null) {
      DatabaseFactory.getMmsDatabase(context).markIncomingNotificationReceived(threadId);
      MessageNotifier.updateNotification(context, null);
    }
  }

  @Override
  public void onRun(MasterSecret masterSecret) {
    MmsDatabase                               database     = DatabaseFactory.getMmsDatabase(context);
    Optional<MmsDatabase.MmsNotificationInfo> notification = database.getNotification(messageId);

    if (!notification.isPresent()) {
      Log.w(TAG, "No notification for ID: " + messageId);
      return;
    }

    try {
      if (notification.get().getContentLocation() == null) {
        throw new MmsException("Notification content location was null.");
      }

      if (!TextSecurePreferences.isPushRegistered(context)) {
        throw new MmsException("Not registered");
      }

      database.markDownloadState(messageId, MmsDatabase.Status.DOWNLOAD_CONNECTING);

      String contentLocation = notification.get().getContentLocation();
      byte[] transactionId   = new byte[0];

      try {
        if (notification.get().getTransactionId() != null) {
          transactionId = notification.get().getTransactionId().getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } else {
          Log.w(TAG, "No transaction ID!");
        }
      } catch (UnsupportedEncodingException e) {
        Log.w(TAG, e);
      }

      Log.w(TAG, "Downloading mms at " + Uri.parse(contentLocation).getHost() + ", subscription ID: " + notification.get().getSubscriptionId());

      RetrieveConf retrieveConf = new CompatMmsConnection(context).retrieve(contentLocation, transactionId, notification.get().getSubscriptionId());

      if (retrieveConf == null) {
        throw new MmsException("RetrieveConf was null");
      }

      storeRetrievedMms(masterSecret, contentLocation, messageId, threadId, retrieveConf, notification.get().getSubscriptionId(), notification.get().getFrom());
    } catch (ApnUnavailableException e) {
      Log.w(TAG, e);
      handleDownloadError(masterSecret, messageId, threadId, MmsDatabase.Status.DOWNLOAD_APN_UNAVAILABLE,
                          automatic);
    } catch (MmsException e) {
      Log.w(TAG, e);
      handleDownloadError(masterSecret, messageId, threadId,
                          MmsDatabase.Status.DOWNLOAD_HARD_FAILURE,
                          automatic);
    } catch (MmsRadioException | IOException e) {
      Log.w(TAG, e);
      handleDownloadError(masterSecret, messageId, threadId,
                          MmsDatabase.Status.DOWNLOAD_SOFT_FAILURE,
                          automatic);
    } catch (DuplicateMessageException e) {
      Log.w(TAG, e);
      database.markAsDecryptDuplicate(messageId, threadId);
    } catch (LegacyMessageException e) {
      Log.w(TAG, e);
      database.markAsLegacyVersion(messageId, threadId);
    } catch (NoSessionException e) {
      Log.w(TAG, e);
      database.markAsNoSession(messageId, threadId);
    } catch (InvalidMessageException e) {
      Log.w(TAG, e);
      database.markAsDecryptFailed(messageId, threadId);
    }
  }

  @Override
  public void onCanceled() {
    MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
    database.markDownloadState(messageId, MmsDatabase.Status.DOWNLOAD_SOFT_FAILURE);

    if (automatic) {
      database.markIncomingNotificationReceived(threadId);
      MessageNotifier.updateNotification(context, null, threadId);
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return false;
  }

  private void storeRetrievedMms(MasterSecret masterSecret, String contentLocation,
                                 long messageId, long threadId, RetrieveConf retrieved,
                                 int subscriptionId, @Nullable Address notificationFrom)
      throws MmsException, NoSessionException, DuplicateMessageException, InvalidMessageException,
             LegacyMessageException
  {
    MmsDatabase           database    = DatabaseFactory.getMmsDatabase(context);
    SingleUseBlobProvider provider    = SingleUseBlobProvider.getInstance();
    Optional<Address>     group       = Optional.absent();
    Set<Address>          members     = new HashSet<>();
    String                body        = null;
    List<Attachment>      attachments = new LinkedList<>();

    Address               from;

    if (retrieved.getFrom() != null) {
      from = Address.fromExternal(context, Util.toIsoString(retrieved.getFrom().getTextString()));
    } else if (notificationFrom != null) {
      from = notificationFrom;
    } else {
      from = Address.UNKNOWN;
    }

    if (retrieved.getTo() != null) {
      for (EncodedStringValue toValue : retrieved.getTo()) {
        members.add(Address.fromExternal(context, Util.toIsoString(toValue.getTextString())));
      }
    }

    if (retrieved.getCc() != null) {
      for (EncodedStringValue ccValue : retrieved.getCc()) {
        members.add(Address.fromExternal(context, Util.toIsoString(ccValue.getTextString())));
      }
    }

    members.add(from);
    members.add(Address.fromExternal(context, TextSecurePreferences.getLocalNumber(context)));

    if (retrieved.getBody() != null) {
      body = PartParser.getMessageText(retrieved.getBody());
      PduBody media = PartParser.getSupportedMediaParts(retrieved.getBody());

      for (int i=0;i<media.getPartsNum();i++) {
        PduPart part = media.getPart(i);

        if (part.getData() != null) {
          Uri    uri  = provider.createUri(part.getData());
          String name = null;

          if (part.getName() != null) name = Util.toIsoString(part.getName());

          attachments.add(new UriAttachment(uri, Util.toIsoString(part.getContentType()),
                                            AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                                            part.getData().length, name, false));
        }
      }
    }

    if (members.size() > 2) {
      group = Optional.of(Address.fromSerialized(DatabaseFactory.getGroupDatabase(context).getOrCreateGroupForMembers(new LinkedList<>(members), true)));
    }

    IncomingMediaMessage   message      = new IncomingMediaMessage(from, group, body, retrieved.getDate() * 1000L, attachments, subscriptionId, 0, false);
    Optional<InsertResult> insertResult = database.insertMessageInbox(new MasterSecretUnion(masterSecret),
                                                                      message, contentLocation, threadId);

    if (insertResult.isPresent()) {
      database.delete(messageId);
      MessageNotifier.updateNotification(context, masterSecret, insertResult.get().getThreadId());
    }
  }

  private void handleDownloadError(MasterSecret masterSecret, long messageId, long threadId,
                                   int downloadStatus, boolean automatic)
  {
    MmsDatabase db = DatabaseFactory.getMmsDatabase(context);

    db.markDownloadState(messageId, downloadStatus);

    if (automatic) {
      db.markIncomingNotificationReceived(threadId);
      MessageNotifier.updateNotification(context, masterSecret, threadId);
    }
  }
}