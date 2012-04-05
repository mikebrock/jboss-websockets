package org.jboss.as.websockets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Mike Brock
 */
public enum WebSocketHeaders {
  SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),
  SEC_WEBSOCKET_KEY1("Sec-WebSocket-Key1"),
  SEC_WEBSOCKET_KEY2("Sec-WebSocket-Key2"),
  SEC_WEBSOCKET_ORIGIN("Sec-WebSocket-Location"),
  SEC_WEBSOCKET_LOCATION("Sec-WebSocket-Origin"),
  SEC_WEBSOCKET_PROTOCOL("Sec-WebSocket-Protocol"),
  SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),
  SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept");

  private final String canonicalHeaderName;

  private WebSocketHeaders(String canonicalHeaderName) {
    this.canonicalHeaderName = canonicalHeaderName;
  }

  public final String getCanonicalHeaderName() {
    return canonicalHeaderName;
  }

  public final String get(final HttpServletRequest request) {
    return request.getHeader(getCanonicalHeaderName());
  }

  public final String get(final HttpServletResponse response) {
    return response.getHeader(getCanonicalHeaderName());
  }

  public final boolean isIn(final HttpServletRequest request) {
    return get(request) != null;
  }

  public final boolean isIn(final HttpServletResponse response) {
    return get(response) != null;
  }

  public final void copy(final HttpServletRequest from, final HttpServletResponse response) {
    if (isIn(from)) {
      set(response, get(from));
    }
  }

  public final void set(final HttpServletResponse response, final String value) {
    response.setHeader(getCanonicalHeaderName(), value);
  }

  public final boolean matches(final HttpServletRequest request, final String matchTo) {
    final String val = get(request);
    return val != null && val.equals(matchTo);
  }

  public final boolean matches(final HttpServletResponse request, final String matchTo) {
    final String val = get(request);
    return val != null && val.equals(matchTo);
  }
}
