package org.jboss.as.websockets.servlet;

import org.jboss.as.websockets.Handshake;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.buffer.BufferColor;
import org.jboss.as.websockets.protocol.ietf00.Ietf00Handshake;
import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;
import org.jboss.as.websockets.protocol.ietf08.Ietf08Handshake;
import org.jboss.as.websockets.protocol.ietf13.Ietf13Handshake;
import org.jboss.servlet.http.HttpEvent;
import org.jboss.servlet.http.HttpEventServlet;
import org.jboss.servlet.http.UpgradableHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A very, very early and experimental spike to get websockets working in JBoss AS. Designed for JBoss AS 7.1.2 and
 * later.
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

  private static final String SESSION_READ_BUFFER_KEY = "JBoss:Experimental:Websocket:ReadBuffer";
  private static final String SESSION_WRITE_STREAM_KEY = "JBoss:Experimental:Websocket:WriteStream";
  private static final String SESSION_WEBSOCKET_HANDLE = "JBoss:Experimental:Websocket:Handle";


  public final void event(final HttpEvent event) throws IOException, ServletException {
    final HttpServletRequest request = event.getHttpServletRequest();
    final HttpServletResponse response = event.getHttpServletResponse();
    final HttpSession session = request.getSession();

    switch (event.getType()) {
      case BEGIN:
        event.setTimeout(20000);

        if (response instanceof UpgradableHttpServletResponse) {
          for (Handshake handshake : websocketHandshakes) {
            if (handshake.matches(request)) {
              handshake.generateResponse(event);

              response.setHeader("Upgrade", "websocket");
              response.setHeader("Connection", "Upgrade");

              ((UpgradableHttpServletResponse) response).sendUpgrade();

              createSessionBufferEntry(event);

              final WebSocket webSocket = new WebSocket() {
                final OutputStream stream = event.getHttpServletResponse().getOutputStream();

                public void writeTextFrame(String text) throws IOException {
                  writeWebSocketFrame(stream, text);
                }
              };

              session.setAttribute(SESSION_WEBSOCKET_HANDLE, webSocket);
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
          onReceivedTextFrame(event, (WebSocket) session.getAttribute(SESSION_WEBSOCKET_HANDLE),
                  readFrame(event, event.getHttpServletRequest().getInputStream()));
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

  private static final byte FRAME_FIN = Byte.MIN_VALUE;
  private static final byte FRAME_OPCODE = 0x0F;
  private static final byte FRAME_MASKED = Byte.MIN_VALUE;
  private static final byte FRAME_LENGTH = 127;

  private static final int OPCODE_CONTINUATION = 0;
  private static final int OPCODE_TEXT = 1;
  private static final int OPCODE_BINARY = 2;
  private static final int OPCODE_CONNECTION_CLOSE = 3;
  private static final int OPCODE_PING = 4;
  private static final int OPCODE_PONG = 5;

  private static String readFrame(HttpEvent event, InputStream stream) throws IOException {
    final StringBuilder payloadBuffer = new StringBuilder();
    int b = stream.read();

    int opcode = (b & FRAME_OPCODE);

    b = stream.read();

    boolean frameMasked = (b & FRAME_MASKED) != 0;

    int payloadLength = (b & FRAME_LENGTH);
    if (payloadLength == 126) {
      payloadLength = ((stream.read() & 0xFF) << 8) +
              (stream.read() & 0xFF);
    }

    final int[] frameMaskingKey = new int[4];

    if (frameMasked) {
      frameMaskingKey[0] = stream.read();
      frameMaskingKey[1] = stream.read();
      frameMaskingKey[2] = stream.read();
      frameMaskingKey[3] = stream.read();
    }
//
//    System.out.println("WS_FRAME(opcode=" + opcode + ";frameMasked=" + frameMasked + ";payloadLength="
//            + payloadLength + ";frameMask=" + Arrays.toString(frameMaskingKey) + ")");

    switch (opcode) {
      case OPCODE_TEXT:
        int read = 0;
        if (frameMasked) {
          do {
            int r = stream.read();
            payloadBuffer.append(((char) ((r ^ frameMaskingKey[read % 4]) & 127)));
          }
          while (++read < payloadLength);
        }
        else {
          // support unmasked frames for testing.

          do {
            payloadBuffer.append((char) stream.read());
          }
          while (++read < payloadLength);
        }
        break;
      case OPCODE_CONNECTION_CLOSE:
        event.close();
        break;

      case OPCODE_PING:
      case OPCODE_PONG:
        break;

      case OPCODE_BINARY:
        // binary transmission not supported
        break;

    }

    return payloadBuffer.toString();
  }

  private final static String secureRandomAlgorithm = "SHA1PRNG";
  final static SecureRandom random;

  static {
    try {
      random = SecureRandom.getInstance(secureRandomAlgorithm);
      random.setSeed(SecureRandom.getInstance(secureRandomAlgorithm).generateSeed(64));
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("runtime does not support secure random algorithm: " + secureRandomAlgorithm);
    }
  }

  // we'll just statically cache the byte mask on startup. there's no real security in this.
  private static final byte[] mask
          = {(byte) random.nextInt(127), (byte) random.nextInt(127),
          (byte) random.nextInt(127), (byte) random.nextInt(127)};

  private static void writeWebSocketFrame(final OutputStream stream, final String txt) throws IOException {
    byte[] strBytes = txt.getBytes("UTF-8");
    boolean big = strBytes.length > 125;

    stream.write(-127);
    if (big) {
      stream.write(-2);
      stream.write(((strBytes.length >> 8) & 0xFF));
      stream.write(((strBytes.length) & 0xFF));
    }
    else {
      stream.write(-128 | (strBytes.length & 127));
    }

    stream.write(mask[0]);
    stream.write(mask[1]);
    stream.write(mask[2]);
    stream.write(mask[3]);


    int len = strBytes.length;
    for (int j = 0; j < len; j++) {
      stream.write((strBytes[j] ^ mask[j % 4]));
    }

    stream.flush();
  }


  private static void createSessionBufferEntry(HttpEvent event) throws IOException {
    final HttpSession session = event.getHttpServletRequest().getSession();

    session.setAttribute(SESSION_READ_BUFFER_KEY, BufferColor.getNewColor());
    session.setAttribute(SESSION_WRITE_STREAM_KEY, event.getHttpServletResponse().getOutputStream());
  }

  private static BufferColor getSessionBufferEntry(HttpEvent event) {
    return getSessionBufferEntry(event.getHttpServletRequest().getSession());
  }

  private static BufferColor getSessionBufferEntry(HttpSession session) {
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

  /**
   * Called when a new websocket is opened.
   *
   * @param event The HttpEvent associated with the WebSocket Upgrade.
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
   * @param event The HttpEvent associated with <em>original</em> WebSocket Upgrade.
   * @param socket A reference to the WebSocket writer interface associated with this socket.
   * @param text the String data from the received websocket payload.
   * @throws IOException
   */
  protected abstract void onReceivedTextFrame(final HttpEvent event, final WebSocket socket, final String text) throws IOException;

}
