package org.jboss.as.websockets;

import org.jboss.as.websockets.util.Hash;

/**
 * @author Mike Brock
 */
public abstract class AbstractWebSocket implements WebSocket {
  protected final String webSocketId;

  protected AbstractWebSocket() {
    webSocketId = Hash.newUniqueHash();
  }

  public final String getSocketID() {
    return webSocketId;
  }
}
