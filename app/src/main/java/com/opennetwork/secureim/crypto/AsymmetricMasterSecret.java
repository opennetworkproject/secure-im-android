package com.opennetwork.secureim.crypto;

import com.opennetwork.libim.ecc.ECPrivateKey;
import com.opennetwork.libim.ecc.ECPublicKey;

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
 * This class represents the ECC keypair.
 * 
 */

public class AsymmetricMasterSecret {

  private final ECPublicKey djbPublicKey;
  private final ECPrivateKey djbPrivateKey;


  public AsymmetricMasterSecret(ECPublicKey djbPublicKey, ECPrivateKey djbPrivateKey)
  {
    this.djbPublicKey   = djbPublicKey;
    this.djbPrivateKey  = djbPrivateKey;
  }

  public ECPublicKey getDjbPublicKey() {
    return djbPublicKey;
  }


  public ECPrivateKey getPrivateKey() {
    return djbPrivateKey;
  }

}
