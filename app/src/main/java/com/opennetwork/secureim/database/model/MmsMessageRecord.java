package com.opennetwork.secureim.database.model;


import android.content.Context;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.database.documents.IdentityKeyMismatch;
import com.opennetwork.secureim.database.documents.NetworkFailure;
import com.opennetwork.secureim.mms.Slide;
import com.opennetwork.secureim.mms.SlideDeck;
import com.opennetwork.secureim.recipients.Recipient;

import java.util.List;

public abstract class MmsMessageRecord extends MessageRecord {

  private final @NonNull SlideDeck slideDeck;

  MmsMessageRecord(Context context, long id, Body body, Recipient conversationRecipient,
                   Recipient individualRecipient, int recipientDeviceId, long dateSent,
                   long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                   long type, List<IdentityKeyMismatch> mismatches,
                   List<NetworkFailure> networkFailures, int subscriptionId, long expiresIn,
                   long expireStarted, @NonNull SlideDeck slideDeck, int readReceiptCount)
  {
    super(context, id, body, conversationRecipient, individualRecipient, recipientDeviceId, dateSent, dateReceived, threadId, deliveryStatus, deliveryReceiptCount, type, mismatches, networkFailures, subscriptionId, expiresIn, expireStarted, readReceiptCount);
    this.slideDeck = slideDeck;
  }

  @Override
  public boolean isMms() {
    return true;
  }

  @NonNull
  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @Override
  public boolean isMediaPending() {
    for (Slide slide : getSlideDeck().getSlides()) {
      if (slide.isInProgress() || slide.isPendingDownload()) {
        return true;
      }
    }

    return false;
  }

  public boolean containsMediaSlide() {
    return slideDeck.containsMediaSlide();
  }


}
