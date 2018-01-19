package com.opennetwork.secureim.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.MmsSmsDatabase;
import com.opennetwork.secureim.util.AbstractCursorLoader;

public class MessageDetailsLoader extends AbstractCursorLoader {
  private final String type;
  private final long   messageId;

  public MessageDetailsLoader(Context context, String type, long messageId) {
    super(context);
    this.type      = type;
    this.messageId = messageId;
  }

  @Override
  public Cursor getCursor() {
    switch (type) {
      case MmsSmsDatabase.SMS_TRANSPORT:
        return DatabaseFactory.getEncryptingSmsDatabase(context).getMessage(messageId);
      case MmsSmsDatabase.MMS_TRANSPORT:
        return DatabaseFactory.getMmsDatabase(context).getMessage(messageId);
      default:
        throw new AssertionError("no valid message type specified");
    }
  }
}
