package com.opennetwork.secureim.mms;

import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.database.ThreadDatabase;
import com.opennetwork.secureim.recipients.Recipient;

import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  public OutgoingExpirationUpdateMessage(Recipient recipient, long sentTimeMillis, long expiresIn) {
    super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn);
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

}
