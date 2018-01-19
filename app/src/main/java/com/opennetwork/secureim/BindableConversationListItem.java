package com.opennetwork.secureim;

import android.support.annotation.NonNull;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.model.ThreadRecord;
import com.opennetwork.secureim.mms.GlideRequests;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

  public void bind(@NonNull MasterSecret masterSecret, @NonNull ThreadRecord thread,
                   @NonNull GlideRequests glideRequests, @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads, boolean batchMode);
}
