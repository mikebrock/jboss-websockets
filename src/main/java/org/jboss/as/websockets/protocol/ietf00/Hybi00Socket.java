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


  public void writeTextFrame(String text) throws IOException {

  }

  public String readTextFrame() throws IOException {
    return null;
  }
}
