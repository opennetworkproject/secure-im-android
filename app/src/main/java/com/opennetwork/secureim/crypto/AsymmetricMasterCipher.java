package com.opennetwork.secureim.crypto;

import com.opennetwork.secureim.util.Base64;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.libim.InvalidKeyException;
import com.opennetwork.libim.InvalidMessageException;
import com.opennetwork.libim.ecc.Curve;
import com.opennetwork.libim.ecc.ECKeyPair;
import com.opennetwork.libim.ecc.ECPrivateKey;
import com.opennetwork.libim.ecc.ECPublicKey;
import com.opennetwork.secureim.util.Conversions;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is used to asymmetricly encrypt local data.  This is used in the case
 * where TextSecure receives an SMS, but the user's local encryption passphrase is
 * not cached (either because of a timeout, or because it hasn't yet been entered).
 * 
 * In this case, we have access to the public key of a local keypair.  We encrypt
 * the message with this, and put it into the DB.  When the user enters their passphrase,
 * we can get access to the private key of the local keypair, decrypt the message, and
 * replace it into the DB with symmetric encryption.
 * 
 * The encryption protocol is as follows:
 * 
 * 1) Generate an ephemeral keypair.
 * 2) Do ECDH with the public key of the local durable keypair.
 * 3) Do KMF with the ECDH result to obtain a master secret.
 * 4) Encrypt the message with that master secret.
 * 
 */
public class AsymmetricMasterCipher {

  private final AsymmetricMasterSecret asymmetricMasterSecret;

  public AsymmetricMasterCipher(AsymmetricMasterSecret asymmetricMasterSecret) {
    this.asymmetricMasterSecret = asymmetricMasterSecret;
  }

  public byte[] encryptBytes(byte[] body) {
    try {
      ECPublicKey  theirPublic        = asymmetricMasterSecret.getDjbPublicKey();
      ECKeyPair    ourKeyPair         = Curve.generateKeyPair();
      byte[]       secret             = Curve.calculateAgreement(theirPublic, ourKeyPair.getPrivateKey());
      MasterCipher masterCipher       = getMasterCipherForSecret(secret);
      byte[]       encryptedBodyBytes = masterCipher.encryptBytes(body);

      PublicKey    ourPublicKey       = new PublicKey(31337, ourKeyPair.getPublicKey());
      byte[]       publicKeyBytes     = ourPublicKey.serialize();

      return Util.combine(publicKeyBytes, encryptedBodyBytes);
    } catch (InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] decryptBytes(byte[] combined) throws IOException, InvalidMessageException {
    try {
      byte[][]  parts          = Util.split(combined, PublicKey.KEY_SIZE, combined.length - PublicKey.KEY_SIZE);
      PublicKey theirPublicKey = new PublicKey(parts[0], 0);

      ECPrivateKey ourPrivateKey = asymmetricMasterSecret.getPrivateKey();
      byte[]       secret        = Curve.calculateAgreement(theirPublicKey.getKey(), ourPrivateKey);
      MasterCipher masterCipher  = getMasterCipherForSecret(secret);

      return masterCipher.decryptBytes(parts[1]);
    } catch (InvalidKeyException e) {
      throw new InvalidMessageException(e);
    }
  }

  public String decryptBody(String body) throws IOException, InvalidMessageException {
    byte[] combined = Base64.decode(body);
    return new String(decryptBytes(combined));
  }

  public String encryptBody(String body) {
    return Base64.encodeBytes(encryptBytes(body.getBytes()));
  }

  private MasterCipher getMasterCipherForSecret(byte[] secretBytes) {
    SecretKeySpec cipherKey   = deriveCipherKey(secretBytes);
    SecretKeySpec macKey      = deriveMacKey(secretBytes);
    MasterSecret masterSecret = new MasterSecret(cipherKey, macKey);

    return new MasterCipher(masterSecret);
  }

  private SecretKeySpec deriveMacKey(byte[] secretBytes) {
    byte[] digestedBytes = getDigestedBytes(secretBytes, 1);
    byte[] macKeyBytes   = new byte[20];

    System.arraycopy(digestedBytes, 0, macKeyBytes, 0, macKeyBytes.length);
    return new SecretKeySpec(macKeyBytes, "HmacSHA1");
  }

  private SecretKeySpec deriveCipherKey(byte[] secretBytes) {
    byte[] digestedBytes  = getDigestedBytes(secretBytes, 0);
    byte[] cipherKeyBytes = new byte[16];

    System.arraycopy(digestedBytes, 0, cipherKeyBytes, 0, cipherKeyBytes.length);
    return new SecretKeySpec(cipherKeyBytes, "AES");
  }

  private byte[] getDigestedBytes(byte[] secretBytes, int iteration) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
      return mac.doFinal(Conversions.intToByteArray(iteration));
    } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }
}
