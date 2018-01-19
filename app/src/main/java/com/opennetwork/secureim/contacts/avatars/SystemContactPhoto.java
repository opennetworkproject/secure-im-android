package com.opennetwork.secureim.contacts.avatars;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.util.Conversions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;

public class SystemContactPhoto implements ContactPhoto {

  private final @NonNull Address address;
  private final @NonNull Uri     contactPhotoUri;
  private final          long    lastModifiedTime;

  public SystemContactPhoto(@NonNull Address address, @NonNull Uri contactPhotoUri, long lastModifiedTime) {
    this.address          = address;
    this.contactPhotoUri  = contactPhotoUri;
    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  public InputStream openInputStream(Context context) throws FileNotFoundException {
    return context.getContentResolver().openInputStream(contactPhotoUri);

  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(address.serialize().getBytes());
    messageDigest.update(contactPhotoUri.toString().getBytes());
    messageDigest.update(Conversions.longToByteArray(lastModifiedTime));
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof SystemContactPhoto)) return false;

    SystemContactPhoto that = (SystemContactPhoto)other;

    return this.address.equals(that.address) && this.contactPhotoUri.equals(that.contactPhotoUri) && this.lastModifiedTime == that.lastModifiedTime;
  }

  @Override
  public int hashCode() {
    return address.hashCode() ^ contactPhotoUri.hashCode() ^ (int)lastModifiedTime;
  }

}
