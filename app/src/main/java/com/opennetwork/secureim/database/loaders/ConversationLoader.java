package com.opennetwork.secureim.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.util.AbstractCursorLoader;
import com.opennetwork.libim.util.Pair;

public class ConversationLoader extends AbstractCursorLoader {
  private final long    threadId;
  private       long    limit;
  private       long    lastSeen;
  private       boolean hasSent;

  public ConversationLoader(Context context, long threadId, long limit, long lastSeen) {
    super(context);
    this.threadId = threadId;
    this.limit    = limit;
    this.lastSeen = lastSeen;
    this.hasSent  = true;
  }

  public boolean hasLimit() {
    return limit > 0;
  }

  public long getLastSeen() {
    return lastSeen;
  }

  public boolean hasSent() {
    return hasSent;
  }

  @Override
  public Cursor getCursor() {
    Pair<Long, Boolean> lastSeenAndHasSent = DatabaseFactory.getThreadDatabase(context).getLastSeenAndHasSent(threadId);

    this.hasSent = lastSeenAndHasSent.second();

    if (lastSeen == -1) {
      this.lastSeen = lastSeenAndHasSent.first();
    }

    return DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId, limit);
  }
}
