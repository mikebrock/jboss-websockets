package org.jboss.as.websockets.protocol.ietf13;

import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.jboss.as.websockets.WebSocketHeaders.ORIGIN;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_LOCATION;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL;

/**
 * @author Mike Brock
 */
public class Ietf13Handshake extends Ietf07Handshake {
  public Ietf13Handshake() {
    super("13");
  }

  @Override
  public byte[] generateResponse(HttpEvent event) throws IOException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();

    ORIGIN.copy(request, response);

    SEC_WEBSOCKET_PROTOCOL.copy(request, response);

    SEC_WEBSOCKET_LOCATION.set(response, getWebSocketLocation(request));

    final String key = SEC_WEBSOCKET_KEY.get(request);
    final String solution = solve(key);

    WebSocketHeaders.SEC_WEBSOCKET_ACCEPT.set(response, solution);

    return new byte[0];
  }
}
