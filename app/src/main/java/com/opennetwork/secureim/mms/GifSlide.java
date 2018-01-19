package com.opennetwork.secureim.mms;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.util.MediaUtil;

public class GifSlide extends ImageSlide {

  public GifSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }

  public GifSlide(Context context, Uri uri, long size) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_GIF, size, true, null, false));
  }

  @Override
  @Nullable
  public Uri getThumbnailUri() {
    return getUri();
  }
}
