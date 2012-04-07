package org.jboss.as.websockets.protocol.ietf00;

import org.jboss.as.websockets.WebSocket;
import org.jboss.servlet.http.HttpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Mike Brock
 */
public class Hybi00Socket implements WebSocket {
  private final HttpEvent event;
  private final InputStream inputStream;
  private final OutputStream outputStream;

  private static final Logger log = LoggerFactory.getLogger(Hybi00Socket.class);

  private Hybi00Socket(final HttpEvent event, final InputStream inputStream, final OutputStream outputStream) {
    this.event = event;
    this.inputStream = inputStream;
    this.outputStream = outputStream;
  }

  public static WebSocket from(final HttpEvent event) throws IOException {
    return new Hybi00Socket(
            event,
            event.getHttpServletRequest().getInputStream(),
            event.getHttpServletResponse().getOutputStream());
  }

  public void writeTextFrame(final String text) throws IOException {
    final byte[] bytes = text.getBytes("UTF-8");

    outputStream.write(0x00);
    for (byte aByte : bytes) {
      outputStream.write(aByte);
    }
    outputStream.write((byte) 0xFF);

    System.out.println("WriteTextFrame:" + text);
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  public String readTextFrame() throws IOException {
    byte frametype = (byte) inputStream.read();
    boolean error = false;

    System.out.println("FrameType=" + frametype);

    if ((frametype & 0x80) == 0x80) {
      int length = 0;
      do {
        int b = inputStream.read();
        int b_v = b & 0x7F;
        length = (length * 128) + b_v;

        if ((b & 0x80) == 0x80) {
          continue;
        }

        for (int i = 0; i < length; i++) {
          inputStream.read();
        }

      }
      while (false);
      if (frametype == 0xFF && length == 0) {
        error = true;
      }
    }
    else if ((frametype & 0x80) == 0x00) {
      System.out.println("TextFrame");
      final StringBuilder buf = new StringBuilder();

      int b;

      while ((b = inputStream.read()) != 0xFF) {
        buf.append((char) b);
      }

      System.out.println("BufferData:" + buf.toString());

      if (frametype == 0) {
        return buf.toString();
      }
      else {
        error = true;
      }
    }

    if (error) {
      throw new RuntimeException("bad websockets payload");
    }

    return "";
  }
}
