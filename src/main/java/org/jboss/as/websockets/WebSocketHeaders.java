/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  SEC_WEBSOCKET_LOCATION("Sec-WebSocket-Location"),
  ORIGIN("Origin"),
  SEC_WEBSOCKET_ORIGIN("Sec-WebSocket-Origin"),
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
