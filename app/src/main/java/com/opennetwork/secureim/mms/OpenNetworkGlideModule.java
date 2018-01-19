package com.opennetwork.secureim.mms;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import com.opennetwork.secureim.contacts.avatars.ContactPhoto;
import com.opennetwork.secureim.giph.model.GiphyPaddedUrl;
import com.opennetwork.secureim.glide.ContactPhotoLoader;
import com.opennetwork.secureim.glide.GiphyPaddedUrlLoader;
import com.opennetwork.secureim.glide.OkHttpUrlLoader;
import com.opennetwork.secureim.mms.AttachmentStreamUriLoader.AttachmentModel;
import com.opennetwork.secureim.mms.DecryptableStreamUriLoader.DecryptableUri;

import java.io.InputStream;

@GlideModule
public class OpenNetworkGlideModule extends AppGlideModule {

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setLogLevel(Log.ERROR);
//    builder.setDiskCache(new NoopDiskCacheFactory());
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(ContactPhoto.class, InputStream.class, new ContactPhotoLoader.Factory(context));
    registry.append(DecryptableUri.class, InputStream.class, new DecryptableStreamUriLoader.Factory(context));
    registry.append(AttachmentModel.class, InputStream.class, new AttachmentStreamUriLoader.Factory());
    registry.append(GiphyPaddedUrl.class, InputStream.class, new GiphyPaddedUrlLoader.Factory());
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }

  public static class NoopDiskCacheFactory implements DiskCache.Factory {
    @Override
    public DiskCache build() {
      return new DiskCacheAdapter();
    }
  }
}
