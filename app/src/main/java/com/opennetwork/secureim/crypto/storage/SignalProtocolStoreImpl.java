package com.opennetwork.secureim.crypto.storage;

import android.content.Context;

import com.opennetwork.libim.IdentityKey;
import com.opennetwork.libim.IdentityKeyPair;
import com.opennetwork.libim.InvalidKeyIdException;
import com.opennetwork.libim.OpenNetworkProtocolAddress;
import com.opennetwork.libim.state.IdentityKeyStore;
import com.opennetwork.libim.state.PreKeyRecord;
import com.opennetwork.libim.state.PreKeyStore;
import com.opennetwork.libim.state.SessionRecord;
import com.opennetwork.libim.state.SessionStore;
import com.opennetwork.libim.state.OpenNetworkProtocolStore;
import com.opennetwork.libim.state.SignedPreKeyRecord;
import com.opennetwork.libim.state.SignedPreKeyStore;

import java.util.List;

public class OpenNetworkProtocolStoreImpl implements OpenNetworkProtocolStore {

  private final PreKeyStore       preKeyStore;
  private final SignedPreKeyStore signedPreKeyStore;
  private final IdentityKeyStore  identityKeyStore;
  private final SessionStore      sessionStore;

  public OpenNetworkProtocolStoreImpl(Context context) {
    this.preKeyStore       = new TextSecurePreKeyStore(context);
    this.signedPreKeyStore = new TextSecurePreKeyStore(context);
    this.identityKeyStore  = new TextSecureIdentityKeyStore(context);
    this.sessionStore      = new TextSecureSessionStore(context);
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return identityKeyStore.getIdentityKeyPair();
  }

  @Override
  public int getLocalRegistrationId() {
    return identityKeyStore.getLocalRegistrationId();
  }

  @Override
  public boolean saveIdentity(OpenNetworkProtocolAddress address, IdentityKey identityKey) {
    return identityKeyStore.saveIdentity(address, identityKey);
  }

  @Override
  public boolean isTrustedIdentity(OpenNetworkProtocolAddress address, IdentityKey identityKey, Direction direction) {
    return identityKeyStore.isTrustedIdentity(address, identityKey, direction);
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    return preKeyStore.loadPreKey(preKeyId);
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    preKeyStore.storePreKey(preKeyId, record);
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return preKeyStore.containsPreKey(preKeyId);
  }

  @Override
  public void removePreKey(int preKeyId) {
    preKeyStore.removePreKey(preKeyId);
  }

  @Override
  public SessionRecord loadSession(OpenNetworkProtocolAddress axolotlAddress) {
    return sessionStore.loadSession(axolotlAddress);
  }

  @Override
  public List<Integer> getSubDeviceSessions(String number) {
    return sessionStore.getSubDeviceSessions(number);
  }

  @Override
  public void storeSession(OpenNetworkProtocolAddress axolotlAddress, SessionRecord record) {
    sessionStore.storeSession(axolotlAddress, record);
  }

  @Override
  public boolean containsSession(OpenNetworkProtocolAddress axolotlAddress) {
    return sessionStore.containsSession(axolotlAddress);
  }

  @Override
  public void deleteSession(OpenNetworkProtocolAddress axolotlAddress) {
    sessionStore.deleteSession(axolotlAddress);
  }

  @Override
  public void deleteAllSessions(String number) {
    sessionStore.deleteAllSessions(number);
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    return signedPreKeyStore.loadSignedPreKey(signedPreKeyId);
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    return signedPreKeyStore.loadSignedPreKeys();
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return signedPreKeyStore.containsSignedPreKey(signedPreKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    signedPreKeyStore.removeSignedPreKey(signedPreKeyId);
  }
}
