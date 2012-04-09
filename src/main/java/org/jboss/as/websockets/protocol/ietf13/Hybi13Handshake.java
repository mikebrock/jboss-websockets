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

package org.jboss.as.websockets.protocol.ietf13;

import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.as.websockets.protocol.ietf07.Hybi07Handshake;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.jboss.as.websockets.WebSocketHeaders.ORIGIN;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_KEY;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_LOCATION;
import static org.jboss.as.websockets.WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL;

/**
 * The handshaking protocol implementation for Hybi-13.
 *
 * @author Mike Brock
 */
public class Hybi13Handshake extends Hybi07Handshake {
  public Hybi13Handshake() {
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
