package org.jboss.as.websockets.servlet;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.buffer.BufferColor;
import org.jboss.as.websockets.buffer.TransmissionBuffer;
import org.jboss.as.websockets.protocol.ietf00.Ietf00Handshake;
import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;
import org.jboss.as.websockets.protocol.ietf08.Ietf08Handshake;
import org.jboss.as.websockets.protocol.ietf13.Ietf13Handshake;
import org.jboss.servlet.http.HttpEvent;
import org.jboss.servlet.http.HttpEventServlet;
import org.jboss.servlet.http.UpgradableHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A very, very early and experimental spike to get websockets working in JBoss AS.
 *
 * @author Mike Brock
 */
public class WebsocketServlet extends HttpServlet implements HttpEventServlet {
  private static final List<Handshake> websocketHandshakes;

  static {
    final List<Handshake> handshakeList = new ArrayList<Handshake>();
    handshakeList.add(new Ietf00Handshake());
    handshakeList.add(new Ietf07Handshake());
    handshakeList.add(new Ietf08Handshake());
    handshakeList.add(new Ietf13Handshake());

    websocketHandshakes = Collections.unmodifiableList(handshakeList);
  }

  private final List<WebsocketFrameListener> frameListeners = new ArrayList<WebsocketFrameListener>();
  private final TransmissionBuffer masterBuffer = TransmissionBuffer.createDirect();

  private static final String SESSION_READ_BUFFER_KEY = "JBoss:Experimental:WebsocketReadBuffer";

  public void event(final HttpEvent event) throws IOException, ServletException {
    switch (event.getType()) {
      case BEGIN:
        event.setTimeout(20000);
        final HttpServletRequest request = event.getHttpServletRequest();
        final HttpServletResponse response = event.getHttpServletResponse();
        if (response instanceof UpgradableHttpServletResponse) {
          for (Handshake handshake : websocketHandshakes) {
            if (handshake.matches(request)) {
              handshake.generateResponse(event);
              ((UpgradableHttpServletResponse) response).sendUpgrade();
              createSessionBufferEntry(event);
            }
          }
        }
        else {
          throw new IllegalStateException("cannot upgrade connection");
        }
        break;
      case END:
        notifyListeners(event);
        break;
      case ERROR:
        event.close();
        break;
      case EVENT:
        break;
      case READ:
        final ServletInputStream is = event.getHttpServletRequest().getInputStream();
        final BufferColor color = getSessionBufferEntry(event);
        if (color == null) break;

        masterBuffer.write(is, color);
        break;

      case TIMEOUT:

        event.resume();
        break;
      case WRITE:
        break;
    }
  }

  void registerListener(final WebsocketFrameListener listener) {
    frameListeners.add(listener);
  }

  private void notifyListeners(final HttpEvent event) throws IOException {
    final BufferColor color = getSessionBufferEntry(event);
    if (color != null) {
      for (WebsocketFrameListener listener : frameListeners) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        masterBuffer.read(outputStream, color);

        listener.handleReceivedEvent(event, new String(outputStream.toByteArray()));
      }
    }
  }

  private static void createSessionBufferEntry(HttpEvent event) {
    event.getHttpServletRequest().getSession()
            .setAttribute(SESSION_READ_BUFFER_KEY, BufferColor.getNewColor());
  }

  public static BufferColor getSessionBufferEntry(HttpEvent event) {
    return (BufferColor) event.getHttpServletRequest().getSession().getAttribute(SESSION_READ_BUFFER_KEY);
  }
}
