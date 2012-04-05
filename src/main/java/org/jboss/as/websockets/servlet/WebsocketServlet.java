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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A very, very early and experimental spike to get websockets working in JBoss AS.
 *
 * @author Mike Brock
 */
public abstract class WebsocketServlet extends HttpServlet implements HttpEventServlet {
  private static final List<Handshake> websocketHandshakes;

  static {
    final List<Handshake> handshakeList = new ArrayList<Handshake>();
    handshakeList.add(new Ietf00Handshake());
    handshakeList.add(new Ietf07Handshake());
    handshakeList.add(new Ietf08Handshake());
    handshakeList.add(new Ietf13Handshake());

    websocketHandshakes = Collections.unmodifiableList(handshakeList);
  }

  private final TransmissionBuffer masterReadBuffer = TransmissionBuffer.createDirect();
  private static final String SESSION_READ_BUFFER_KEY = "JBoss:Experimental:WebsocketReadBuffer";
  private static final String SESSION_WRITE_STREAM_KEY = "JBoss:Experimental:WebsocketWriteStream";


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
              notifyConnectionBegin(event.getHttpServletRequest().getSession());
            }
          }
        }
        else {
          throw new IllegalStateException("cannot upgrade connection");
        }
        break;
      case END:
        notifyMessageReceived(event);
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

        masterReadBuffer.write(is, color);
        break;

      case TIMEOUT:

        event.resume();
        break;

    }
  }

  protected abstract void notifyConnectionBegin(final HttpSession session) throws IOException;

  protected abstract void handleReceivedEvent(HttpEvent event, String text) throws IOException;

  protected void writeToSocket(final HttpSession session, final String text) throws IOException {
    final OutputStream outputStream = (OutputStream) session.getAttribute(SESSION_WRITE_STREAM_KEY);
    if (outputStream != null) {
      outputStream.write(text.getBytes());
    }
  }

  private void notifyMessageReceived(final HttpEvent event) throws IOException {
    final BufferColor color = getSessionBufferEntry(event);
    if (color != null) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      masterReadBuffer.read(outputStream, color);

      handleReceivedEvent(event, new String(outputStream.toByteArray()));
    }
  }

  private static void createSessionBufferEntry(HttpEvent event) throws IOException {
    final HttpSession session = event.getHttpServletRequest().getSession();

    session.setAttribute(SESSION_READ_BUFFER_KEY, BufferColor.getNewColor());
    session.setAttribute(SESSION_WRITE_STREAM_KEY, event.getHttpServletResponse().getOutputStream());
  }

  public static BufferColor getSessionBufferEntry(HttpEvent event) {
    return getSessionBufferEntry(event.getHttpServletRequest().getSession());
  }

  public static BufferColor getSessionBufferEntry(HttpSession session) {
    return (BufferColor) session.getAttribute(SESSION_READ_BUFFER_KEY);
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
}
