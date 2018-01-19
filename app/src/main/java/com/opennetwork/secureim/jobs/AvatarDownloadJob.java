package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupDatabase;
import com.opennetwork.secureim.database.GroupDatabase.GroupRecord;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.mms.AttachmentStreamUriLoader.AttachmentModel;
import com.opennetwork.secureim.util.BitmapDecodingException;
import com.opennetwork.secureim.util.BitmapUtil;
import com.opennetwork.secureim.util.GroupUtil;
import com.opennetwork.secureim.util.Hex;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.InvalidMessageException;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageReceiver;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachmentPointer;
import com.opennetwork.imservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

public class AvatarDownloadJob extends MasterSecretJob implements InjectableType {

  private static final int MAX_AVATAR_SIZE = 20 * 1024 * 1024;
  private static final long serialVersionUID = 1L;

  private static final String TAG = AvatarDownloadJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageReceiver receiver;

  private final byte[] groupId;

  public AvatarDownloadJob(Context context, @NonNull byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.groupId = groupId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    String                encodeId   = GroupUtil.getEncodedId(groupId, false);
    GroupDatabase         database   = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupRecord> record     = database.getGroup(encodeId);
    File                  attachment = null;

    try {
      if (record.isPresent()) {
        long             avatarId    = record.get().getAvatarId();
        String           contentType = record.get().getAvatarContentType();
        byte[]           key         = record.get().getAvatarKey();
        String           relay       = record.get().getRelay();
        Optional<byte[]> digest      = Optional.fromNullable(record.get().getAvatarDigest());
        Optional<String> fileName    = Optional.absent();

        if (avatarId == -1 || key == null) {
          return;
        }

        if (digest.isPresent()) {
          Log.w(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
        }

        attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
        attachment.deleteOnExit();

        OpenNetworkServiceAttachmentPointer pointer     = new OpenNetworkServiceAttachmentPointer(avatarId, contentType, key, relay, Optional.of(0), Optional.absent(), digest, fileName, false);
        InputStream                    inputStream = receiver.retrieveAttachment(pointer, attachment, MAX_AVATAR_SIZE);
        Bitmap                         avatar      = BitmapUtil.createScaledBitmap(context, new AttachmentModel(attachment, key, 0, digest), 500, 500);

        database.updateAvatar(encodeId, avatar);
        inputStream.close();
      }
    } catch (BitmapDecodingException | NonSuccessfulResponseCodeException | InvalidMessageException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onCanceled() {}

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

}
