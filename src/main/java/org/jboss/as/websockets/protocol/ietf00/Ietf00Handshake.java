package org.jboss.as.websockets.protocol.ietf00;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY1;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY2;

/**
 * @author Mike Brock
 */
public class Ietf00Handshake extends Handshake {
  public Ietf00Handshake() {
    super("0", "MD5", null);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return SEC_WEBSOCKET_KEY1.isIn(request) && SEC_WEBSOCKET_KEY2.isIn(request);
  }

  @Override
  public void generateResponse(HttpEvent event) throws IOException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();

    WebSocketHeaders.SEC_WEBSOCKET_ORIGIN.copy(request, response);

    // Calculate the answer of the challenge.
    final String key1 = SEC_WEBSOCKET_KEY1.get(request);
    final String key2 = SEC_WEBSOCKET_KEY2.get(request);
    final byte[] key3 = new byte[8];

    final InputStream inputStream = request.getInputStream();
    for (int i = 0; i < 8; i++) {
      key3[i] = (byte) inputStream.read();
    }

    final byte[] solution = solve(key1, key2, key3);

    response.getOutputStream().write(solution);
  }

  public byte[] solve(String encodedKey1, String encodedKey2, byte[] key3) {
    return solve(decodeKey(encodedKey1), decodeKey(encodedKey2), key3);
  }

  public byte[] solve(long key1, long key2, byte[] key3) {
    ByteBuffer buffer = ByteBuffer.allocate(16);

    buffer.putInt((int) key1);
    buffer.putInt((int) key2);
    buffer.put(key3);

    final byte[] solution = new byte[16];
    buffer.rewind();
    buffer.get(solution);
    try {
      final MessageDigest digest = MessageDigest.getInstance(getHashAlgorithm());
      final byte[] solutionMD5 = digest.digest(solution);

      return solutionMD5;
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("error generating hash", e);
    }
  }


  public static long decodeKey(String encoded) {
    final int len = encoded.length();
    int numSpaces = 0;

    for (int i = 0; i < len; ++i) {
      if (encoded.charAt(i) == ' ') {
        ++numSpaces;
      }
    }

    final String digits = encoded.replaceAll("[^0-9]", "");
    final long product = Long.parseLong(digits);
    final long key = product / numSpaces;

    return key;
  }


}
