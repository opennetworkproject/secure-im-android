package com.opennetwork.secureim.crypto;

import android.content.Context;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.crypto.storage.TextSecureSessionStore;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.libim.OpenNetworkProtocolAddress;
import com.opennetwork.libim.state.SessionRecord;
import com.opennetwork.libim.state.SessionStore;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;

import java.util.List;

public class SessionUtil {

  public static boolean hasSession(Context context, MasterSecret masterSecret, Recipient recipient) {
    return hasSession(context, masterSecret, recipient.getAddress());
  }

  public static boolean hasSession(Context context, MasterSecret masterSecret, @NonNull Address address) {
    SessionStore          sessionStore   = new TextSecureSessionStore(context, masterSecret);
    OpenNetworkProtocolAddress axolotlAddress = new OpenNetworkProtocolAddress(address.serialize(), OpenNetworkServiceAddress.DEFAULT_DEVICE_ID);

    return sessionStore.containsSession(axolotlAddress);
  }

  public static void archiveSiblingSessions(Context context, OpenNetworkProtocolAddress address) {
    SessionStore  sessionStore = new TextSecureSessionStore(context);
    List<Integer> devices      = sessionStore.getSubDeviceSessions(address.getName());
    devices.add(1);

    for (int device : devices) {
      if (device != address.getDeviceId()) {
        OpenNetworkProtocolAddress sibling = new OpenNetworkProtocolAddress(address.getName(), device);

        if (sessionStore.containsSession(sibling)) {
          SessionRecord sessionRecord = sessionStore.loadSession(sibling);
          sessionRecord.archiveCurrentState();
          sessionStore.storeSession(sibling, sessionRecord);
        }
      }
    }
  }

  public static void archiveAllSessions(Context context) {
    new TextSecureSessionStore(context).archiveAllSessions();
  }
}
