package com.opennetwork.secureim.sms;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceGroup;

public class IncomingJoinedMessage extends IncomingTextMessage {

  public IncomingJoinedMessage(Address sender) {
    super(sender, 1, System.currentTimeMillis(), null, Optional.<OpenNetworkServiceGroup>absent(), 0);
  }

  @Override
  public boolean isJoined() {
    return true;
  }

  @Override
  public boolean isSecureMessage() {
    return true;
  }

}
