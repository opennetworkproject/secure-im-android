package com.opennetwork.secureim;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.model.MessageRecord;
import com.opennetwork.secureim.mms.GlideRequests;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.Conversions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

class MessageDetailsRecipientAdapter extends BaseAdapter implements AbsListView.RecyclerListener {

  private final Context                       context;
  private final MasterSecret                  masterSecret;
  private final GlideRequests                 glideRequests;
  private final MessageRecord                 record;
  private final List<RecipientDeliveryStatus> members;
  private final boolean                       isPushGroup;

  MessageDetailsRecipientAdapter(@NonNull Context context, @NonNull MasterSecret masterSecret,
                                 @NonNull GlideRequests glideRequests, @NonNull MessageRecord record,
                                 @NonNull List<RecipientDeliveryStatus> members, boolean isPushGroup)
  {
    this.context       = context;
    this.masterSecret  = masterSecret;
    this.glideRequests = glideRequests;
    this.record        = record;
    this.isPushGroup   = isPushGroup;
    this.members       = members;
  }

  @Override
  public int getCount() {
    return members.size();
  }

  @Override
  public Object getItem(int position) {
    return members.get(position);
  }

  @Override
  public long getItemId(int position) {
    try {
      return Conversions.byteArrayToLong(MessageDigest.getInstance("SHA1").digest(members.get(position).recipient.getAddress().serialize().getBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(context).inflate(R.layout.message_recipient_list_item, parent, false);
    }

    RecipientDeliveryStatus member = members.get(position);

    ((MessageRecipientListItem)convertView).set(masterSecret, glideRequests, record, member, isPushGroup);
    return convertView;
  }

  @Override
  public void onMovedToScrapHeap(View view) {
    ((MessageRecipientListItem)view).unbind();
  }


  static class RecipientDeliveryStatus {

    enum Status {
      UNKNOWN, PENDING, SENT, DELIVERED, READ
    }

    private final Recipient recipient;
    private final Status    deliveryStatus;
    private final long      timestamp;

    RecipientDeliveryStatus(Recipient recipient, Status deliveryStatus, long timestamp) {
      this.recipient      = recipient;
      this.deliveryStatus = deliveryStatus;
      this.timestamp      = timestamp;
    }

    Status getDeliveryStatus() {
      return deliveryStatus;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public Recipient getRecipient() {
      return recipient;
    }

  }

}
