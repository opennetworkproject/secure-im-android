package com.opennetwork.secureim.crypto.storage;

import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.crypto.IdentityKeyUtil;
import com.opennetwork.secureim.crypto.SessionUtil;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.IdentityDatabase;
import com.opennetwork.secureim.database.IdentityDatabase.IdentityRecord;
import com.opennetwork.secureim.database.IdentityDatabase.VerifiedStatus;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.IdentityUtil;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.libim.IdentityKey;
import com.opennetwork.libim.IdentityKeyPair;
import com.opennetwork.libim.OpenNetworkProtocolAddress;
import com.opennetwork.libim.state.IdentityKeyStore;
import com.opennetwork.libim.util.guava.Optional;

import java.util.concurrent.TimeUnit;

public class TextSecureIdentityKeyStore implements IdentityKeyStore {

  private static final int TIMESTAMP_THRESHOLD_SECONDS = 5;

  private static final String TAG = TextSecureIdentityKeyStore.class.getSimpleName();
  private static final Object LOCK = new Object();

  private final Context context;

  public TextSecureIdentityKeyStore(Context context) {
    this.context = context;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return IdentityKeyUtil.getIdentityKeyPair(context);
  }

  @Override
  public int getLocalRegistrationId() {
    return TextSecurePreferences.getLocalRegistrationId(context);
  }

  public boolean saveIdentity(OpenNetworkProtocolAddress address, IdentityKey identityKey, boolean nonBlockingApproval) {
    synchronized (LOCK) {
      IdentityDatabase         identityDatabase = DatabaseFactory.getIdentityDatabase(context);
      Address                  opennetworkAddress    = Address.fromExternal(context, address.getName());
      Optional<IdentityRecord> identityRecord   = identityDatabase.getIdentity(opennetworkAddress);

      if (!identityRecord.isPresent()) {
        Log.w(TAG, "Saving new identity...");
        identityDatabase.saveIdentity(opennetworkAddress, identityKey, VerifiedStatus.DEFAULT, true, System.currentTimeMillis(), nonBlockingApproval);
        return false;
      }

      if (!identityRecord.get().getIdentityKey().equals(identityKey)) {
        Log.w(TAG, "Replacing existing identity...");
        VerifiedStatus verifiedStatus;

        if (identityRecord.get().getVerifiedStatus() == VerifiedStatus.VERIFIED ||
            identityRecord.get().getVerifiedStatus() == VerifiedStatus.UNVERIFIED)
        {
          verifiedStatus = VerifiedStatus.UNVERIFIED;
        } else {
          verifiedStatus = VerifiedStatus.DEFAULT;
        }

        identityDatabase.saveIdentity(opennetworkAddress, identityKey, verifiedStatus, false, System.currentTimeMillis(), nonBlockingApproval);
        IdentityUtil.markIdentityUpdate(context, Recipient.from(context, opennetworkAddress, true));
        SessionUtil.archiveSiblingSessions(context, address);
        return true;
      }

      if (isNonBlockingApprovalRequired(identityRecord.get())) {
        Log.w(TAG, "Setting approval status...");
        identityDatabase.setApproval(opennetworkAddress, nonBlockingApproval);
        return false;
      }

      return false;
    }
  }

  @Override
  public boolean saveIdentity(OpenNetworkProtocolAddress address, IdentityKey identityKey) {
    return saveIdentity(address, identityKey, false);
  }

  @Override
  public boolean isTrustedIdentity(OpenNetworkProtocolAddress address, IdentityKey identityKey, Direction direction) {
    synchronized (LOCK) {
      IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(context);
      String           ourNumber        = TextSecurePreferences.getLocalNumber(context);
      Address          theirAddress     = Address.fromExternal(context, address.getName());

      if (ourNumber.equals(address.getName()) || Address.fromSerialized(ourNumber).equals(theirAddress)) {
        return identityKey.equals(IdentityKeyUtil.getIdentityKey(context));
      }

      switch (direction) {
        case SENDING:   return isTrustedForSending(identityKey, identityDatabase.getIdentity(theirAddress));
        case RECEIVING: return true;
        default:        throw new AssertionError("Unknown direction: " + direction);
      }
    }
  }

  private boolean isTrustedForSending(IdentityKey identityKey, Optional<IdentityRecord> identityRecord) {
    if (!identityRecord.isPresent()) {
      Log.w(TAG, "Nothing here, returning true...");
      return true;
    }

    if (!identityKey.equals(identityRecord.get().getIdentityKey())) {
      Log.w(TAG, "Identity keys don't match...");
      return false;
    }

    if (identityRecord.get().getVerifiedStatus() == VerifiedStatus.UNVERIFIED) {
      Log.w(TAG, "Needs unverified approval!");
      return false;
    }

    if (isNonBlockingApprovalRequired(identityRecord.get())) {
      Log.w(TAG, "Needs non-blocking approval!");
      return false;
    }

    return true;
  }

  private boolean isNonBlockingApprovalRequired(IdentityRecord identityRecord) {
    return !identityRecord.isFirstUse() &&
           System.currentTimeMillis() - identityRecord.getTimestamp() < TimeUnit.SECONDS.toMillis(TIMESTAMP_THRESHOLD_SECONDS) &&
           !identityRecord.isApprovedNonBlocking();
  }
}
