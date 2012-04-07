package org.jboss.as.websockets.protocol.ietf07;


import com.sun.xml.internal.fastinfoset.algorithm.IEEE754FloatingPointEncodingAlgorithm;
import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.as.websockets.util.Base64;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.jboss.as.websockets.WebSocketHeaders.ORIGIN;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_LOCATION;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_ORIGIN;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_VERSION;

/**
 * @author Mike Brock
 */
public class Ietf07Handshake extends Handshake {
  protected Ietf07Handshake(final String version) {
    super(version, "SHA1", "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
  }

  public Ietf07Handshake() {
    this("7");
  }

  @Override
  public WebSocket getWebSocket(HttpEvent event) throws IOException {
    return Hybi07Socket.from(event);
  }

  @Override
  public boolean matches(final HttpServletRequest request) {
    return (SEC_WEBSOCKET_KEY.isIn(request) && SEC_WEBSOCKET_VERSION.matches(request, getVersion()));
  }

  @Override
  public byte[] generateResponse(HttpEvent event) throws IOException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();

    if (ORIGIN.isIn(request)) {
      SEC_WEBSOCKET_ORIGIN.set(response, ORIGIN.get(request));
    }

    SEC_WEBSOCKET_PROTOCOL.copy(request, response);

    SEC_WEBSOCKET_LOCATION.set(response, getWebSocketLocation(request));

    final String key = SEC_WEBSOCKET_KEY.get(request);
    final String solution = solve(key);

    WebSocketHeaders.SEC_WEBSOCKET_ACCEPT.set(response, solution);

    return new byte[0];
  }

  public String solve(final String nonceBase64) {
    try {
      final String concat = nonceBase64.trim().concat(getMagicNumber());
      final MessageDigest digest = MessageDigest.getInstance(getHashAlgorithm());
      digest.update(concat.getBytes("UTF-8"));
      final String result = Base64.encodeBase64String(digest.digest()).trim();
//
//      System.out.println("Browser Key: '" + nonceBase64 + "'");
//      System.out.println("Concat     : '" + concat + "'");
//      System.out.println("Result     : '" + result + "'");

      return result;
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("error generating hash", e);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("could not get UTF-8 bytes");
    }
  }
}
