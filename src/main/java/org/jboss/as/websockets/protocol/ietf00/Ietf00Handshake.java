package org.jboss.as.websockets.protocol.ietf00;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
  public WebSocket getWebSocket(HttpEvent event) throws IOException {
    return Hybi00Socket.from(event);
  }

  @Override
  public byte[] generateResponse(HttpEvent event) throws IOException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();

    if (WebSocketHeaders.ORIGIN.isIn(request)) {
      WebSocketHeaders.SEC_WEBSOCKET_ORIGIN.set(response, WebSocketHeaders.ORIGIN.get(request));
    }

    final String origin = "ws://" + request.getHeader("Host") + request.getRequestURI();

    WebSocketHeaders.SEC_WEBSOCKET_LOCATION.set(response, origin);
    WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL.copy(request, response);

    // Calculate the answer of the challenge.
    final String key1 = SEC_WEBSOCKET_KEY1.get(request);
    final String key2 = SEC_WEBSOCKET_KEY2.get(request);
    final byte[] key3 = new byte[8];

    final InputStream inputStream = request.getInputStream();
    inputStream.read(key3);

    final byte[] solution = solve(getHashAlgorithm(), key1, key2, key3);

    return solution;
  }

  public static byte[] solve(final String hashAlgorithm, String encodedKey1, String encodedKey2, byte[] key3) {
    return solve(hashAlgorithm, decodeKey(encodedKey1), decodeKey(encodedKey2), key3);
  }

  public static byte[] solve(final String hashAlgorithm, long key1, long key2, byte[] key3) {
    ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);

    buffer.putInt((int) key1);
    buffer.putInt((int) key2);
    buffer.put(key3);

    final byte[] solution = new byte[16];
    buffer.rewind();
    buffer.get(solution);
    try {
      final MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
      final byte[] solutionMD5 = digest.digest(solution);

      System.out.println("calculates solution to challenge: " + Arrays.toString(solutionMD5));

      return solutionMD5;
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("error generating hash", e);
    }
  }

  public static long decodeKey(final String encoded) {
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
