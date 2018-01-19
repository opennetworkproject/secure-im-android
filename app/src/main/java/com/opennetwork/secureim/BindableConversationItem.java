package com.opennetwork.secureim;

import android.support.annotation.NonNull;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.model.MessageRecord;
import com.opennetwork.secureim.mms.GlideRequests;
import com.opennetwork.secureim.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationItem extends Unbindable {
  void bind(@NonNull MasterSecret masterSecret,
            @NonNull MessageRecord messageRecord,
            @NonNull GlideRequests glideRequests,
            @NonNull Locale locale,
            @NonNull Set<MessageRecord> batchSelected,
            @NonNull Recipient recipients);

  MessageRecord getMessageRecord();
}
