package org.jboss.as.websockets.util;

/**
 * @author Mike Brock
 */
public class Base64 {
  public static String encode(byte[] hash) {
    final StringBuilder hexString = new StringBuilder(hash.length);
    for (byte mdbyte : hash) {
      hexString.append(Integer.toHexString(0xFF & mdbyte));
    }
    return hexString.toString();
  }
}
