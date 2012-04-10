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

package org.jboss.as.websockets.servlet;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.WebSocketHeaders;
import org.jboss.as.websockets.protocol.ietf00.Hybi00Handshake;
import org.jboss.as.websockets.protocol.ietf07.Hybi07Handshake;
import org.jboss.as.websockets.protocol.ietf08.Hybi08Handshake;
import org.jboss.as.websockets.protocol.ietf13.Hybi13Handshake;
import org.jboss.servlet.http.HttpEvent;
import org.jboss.servlet.http.HttpEventServlet;
import org.jboss.servlet.http.UpgradableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A very, very early and experimental spike to get websockets working in JBoss AS. Designed for JBoss AS 7.1.2 and
 * later.
 *
 * @author Mike Brock
 */
public abstract class WebSocketServlet extends HttpServlet implements HttpEventServlet {
  private static final List<Handshake> websocketHandshakes;

  private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

  private String protocolName;

  static {
    final List<Handshake> handshakeList = new ArrayList<Handshake>();
    handshakeList.add(new Hybi13Handshake());
    handshakeList.add(new Hybi07Handshake());
    handshakeList.add(new Hybi08Handshake());
    handshakeList.add(new Hybi00Handshake());

    websocketHandshakes = Collections.unmodifiableList(handshakeList);
  }

  /**
   * An attribute name to stuff WebSocket handles into the sessions with.
   */
  private static final String SESSION_WEBSOCKET_HANDLE = "JBoss:Experimental:Websocket:Handle";

  private void setStandardUpgradeHeaders(final HttpServletResponse response) {
    response.setHeader("Upgrade", "WebSocket");
    response.setHeader("Connection", "Upgrade");

    if (protocolName != null)
      WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL.set(response, protocolName);
    else {
      WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL.set(response, "*");
    }
  }

  public final void event(final HttpEvent event) throws IOException, ServletException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();

    switch (event.getType()) {
      case BEGIN:
        event.setTimeout(20000);

        log.debug("Begin Websocket Handshake");

        if (response instanceof UpgradableHttpServletResponse) {
          for (Handshake handshake : websocketHandshakes) {
            if (handshake.matches(request)) {
              setStandardUpgradeHeaders(response);

              log.debug("Found a compatible handshake: (Version:"
                      + handshake.getVersion() + "; Handler: " + handshake.getClass().getName() + ")");

              /**
               * Generate the server handshake response -- setting the necessary headers and also capturing
               * any data bound for the body of the response.
               */
              final byte[] handShakeData = handshake.generateResponse(event);

              /**
               * Obtain an WebSocket instance from the handshaker.
               */
              final WebSocket webSocket = handshake.getWebSocket(event);

              log.debug("Using WebSocket implementation: " + webSocket.getClass().getName());

              request.setAttribute(SESSION_WEBSOCKET_HANDLE, webSocket);

              /**
               * Transition the request from HTTP to a persistent socket.
               */
              ((UpgradableHttpServletResponse) response).sendUpgrade(handShakeData);

              log.debug("Websocket opened for session: " + request.getSession().getId());

              onSocketOpened(event, webSocket);
            }
          }
        }
        else {
          throw new IllegalStateException("cannot upgrade connection");
        }
        break;
      case END:
        break;
      case ERROR:
        event.close();
        break;
      case EVENT:
      case READ:
        while (event.isReadReady()) {
          onReceivedTextFrame(event, (WebSocket) request.getAttribute(SESSION_WEBSOCKET_HANDLE));
        }
        break;

      case TIMEOUT:
        event.resume();
        break;

      case EOF:
        onSocketClosed(event);
        break;

    }
  }

  //
  // Override all the normal HTTP methods and make them final so they can't be inherited by users of this servlet.
  //
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doGet(req, resp);
  }

  @Override
  protected final long getLastModified(HttpServletRequest req) {
    return super.getLastModified(req);
  }

  @Override
  protected final void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doHead(req, resp);
  }

  @Override
  protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doPost(req, resp);
  }

  @Override
  protected final void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doPut(req, resp);
  }

  @Override
  protected final void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doDelete(req, resp);
  }

  @Override
  protected final void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doOptions(req, resp);
  }

  @Override
  protected final void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doTrace(req, resp);
  }

  @Override
  protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.service(req, resp);
  }

  @Override
  public final void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    super.service(req, res);
  }
  //
  //  Finish overriding methods
  //

  /**
   * Set the protocol name to be returned in the Sec-WebSocket-Protocol header attribute during negotiation. This is
   * not thread-safe. It should only be set from the init() method of the servlet.
   *
   * @param protocol
   */
  protected void setProtocolName(final String protocol) {
    this.protocolName = protocol;
  }

  /**
   * Called when a new websocket is opened.
   *
   * @param event  The HttpEvent associated with the WebSocket Upgrade.
   * @param socket A reference to the WebSocket writer interface
   * @throws IOException
   */
  protected abstract void onSocketOpened(final HttpEvent event, final WebSocket socket) throws IOException;

  /**
   * Called when the websocket is closed.
   *
   * @param event The HttpEvent associated with the socket closure.
   * @throws IOException
   */
  protected abstract void onSocketClosed(final HttpEvent event) throws IOException;

  /**
   * Called when a new text frame is received.
   *
   * @param event  The HttpEvent associated with <em>original</em> WebSocket Upgrade.
   * @param socket A reference to the WebSocket writer interface associated with this socket.
   * @throws IOException
   */
  protected abstract void onReceivedTextFrame(final HttpEvent event, final WebSocket socket) throws IOException;

}
