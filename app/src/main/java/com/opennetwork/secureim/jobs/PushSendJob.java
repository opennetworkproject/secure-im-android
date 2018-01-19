package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.TextSecureExpiredException;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.ProfileKeyUtil;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.events.PartProgressEvent;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.mms.PartAuthority;
import com.opennetwork.secureim.notifications.MessageNotifier;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.TextSecurePreferences;

import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment.ProgressListener;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public abstract class PushSendJob extends SendJob {

  private static final String TAG = PushSendJob.class.getSimpleName();

  protected PushSendJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  protected static JobParameters constructParameters(Context context, Address destination) {
    JobParameters.Builder builder = JobParameters.newBuilder();
    builder.withPersistence();
    builder.withGroupId(destination.serialize());
    builder.withRequirement(new MasterSecretRequirement(context));
    builder.withRequirement(new NetworkRequirement(context));
    builder.withRetryCount(5);

    return builder.create();
  }

  @Override
  protected final void onSend(MasterSecret masterSecret) throws Exception {
    if (TextSecurePreferences.getSignedPreKeyFailureCount(context) > 5) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new RotateSignedPreKeyJob(context));

      throw new TextSecureExpiredException("Too many signed prekey rotation failures");
    }

    onPushSend(masterSecret);
  }

  protected Optional<byte[]> getProfileKey(@NonNull Recipient recipient) {
    if (!recipient.resolve().isSystemContact() && !recipient.resolve().isProfileSharing()) {
      return Optional.absent();
    }

    return Optional.of(ProfileKeyUtil.getProfileKey(context));
  }

  protected OpenNetworkServiceAddress getPushAddress(Address address) {
//    String relay = TextSecureDirectory.getInstance(context).getRelay(address.toPhoneString());
    String relay = null;
    return new OpenNetworkServiceAddress(address.toPhoneString(), Optional.fromNullable(relay));
  }

  protected List<OpenNetworkServiceAttachment> getAttachmentsFor(MasterSecret masterSecret, List<Attachment> parts) {
    List<OpenNetworkServiceAttachment> attachments = new LinkedList<>();

    for (final Attachment attachment : parts) {
      try {
        if (attachment.getDataUri() == null || attachment.getSize() == 0) throw new IOException("Assertion failed, outgoing attachment has no data!");
        InputStream is = PartAuthority.getAttachmentStream(context, masterSecret, attachment.getDataUri());
        attachments.add(OpenNetworkServiceAttachment.newStreamBuilder()
                                               .withStream(is)
                                               .withContentType(attachment.getContentType())
                                               .withLength(attachment.getSize())
                                               .withFileName(attachment.getFileName())
                                               .withVoiceNote(attachment.isVoiceNote())
                                               .withListener(new ProgressListener() {
                                                 @Override
                                                 public void onAttachmentProgress(long total, long progress) {
                                                   EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress));
                                                 }
                                               })
                                               .build());
      } catch (IOException ioe) {
        Log.w(TAG, "Couldn't open attachment", ioe);
      }
    }

    return attachments;
  }

  protected void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
    long      threadId  = DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId);
    Recipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

    if (threadId != -1 && recipient != null) {
      MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
    }
  }

  protected abstract void onPushSend(MasterSecret masterSecret) throws Exception;
}
