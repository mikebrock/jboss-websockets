package org.jboss.as.websockets.servlet;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.protocol.ietf00.Ietf00Handshake;
import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;
import org.jboss.as.websockets.protocol.ietf08.Ietf08Handshake;
import org.jboss.as.websockets.protocol.ietf13.Ietf13Handshake;
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

  static {
    final List<Handshake> handshakeList = new ArrayList<Handshake>();
    handshakeList.add(new Ietf00Handshake());
    handshakeList.add(new Ietf07Handshake());
    handshakeList.add(new Ietf08Handshake());
    handshakeList.add(new Ietf13Handshake());

    websocketHandshakes = Collections.unmodifiableList(handshakeList);
  }

  private static final String SESSION_WEBSOCKET_HANDLE = "JBoss:Experimental:Websocket:Handle";

  private static void setStandardUpgradeHeaders(final HttpServletResponse response) {
    response.setHeader("Upgrade", "WebSocket");
    response.setHeader("Connection", "Upgrade");

  }

  public final void event(final HttpEvent event) throws IOException, ServletException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();
    final HttpSession session = request.getSession();

    switch (event.getType()) {
      case BEGIN:
        event.setTimeout(20000);

        log.info("Begin Handshake");

        if (response instanceof UpgradableHttpServletResponse) {
          for (Handshake handshake : websocketHandshakes) {
            if (handshake.matches(request)) {
              log.info("Found a compatibile handshake: (Version:"
                      + handshake.getVersion() + "; Handler: " + handshake.getClass().getName() + ")");
              // do the handshake.
              handshake.generateResponse(event);

              setStandardUpgradeHeaders(response);

              // Not sure what this actually does, but Remy told me to call it -- Mike
              // TODO: look at what it actually does.
              ((UpgradableHttpServletResponse) response).sendUpgrade();

              final WebSocket webSocket = handshake.getWebSocket(event);
              log.info("Using WebSocked implementation: " + webSocket.getClass().getName());

              session.setAttribute(SESSION_WEBSOCKET_HANDLE, webSocket);

              log.info("WebSocket is open.");
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
          log.info("Text Frame Ready to Read");
          onReceivedTextFrame(event, (WebSocket) session.getAttribute(SESSION_WEBSOCKET_HANDLE));
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
