package com.opennetwork.secureim.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.R;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.attachments.UriAttachment;
import com.opennetwork.secureim.database.AttachmentDatabase;
import com.opennetwork.secureim.util.MediaUtil;
import com.opennetwork.secureim.util.ResUtil;


public class AudioSlide extends Slide {

  public AudioSlide(Context context, Uri uri, long dataSize, boolean voiceNote) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.AUDIO_UNSPECIFIED, dataSize, false, null, voiceNote));
  }

  public AudioSlide(Context context, Uri uri, long dataSize, String contentType, boolean voiceNote) {
    super(context,  new UriAttachment(uri, null, contentType, AttachmentDatabase.TRANSFER_PROGRESS_STARTED, dataSize, null, null, voiceNote));
  }

  public AudioSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }

  @Override
  @Nullable
  public Uri getThumbnailUri() {
    return null;
  }

  @Override
  public boolean hasPlaceholder() {
    return true;
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @Override
  public boolean hasAudio() {
    return true;
  }

  @NonNull
  @Override
  public String getContentDescription() {
    return context.getString(R.string.Slide_audio);
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_audio);
  }
}
