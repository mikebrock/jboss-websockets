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

package org.jboss.as.websockets.protocol.ietf00;

import org.jboss.as.websockets.AbstractWebSocket;
import org.jboss.as.websockets.WebSocket;
import org.jboss.servlet.http.HttpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The Hybi-00 Framing Protocol implementation.
 *
 * @see Hybi00Handshake
 * @author Mike Brock
 */
public class Hybi00Socket extends AbstractWebSocket {
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final int MAX_FRAME_SIZE = 1024 * 32; //32kb


  private static final Logger log = LoggerFactory.getLogger(Hybi00Socket.class);

  private Hybi00Socket(final InputStream inputStream, final OutputStream outputStream) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
  }

  public static WebSocket from(final HttpEvent event) throws IOException {
    return new Hybi00Socket(
            event.getHttpServletRequest().getInputStream(),
            event.getHttpServletResponse().getOutputStream());
  }

  public void writeTextFrame(final String text) throws IOException {
    outputStream.write(0x00);
    outputStream.write(text.getBytes("UTF-8"));
    outputStream.write((byte) 0xFF);
    outputStream.flush();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public String readTextFrame() throws IOException {
    byte frametype = (byte) inputStream.read();
    boolean error = false;

    if ((frametype & 0x80) == 0x80) {
      throw new RuntimeException("binary payload not supported");
//      int length = 0;
//      do {
//        int b = inputStream.read();
//        int b_v = b & 0x7F;
//        length = (length * 128) + b_v;
//
//        if ((b & 0x80) == 0x80) {
//          continue;
//        }
//
//        for (int i = 0; i < length; i++) {
//          inputStream.read();
//        }
//
//      }
//      while (false);
//      if (frametype == 0xFF && length == 0) {
//        error = true;
//      }
    }
    else if (frametype == 0) {
      final StringBuilder buf = new StringBuilder();
      int b;
      int read = 0;

      while ((b = inputStream.read()) != 0xFF) {
        if (++read > MAX_FRAME_SIZE) {
          throw new RuntimeException("frame too large");
        }
        buf.append((char) b);
      }

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
