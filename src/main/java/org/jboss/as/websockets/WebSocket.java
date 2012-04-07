package org.jboss.as.websockets;

import java.io.IOException;

/**
 * Represents a handle to a single WebSocket connection. It has reader and writer methods to get data in and out.
 *
 * TODO: Implement support for binary frames.
 *
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

  /**
   * Read a single text frame -- if available.
   *
   * @return the UTF-8 text payload string.
   * @throws IOException
   */
  public String readTextFrame() throws IOException;
}
