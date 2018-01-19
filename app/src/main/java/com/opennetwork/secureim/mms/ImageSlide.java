package com.opennetwork.secureim.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.R;
import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.util.MediaUtil;

public class ImageSlide extends Slide {

  private static final String TAG = ImageSlide.class.getSimpleName();

  public ImageSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  public ImageSlide(Context context, Uri uri, long size) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_JPEG, size, true, null, false));
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    return 0;
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @NonNull
  @Override
  public String getContentDescription() {
    return context.getString(R.string.Slide_image);
  }
}
