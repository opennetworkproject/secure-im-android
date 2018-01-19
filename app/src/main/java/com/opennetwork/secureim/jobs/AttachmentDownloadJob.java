package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.attachments.AttachmentId;
import com.opennetwork.secureim.crypto.AsymmetricMasterSecret;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.MasterSecretUtil;
import com.opennetwork.secureim.crypto.MediaKey;
import com.opennetwork.secureim.database.AttachmentDatabase;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.events.PartProgressEvent;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.mms.MmsException;
import com.opennetwork.secureim.notifications.MessageNotifier;
import com.opennetwork.secureim.util.AttachmentUtil;
import com.opennetwork.secureim.util.Hex;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.InvalidMessageException;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageReceiver;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment.ProgressListener;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachmentPointer;
import com.opennetwork.imservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

public class AttachmentDownloadJob extends MasterSecretJob implements InjectableType {
  private static final long   serialVersionUID    = 2L;
  private static final int    MAX_ATTACHMENT_SIZE = 150 * 1024  * 1024;
  private static final String TAG                  = AttachmentDownloadJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageReceiver messageReceiver;

  private final long    messageId;
  private final long    partRowId;
  private final long    partUniqueId;
  private final boolean manual;

  public AttachmentDownloadJob(Context context, long messageId, AttachmentId attachmentId, boolean manual) {
    super(context, JobParameters.newBuilder()
                                .withGroupId(AttachmentDownloadJob.class.getCanonicalName())
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.messageId    = messageId;
    this.partRowId    = attachmentId.getRowId();
    this.partUniqueId = attachmentId.getUniqueId();
    this.manual       = manual;
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    final AttachmentDatabase database     = DatabaseFactory.getAttachmentDatabase(context);
    final AttachmentId       attachmentId = new AttachmentId(partRowId, partUniqueId);
    final Attachment         attachment   = database.getAttachment(masterSecret, attachmentId);

    if (attachment == null) {
      Log.w(TAG, "attachment no longer exists.");
      return;
    }

    if (!attachment.isInProgress()) {
      Log.w(TAG, "Attachment was already downloaded.");
      return;
    }

    if (!manual && !AttachmentUtil.isAutoDownloadPermitted(context, attachment)) {
      Log.w(TAG, "Attachment can't be auto downloaded...");
      return;
    }

    Log.w(TAG, "Downloading push part " + attachmentId);
    database.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_STARTED);

    retrieveAttachment(masterSecret, messageId, attachmentId, attachment);
    MessageNotifier.updateNotification(context, masterSecret);
  }

  @Override
  public void onCanceled() {
    final AttachmentId attachmentId = new AttachmentId(partRowId, partUniqueId);
    markFailed(messageId, attachmentId);
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return (exception instanceof PushNetworkException);
  }

  private void retrieveAttachment(MasterSecret masterSecret,
                                  long messageId,
                                  final AttachmentId attachmentId,
                                  final Attachment attachment)
      throws IOException
  {

    AttachmentDatabase database       = DatabaseFactory.getAttachmentDatabase(context);
    File               attachmentFile = null;

    try {
      attachmentFile = createTempFile();

      OpenNetworkServiceAttachmentPointer pointer = createAttachmentPointer(masterSecret, attachment);
      InputStream                    stream  = messageReceiver.retrieveAttachment(pointer, attachmentFile, MAX_ATTACHMENT_SIZE, new ProgressListener() {
        @Override
        public void onAttachmentProgress(long total, long progress) {
          EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress));
        }
      });

      database.insertAttachmentsForPlaceholder(masterSecret, messageId, attachmentId, stream);
    } catch (InvalidPartException | NonSuccessfulResponseCodeException | InvalidMessageException | MmsException e) {
      Log.w(TAG, e);
      markFailed(messageId, attachmentId);
    } finally {
      if (attachmentFile != null)
        attachmentFile.delete();
    }
  }

  @VisibleForTesting
  OpenNetworkServiceAttachmentPointer createAttachmentPointer(MasterSecret masterSecret, Attachment attachment)
      throws InvalidPartException
  {
    if (TextUtils.isEmpty(attachment.getLocation())) {
      throw new InvalidPartException("empty content id");
    }

    if (TextUtils.isEmpty(attachment.getKey())) {
      throw new InvalidPartException("empty encrypted key");
    }

    try {
      AsymmetricMasterSecret asymmetricMasterSecret = MasterSecretUtil.getAsymmetricMasterSecret(context, masterSecret);
      long                   id                     = Long.parseLong(attachment.getLocation());
      byte[]                 key                    = MediaKey.getDecrypted(masterSecret, asymmetricMasterSecret, attachment.getKey());
      String                 relay                  = null;

      if (TextUtils.isEmpty(attachment.getRelay())) {
        relay = attachment.getRelay();
      }

      if (attachment.getDigest() != null) {
        Log.w(TAG, "Downloading attachment with digest: " + Hex.toString(attachment.getDigest()));
      } else {
        Log.w(TAG, "Downloading attachment with no digest...");
      }

      return new OpenNetworkServiceAttachmentPointer(id, null, key, relay,
                                                Optional.of(Util.toIntExact(attachment.getSize())),
                                                Optional.absent(),
                                                Optional.fromNullable(attachment.getDigest()),
                                                Optional.fromNullable(attachment.getFileName()),
                                                attachment.isVoiceNote());
    } catch (InvalidMessageException | IOException | ArithmeticException e) {
      Log.w(TAG, e);
      throw new InvalidPartException(e);
    }
  }

  private File createTempFile() throws InvalidPartException {
    try {
      File file = File.createTempFile("push-attachment", "tmp", context.getCacheDir());
      file.deleteOnExit();

      return file;
    } catch (IOException e) {
      throw new InvalidPartException(e);
    }
  }

  private void markFailed(long messageId, AttachmentId attachmentId) {
    try {
      AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);
      database.setTransferProgressFailed(attachmentId, messageId);
    } catch (MmsException e) {
      Log.w(TAG, e);
    }
  }

  @VisibleForTesting static class InvalidPartException extends Exception {
    public InvalidPartException(String s) {super(s);}
    public InvalidPartException(Exception e) {super(e);}
  }

}
