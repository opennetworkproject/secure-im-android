package com.opennetwork.secureim.database.loaders;


import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.AbstractCursorLoader;

public class ThreadMediaLoader extends AbstractCursorLoader {

  private final Address      address;
  private final MasterSecret masterSecret;
  private final boolean      gallery;

  public ThreadMediaLoader(@NonNull Context context, @NonNull MasterSecret masterSecret, @NonNull Address address, boolean gallery) {
    super(context);
    this.masterSecret = masterSecret;
    this.address      = address;
    this.gallery      = gallery;
  }

  @Override
  public Cursor getCursor() {
    long threadId = DatabaseFactory.getThreadDatabase(getContext()).getThreadIdFor(Recipient.from(getContext(), address, true));

    if (gallery) return DatabaseFactory.getMediaDatabase(getContext()).getGalleryMediaForThread(threadId);
    else         return DatabaseFactory.getMediaDatabase(getContext()).getDocumentMediaForThread(threadId);
  }

  public Address getAddress() {
    return address;
  }

  public MasterSecret getMasterSecret() {
    return masterSecret;
  }
}
