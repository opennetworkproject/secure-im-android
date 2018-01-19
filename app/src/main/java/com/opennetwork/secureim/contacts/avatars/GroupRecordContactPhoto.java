package com.opennetwork.secureim.contacts.avatars;


import android.content.Context;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupDatabase;
import com.opennetwork.secureim.util.Conversions;
import com.opennetwork.libim.util.guava.Optional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class GroupRecordContactPhoto implements ContactPhoto {

  private final @NonNull Address address;
  private final          long avatarId;

  public GroupRecordContactPhoto(@NonNull Address address, long avatarId) {
    this.address  = address;
    this.avatarId = avatarId;
  }

  @Override
  public InputStream openInputStream(Context context) throws IOException {
    GroupDatabase                       groupDatabase = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupDatabase.GroupRecord> groupRecord   = groupDatabase.getGroup(address.toGroupString());

    if (groupRecord.isPresent() && groupRecord.get().getAvatar() != null) {
      return new ByteArrayInputStream(groupRecord.get().getAvatar());
    }

    throw new IOException("Couldn't load avatar for group: " + address.toGroupString());
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(address.serialize().getBytes());
    messageDigest.update(Conversions.longToByteArray(avatarId));
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof GroupRecordContactPhoto)) return false;

    GroupRecordContactPhoto that = (GroupRecordContactPhoto)other;
    return this.address.equals(that.address) && this.avatarId == that.avatarId;
  }

  @Override
  public int hashCode() {
    return this.address.hashCode() ^ (int) avatarId;
  }

}
