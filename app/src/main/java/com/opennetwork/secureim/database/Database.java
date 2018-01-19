package com.opennetwork.secureim.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.Set;

public abstract class Database {

  protected static final String ID_WHERE              = "_id = ?";
  private   static final String CONVERSATION_URI      = "content://textsecure/thread/";
  private   static final String CONVERSATION_LIST_URI = "content://textsecure/conversation-list";

  protected       SQLiteOpenHelper databaseHelper;
  protected final Context context;

  public Database(Context context, SQLiteOpenHelper databaseHelper) {
    this.context        = context;
    this.databaseHelper = databaseHelper;
  }

  protected void notifyConversationListeners(Set<Long> threadIds) {
    for (long threadId : threadIds)
      notifyConversationListeners(threadId);
  }

  protected void notifyConversationListeners(long threadId) {
    context.getContentResolver().notifyChange(Uri.parse(CONVERSATION_URI + threadId), null);
  }

  protected void notifyConversationListListeners() {
    context.getContentResolver().notifyChange(Uri.parse(CONVERSATION_LIST_URI), null);
  }

  protected void setNotifyConverationListeners(Cursor cursor, long threadId) {
    cursor.setNotificationUri(context.getContentResolver(), Uri.parse(CONVERSATION_URI + threadId));
  }

  protected void setNotifyConverationListListeners(Cursor cursor) {
    cursor.setNotificationUri(context.getContentResolver(), Uri.parse(CONVERSATION_LIST_URI));
  }

  public void reset(SQLiteOpenHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

}
