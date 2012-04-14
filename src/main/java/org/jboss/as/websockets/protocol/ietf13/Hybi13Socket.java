package org.jboss.as.websockets.protocol.ietf13;

import org.jboss.as.websockets.AbstractWebSocket;
import org.jboss.as.websockets.WebSocket;
import org.jboss.as.websockets.protocol.ClosingStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Mike Brock
 */
public class Hybi13Socket extends AbstractWebSocket {

  public Hybi13Socket(final HttpServletRequest servletRequest,
                        final InputStream inputStream,
                        final OutputStream outputStream,
                        final ClosingStrategy closingStrategy) {
      super(servletRequest, inputStream, outputStream, closingStrategy);

    }

  public static WebSocket from(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final ClosingStrategy closingStrategy) throws IOException {

    return new Hybi13Socket(request, request.getInputStream(), response.getOutputStream(), closingStrategy);
  }

  private static final byte FRAME_FIN = Byte.MIN_VALUE;
  private static final byte FRAME_OPCODE = 127;
  private static final byte FRAME_MASKED = Byte.MIN_VALUE;
  private static final byte FRAME_LENGTH = 127;

  private static final int OPCODE_CONTINUATION = 0;
  private static final int OPCODE_TEXT = 1;
  private static final int OPCODE_BINARY = 2;
  private static final int OPCODE_CONNECTION_CLOSE = 3;
  private static final int OPCODE_PING = 4;
  private static final int OPCODE_PONG = 5;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private String _readTextFrame() throws IOException {
    //TODO: check the first bit?
    int b = inputStream.read();
    final int opcode = (b & FRAME_OPCODE);

    b = inputStream.read();
    final boolean frameMasked = (b & FRAME_MASKED) != 0;

    int payloadLength = (b & FRAME_LENGTH);
    if (payloadLength == 126) {
      payloadLength = ((inputStream.read() & 0xFF) << 8) +
              (inputStream.read() & 0xFF);
    }
    else if (payloadLength == 127) {
      // ignore the first 4-bytes. We can't deal with 64-bit ints right now anyways.
      inputStream.read();
      inputStream.read();
      inputStream.read();
      inputStream.read();
      payloadLength = ((inputStream.read() & 0xFF) << 24) +
              ((inputStream.read() & 0xFF) << 16) +
              ((inputStream.read() & 0xFF) << 8) +
              ((inputStream.read() & 0xFF));
    }

    final byte[] frameMaskingKey = new byte[4];

    if (frameMasked) {
      inputStream.read(frameMaskingKey);
    }

    final StringBuilder payloadBuffer = new StringBuilder(payloadLength);
    switch (opcode) {
      case OPCODE_TEXT:
        int read = 0;
        if (frameMasked) {
          do {
            payloadBuffer.append(((char) ((inputStream.read() ^ frameMaskingKey[read % 4]) & 127)));
          }
          while (++read < payloadLength);
        }
        else {
          // support unmasked frames for testing.

          do {
            payloadBuffer.append((char) inputStream.read());
          }
          while (++read < payloadLength);
        }
        break;
      case OPCODE_CONNECTION_CLOSE:
        closeSocket();
        break;

      case OPCODE_PING:
      case OPCODE_PONG:
        break;

      case OPCODE_BINARY:
        // binary transmission not yet supported
        throw new RuntimeException("binary frame not yet supported");
    }


    return payloadBuffer.toString();
  }

  private void _writeTextFrame(final String txt) throws IOException {

    byte[] strBytes = txt.getBytes("UTF-8");
    final int len = strBytes.length;

    outputStream.write(-127);
    if (strBytes.length > Short.MAX_VALUE) {
      outputStream.write(127);

      // pad the first 4 bytes of 64-bit context length. If this frame is larger than 2GB, you're in trouble. =)
      outputStream.write(0);
      outputStream.write(0);
      outputStream.write(0);
      outputStream.write(0);
      outputStream.write((len & 0xFF) << 24);
      outputStream.write((len & 0xFF) << 16);
      outputStream.write((len & 0xFF) << 8);
      outputStream.write((len & 0xFF));
    }
    else if (strBytes.length > 125) {
      outputStream.write(126);
      outputStream.write(((len >> 8) & 0xFF));
      outputStream.write(((len) & 0xFF));
    }
    else {
      outputStream.write((len & 127));
    }

    for (byte strByte : strBytes) {
      outputStream.write(strByte);
    }

    outputStream.flush();
  }

  public void writeTextFrame(String text) throws IOException {
    _writeTextFrame(text);
  }

  public String readTextFrame() throws IOException {
    return _readTextFrame();
  }
}