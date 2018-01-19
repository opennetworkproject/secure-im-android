package com.opennetwork.secureim.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.R;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.util.MediaUtil;
import com.opennetwork.secureim.util.ResUtil;

public class VideoSlide extends Slide {

  public VideoSlide(Context context, Uri uri, long dataSize) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.VIDEO_UNSPECIFIED, dataSize, MediaUtil.hasVideoThumbnail(uri), null, false));
  }

  public VideoSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }

  @Override
  public boolean hasPlaceholder() {
    return true;
  }

  @Override
  public boolean hasPlayOverlay() {
    return true;
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_video);
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @Override
  public boolean hasVideo() {
    return true;
  }

  @NonNull @Override
  public String getContentDescription() {
    return context.getString(R.string.Slide_video);
  }
}
