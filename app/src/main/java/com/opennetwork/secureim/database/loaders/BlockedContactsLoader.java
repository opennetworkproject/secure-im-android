package com.opennetwork.secureim.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext())
                          .getBlocked();
  }

}
