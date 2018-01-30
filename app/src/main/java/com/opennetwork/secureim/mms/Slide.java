package com.opennetwork.secureim.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.attachments.UriAttachment;
import com.opennetwork.secureim.database.AttachmentDatabase;
import com.opennetwork.secureim.util.MediaUtil;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.libim.util.guava.Optional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public abstract class Slide {

  protected final Attachment attachment;
  protected final Context    context;

  public Slide(@NonNull Context context, @NonNull Attachment attachment) {
    this.context    = context;
    this.attachment = attachment;

  }

  public String getContentType() {
    return attachment.getContentType();
  }

  @Nullable
  public Uri getUri() {
    return attachment.getDataUri();
  }

  @Nullable
  public Uri getThumbnailUri() {
    return attachment.getThumbnailUri();
  }

  @NonNull
  public Optional<String> getBody() {
    return Optional.absent();
  }

  @NonNull
  public Optional<String> getFileName() {
    return Optional.fromNullable(attachment.getFileName());
  }

  @Nullable
  public String getFastPreflightId() {
    return attachment.getFastPreflightId();
  }

  public long getFileSize() {
    return attachment.getSize();
  }

  public boolean hasImage() {
    return false;
  }

  public boolean hasVideo() {
    return false;
  }

  public boolean hasAudio() {
    return false;
  }

  public boolean hasDocument() {
    return false;
  }

  public boolean hasLocation() {
    return false;
  }

  public @NonNull String getContentDescription() { return ""; }

  public Attachment asAttachment() {
    return attachment;
  }

  public boolean isInProgress() {
    return attachment.isInProgress();
  }

  public boolean isPendingDownload() {
    return getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_FAILED ||
           getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_PENDING;
  }

  public long getTransferState() {
    return attachment.getTransferState();
  }

  public @DrawableRes int getPlaceholderRes(Theme theme) {
    throw new AssertionError("getPlaceholderRes() called for non-drawable slide");
  }

  public boolean hasPlaceholder() {
    return false;
  }

  public boolean hasPlayOverlay() {
    return false;
  }

  protected static Attachment constructAttachmentFromUri(@NonNull  Context context,
                                                         @NonNull  Uri     uri,
                                                         @NonNull  String  defaultMime,
                                                                   long     size,
                                                                   boolean  hasThumbnail,
                                                         @Nullable String   fileName,
                                                                   boolean  voiceNote)
  {
    try {
      Optional<String> resolvedType    = Optional.fromNullable(MediaUtil.getMimeType(context, uri));
      String           fastPreflightId = String.valueOf(SecureRandom.getInstance("SHA1PRNG").nextLong());
      return new UriAttachment(uri, hasThumbnail ? uri : null, resolvedType.or(defaultMime), AttachmentDatabase.TRANSFER_PROGRESS_STARTED, size, fileName, fastPreflightId, voiceNote);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)             return false;
    if (!(other instanceof Slide)) return false;

    Slide that = (Slide)other;

    return Util.equals(this.getContentType(), that.getContentType()) &&
           this.hasAudio() == that.hasAudio()                        &&
           this.hasImage() == that.hasImage()                        &&
           this.hasVideo() == that.hasVideo()                        &&
           this.getTransferState() == that.getTransferState()        &&
           Util.equals(this.getUri(), that.getUri())                 &&
           Util.equals(this.getThumbnailUri(), that.getThumbnailUri());
  }

  @Override
  public int hashCode() {
    return Util.hashCode(getContentType(), hasAudio(), hasImage(),
                         hasVideo(), getUri(), getThumbnailUri(), getTransferState());
  }
}
