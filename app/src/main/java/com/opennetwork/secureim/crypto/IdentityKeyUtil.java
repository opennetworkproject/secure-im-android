package com.opennetwork.secureim.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.util.Base64;
import com.opennetwork.libim.IdentityKey;
import com.opennetwork.libim.IdentityKeyPair;
import com.opennetwork.libim.InvalidKeyException;
import com.opennetwork.libim.ecc.Curve;
import com.opennetwork.libim.ecc.ECKeyPair;
import com.opennetwork.libim.ecc.ECPrivateKey;

import java.io.IOException;

/**
 * Utility class for working with identity keys.
 * 
 *
 */

public class IdentityKeyUtil {

  private static final String TAG = IdentityKeyUtil.class.getSimpleName();

  private static final String IDENTITY_PUBLIC_KEY_CIPHERTEXT_LEGACY_PREF  = "pref_identity_public_curve25519";
  private static final String IDENTITY_PRIVATE_KEY_CIPHERTEXT_LEGACY_PREF = "pref_identity_private_curve25519";

  private static final String IDENTITY_PUBLIC_KEY_PREF                    = "pref_identity_public_v3";
  private static final String IDENTITY_PRIVATE_KEY_PREF                   = "pref_identity_private_v3";

  public static boolean hasIdentityKey(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(MasterSecretUtil.PREFERENCES_NAME, 0);

    return
        preferences.contains(IDENTITY_PUBLIC_KEY_PREF) &&
        preferences.contains(IDENTITY_PRIVATE_KEY_PREF);
  }

  public static @NonNull IdentityKey getIdentityKey(@NonNull Context context) {
    if (!hasIdentityKey(context)) throw new AssertionError("There isn't one!");

    try {
      byte[] publicKeyBytes = Base64.decode(retrieve(context, IDENTITY_PUBLIC_KEY_PREF));
      return new IdentityKey(publicKeyBytes, 0);
    } catch (IOException | InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }

  public static @NonNull IdentityKeyPair getIdentityKeyPair(@NonNull Context context) {
    if (!hasIdentityKey(context)) throw new AssertionError("There isn't one!");

    try {
      IdentityKey  publicKey  = getIdentityKey(context);
      ECPrivateKey privateKey = Curve.decodePrivatePoint(Base64.decode(retrieve(context, IDENTITY_PRIVATE_KEY_PREF)));

      return new IdentityKeyPair(publicKey, privateKey);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static void generateIdentityKeys(Context context) {
    ECKeyPair    djbKeyPair     = Curve.generateKeyPair();
    IdentityKey  djbIdentityKey = new IdentityKey(djbKeyPair.getPublicKey());
    ECPrivateKey djbPrivateKey  = djbKeyPair.getPrivateKey();

    save(context, IDENTITY_PUBLIC_KEY_PREF, Base64.encodeBytes(djbIdentityKey.serialize()));
    save(context, IDENTITY_PRIVATE_KEY_PREF, Base64.encodeBytes(djbPrivateKey.serialize()));
  }

  public static void migrateIdentityKeys(@NonNull Context context,
                                         @NonNull MasterSecret masterSecret)
  {
    if (!hasIdentityKey(context)) {
      if (hasLegacyIdentityKeys(context)) {
        IdentityKeyPair legacyPair = getLegacyIdentityKeyPair(context, masterSecret);

        save(context, IDENTITY_PUBLIC_KEY_PREF, Base64.encodeBytes(legacyPair.getPublicKey().serialize()));
        save(context, IDENTITY_PRIVATE_KEY_PREF, Base64.encodeBytes(legacyPair.getPrivateKey().serialize()));

        delete(context, IDENTITY_PUBLIC_KEY_CIPHERTEXT_LEGACY_PREF);
        delete(context, IDENTITY_PRIVATE_KEY_CIPHERTEXT_LEGACY_PREF);
      } else {
        generateIdentityKeys(context);
      }
    }
  }

  private static boolean hasLegacyIdentityKeys(Context context) {
    return
        retrieve(context, IDENTITY_PUBLIC_KEY_CIPHERTEXT_LEGACY_PREF) != null &&
        retrieve(context, IDENTITY_PRIVATE_KEY_CIPHERTEXT_LEGACY_PREF) != null;
  }

  private static IdentityKeyPair getLegacyIdentityKeyPair(@NonNull Context context,
                                                          @NonNull MasterSecret masterSecret)
  {
    try {
      MasterCipher masterCipher   = new MasterCipher(masterSecret);
      byte[]       publicKeyBytes = Base64.decode(retrieve(context, IDENTITY_PUBLIC_KEY_CIPHERTEXT_LEGACY_PREF));
      IdentityKey  identityKey    = new IdentityKey(publicKeyBytes, 0);
      ECPrivateKey privateKey     = masterCipher.decryptKey(Base64.decode(retrieve(context, IDENTITY_PRIVATE_KEY_CIPHERTEXT_LEGACY_PREF)));

      return new IdentityKeyPair(identityKey, privateKey);
    } catch (IOException | InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }

  private static String retrieve(Context context, String key) {
    SharedPreferences preferences = context.getSharedPreferences(MasterSecretUtil.PREFERENCES_NAME, 0);
    return preferences.getString(key, null);
  }

  private static void save(Context context, String key, String value) {
    SharedPreferences preferences   = context.getSharedPreferences(MasterSecretUtil.PREFERENCES_NAME, 0);
    Editor preferencesEditor        = preferences.edit();

    preferencesEditor.putString(key, value);
    if (!preferencesEditor.commit()) throw new AssertionError("failed to save identity key/value to shared preferences");
  }

  private static void delete(Context context, String key) {
    context.getSharedPreferences(MasterSecretUtil.PREFERENCES_NAME, 0).edit().remove(key).commit();
  }

}
