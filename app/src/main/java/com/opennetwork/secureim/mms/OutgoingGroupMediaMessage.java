package com.opennetwork.secureim.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.attachments.Attachment;
import com.opennetwork.secureim.database.ThreadDatabase;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.Base64;
import com.opennetwork.imservice.internal.push.OpenNetworkServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OutgoingGroupMediaMessage extends OutgoingSecureMediaMessage {

  private final GroupContext group;

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull String encodedGroupContext,
                                   @NonNull List<Attachment> avatar,
                                   long sentTimeMillis,
                                   long expiresIn)
      throws IOException
  {
    super(recipient, encodedGroupContext, avatar, sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn);

    this.group = GroupContext.parseFrom(Base64.decode(encodedGroupContext));
  }

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull GroupContext group,
                                   @Nullable final Attachment avatar,
                                   long sentTimeMillis,
                                   long expireIn)
  {
    super(recipient, Base64.encodeBytes(group.toByteArray()),
          new LinkedList<Attachment>() {{if (avatar != null) add(avatar);}},
          System.currentTimeMillis(),
          ThreadDatabase.DistributionTypes.CONVERSATION, expireIn);

    this.group = group;
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public boolean isGroupUpdate() {
    return group.getType().getNumber() == GroupContext.Type.UPDATE_VALUE;
  }

  public boolean isGroupQuit() {
    return group.getType().getNumber() == GroupContext.Type.QUIT_VALUE;
  }

  public GroupContext getGroupContext() {
    return group;
  }
}
