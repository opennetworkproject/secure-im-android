package com.opennetwork.secureim.crypto;

import android.os.Parcel;
import android.os.Parcelable;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * When a user first initializes TextSecure, a few secrets
 * are generated.  These are:
 *
 * 1) A 128bit symmetric encryption key.
 * 2) A 160bit symmetric MAC key.
 * 3) An ECC keypair.
 *
 * The first two, along with the ECC keypair's private key, are
 * then encrypted on disk using PBE.
 *
 * This class represents 1 and 2.
 */

public class MasterSecret implements Parcelable {

  private final SecretKeySpec encryptionKey;
  private final SecretKeySpec macKey;

  public static final Parcelable.Creator<MasterSecret> CREATOR = new Parcelable.Creator<MasterSecret>() {
    @Override
    public MasterSecret createFromParcel(Parcel in) {
      return new MasterSecret(in);
    }

    @Override
    public MasterSecret[] newArray(int size) {
      return new MasterSecret[size];
    }
  };

  public MasterSecret(SecretKeySpec encryptionKey, SecretKeySpec macKey) {
    this.encryptionKey = encryptionKey;
    this.macKey        = macKey;
  }

  private MasterSecret(Parcel in) {
    byte[] encryptionKeyBytes = new byte[in.readInt()];
    in.readByteArray(encryptionKeyBytes);

    byte[] macKeyBytes = new byte[in.readInt()];
    in.readByteArray(macKeyBytes);

    this.encryptionKey = new SecretKeySpec(encryptionKeyBytes, "AES");
    this.macKey        = new SecretKeySpec(macKeyBytes, "HmacSHA1");

    // SecretKeySpec does an internal copy in its constructor.
    Arrays.fill(encryptionKeyBytes, (byte) 0x00);
    Arrays.fill(macKeyBytes, (byte)0x00);
  }


  public SecretKeySpec getEncryptionKey() {
    return this.encryptionKey;
  }

  public SecretKeySpec getMacKey() {
    return this.macKey;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(encryptionKey.getEncoded().length);
    out.writeByteArray(encryptionKey.getEncoded());
    out.writeInt(macKey.getEncoded().length);
    out.writeByteArray(macKey.getEncoded());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public MasterSecret parcelClone() {
    Parcel thisParcel = Parcel.obtain();
    Parcel thatParcel = Parcel.obtain();
    byte[] bytes      = null;

    thisParcel.writeValue(this);
    bytes = thisParcel.marshall();

    thatParcel.unmarshall(bytes, 0, bytes.length);
    thatParcel.setDataPosition(0);

    MasterSecret that = (MasterSecret)thatParcel.readValue(MasterSecret.class.getClassLoader());

    thisParcel.recycle();
    thatParcel.recycle();

    return that;
  }

}
