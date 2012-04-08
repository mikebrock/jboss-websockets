package org.jboss.as.websockets.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author Mike Brock
 */
public final class Hash {
  private Hash() {}

  final static String secureRandomAlgorithm = "SHA1PRNG";
  final static String hashAlgorithm = "SHA1";
  final static SecureRandom random;

  static {
    try {
      random = SecureRandom.getInstance(secureRandomAlgorithm);
      random.setSeed(SecureRandom.getInstance(secureRandomAlgorithm).generateSeed(64));
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("runtime does not support secure random algorithm: " + secureRandomAlgorithm);
    }
  }

  public static String newUniqueHash() {
    return nextSecureHash(hashAlgorithm, String.valueOf(System.nanoTime()));
  }

  private static String nextSecureHash(final String algorithm, final String additionalSeed) {
    try {
      final MessageDigest md = MessageDigest.getInstance(algorithm);

      md.update(String.valueOf(System.nanoTime()).getBytes());

      if (additionalSeed != null) {
        md.update(additionalSeed.getBytes());
      }

      byte[] randBytes = new byte[64];
      random.nextBytes(randBytes);

      for (int i = 0; i < 1000; i++) {
        md.update(md.digest());
      }

      return hashToHexString(md.digest());
    }
    catch (Exception e) {
      throw new RuntimeException("failed to generate session id hash", e);
    }
  }

  private static String hashToHexString(byte[] hash) {
    final StringBuilder hexString = new StringBuilder(hash.length);
    for (byte mdbyte : hash) {
      hexString.append(Integer.toHexString(0xFF & mdbyte));
    }
    return hexString.toString();
  }
}
