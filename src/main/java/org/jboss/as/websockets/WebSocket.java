package org.jboss.as.websockets;

import java.io.IOException;

/**
 * @author Mike Brock
 */
public interface WebSocket {
  /**
   * Write an text frame to the websocket. All String data will be UTF-8 encoded on the wire.
   *
   * @param text the UTF-8 text string
   * @throws IOException
   */
  public void writeTextFrame(String text) throws IOException;
}
