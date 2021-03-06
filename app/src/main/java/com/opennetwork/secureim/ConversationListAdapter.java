package com.opennetwork.secureim;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.opennetwork.secureim.crypto.MasterCipher;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.CursorRecyclerViewAdapter;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.ThreadDatabase;
import com.opennetwork.secureim.database.model.ThreadRecord;
import com.opennetwork.secureim.mms.GlideRequests;
import com.opennetwork.secureim.util.Conversions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A CursorAdapter for building a list of conversation threads.
 */
class ConversationListAdapter extends CursorRecyclerViewAdapter<ConversationListAdapter.ViewHolder> {

  private static final int MESSAGE_TYPE_SWITCH_ARCHIVE = 1;
  private static final int MESSAGE_TYPE_THREAD         = 2;
  private static final int MESSAGE_TYPE_INBOX_ZERO     = 3;

  private final @NonNull  ThreadDatabase    threadDatabase;
  private final @NonNull  MasterSecret      masterSecret;
  private final @NonNull  MasterCipher      masterCipher;
  private final @NonNull  GlideRequests     glideRequests;
  private final @NonNull  Locale            locale;
  private final @NonNull  LayoutInflater    inflater;
  private final @Nullable ItemClickListener clickListener;
  private final @NonNull  MessageDigest     digest;

  private final Set<Long> batchSet  = Collections.synchronizedSet(new HashSet<Long>());
  private       boolean   batchMode = false;

  protected static class ViewHolder extends RecyclerView.ViewHolder {
    public <V extends View & BindableConversationListItem> ViewHolder(final @NonNull V itemView)
    {
      super(itemView);
    }

    public BindableConversationListItem getItem() {
      return (BindableConversationListItem)itemView;
    }
  }

  @Override
  public long getItemId(@NonNull Cursor cursor) {
    ThreadRecord  record  = getThreadRecord(cursor);

    return Conversions.byteArrayToLong(digest.digest(record.getRecipient().getAddress().serialize().getBytes()));
  }

  ConversationListAdapter(@NonNull Context context,
                          @NonNull MasterSecret masterSecret,
                          @NonNull GlideRequests glideRequests,
                          @NonNull Locale locale,
                          @Nullable Cursor cursor,
                          @Nullable ItemClickListener clickListener)
  {
    super(context, cursor);
    try {
      this.masterSecret   = masterSecret;
      this.masterCipher   = new MasterCipher(masterSecret);
      this.glideRequests  = glideRequests;
      this.threadDatabase = DatabaseFactory.getThreadDatabase(context);
      this.locale         = locale;
      this.inflater       = LayoutInflater.from(context);
      this.clickListener  = clickListener;
      this.digest         = MessageDigest.getInstance("SHA1");
      setHasStableIds(true);
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError("SHA-1 missing");
    }
  }

  @Override
  public ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
    if (viewType == MESSAGE_TYPE_SWITCH_ARCHIVE) {
      ConversationListItemAction action = (ConversationListItemAction) inflater.inflate(R.layout.conversation_list_item_action,
                                                                                        parent, false);

      action.setOnClickListener(v -> {
        if (clickListener != null) clickListener.onSwitchToArchive();
      });

      return new ViewHolder(action);
    } else if (viewType == MESSAGE_TYPE_INBOX_ZERO) {
      return new ViewHolder((ConversationListItemInboxZero)inflater.inflate(R.layout.conversation_list_item_inbox_zero, parent, false));
    } else {
      final ConversationListItem item = (ConversationListItem)inflater.inflate(R.layout.conversation_list_item_view,
                                                                               parent, false);

      item.setOnClickListener(view -> {
        if (clickListener != null) clickListener.onItemClick(item);
      });

      item.setOnLongClickListener(view -> {
        if (clickListener != null) clickListener.onItemLongClick(item);
        return true;
      });

      return new ViewHolder(item);
    }
  }

  @Override
  public void onItemViewRecycled(ViewHolder holder) {
    holder.getItem().unbind();
  }

  @Override
  public void onBindItemViewHolder(ViewHolder viewHolder, @NonNull Cursor cursor) {
    viewHolder.getItem().bind(masterSecret, getThreadRecord(cursor), glideRequests, locale, batchSet, batchMode);
  }

  @Override
  public int getItemViewType(@NonNull Cursor cursor) {
    ThreadRecord threadRecord = getThreadRecord(cursor);

    if (threadRecord.getDistributionType() == ThreadDatabase.DistributionTypes.ARCHIVE) {
      return MESSAGE_TYPE_SWITCH_ARCHIVE;
    } else if (threadRecord.getDistributionType() == ThreadDatabase.DistributionTypes.INBOX_ZERO) {
      return MESSAGE_TYPE_INBOX_ZERO;
    } else {
      return MESSAGE_TYPE_THREAD;
    }
  }

  private ThreadRecord getThreadRecord(@NonNull Cursor cursor) {
    return threadDatabase.readerFor(cursor, masterCipher).getCurrent();
  }

  public void toggleThreadInBatchSet(long threadId) {
    if (batchSet.contains(threadId)) {
      batchSet.remove(threadId);
    } else if (threadId != -1) {
      batchSet.add(threadId);
    }
  }

  public Set<Long> getBatchSelections() {
    return batchSet;
  }

  public void initializeBatchMode(boolean toggle) {
    this.batchMode = toggle;
    unselectAllThreads();
  }

  public void unselectAllThreads() {
    this.batchSet.clear();
    this.notifyDataSetChanged();
  }

  public void selectAllThreads() {
    for (int i = 0; i < getItemCount(); i++) {
      long threadId = getThreadRecord(getCursorAtPositionOrThrow(i)).getThreadId();
      if (threadId != -1) batchSet.add(threadId);
    }
    this.notifyDataSetChanged();
  }

  interface ItemClickListener {
    void onItemClick(ConversationListItem item);
    void onItemLongClick(ConversationListItem item);
    void onSwitchToArchive();
  }
}
