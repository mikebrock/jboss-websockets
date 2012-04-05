package org.jboss.as.websockets.protocol.ietf13;

import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;

/**
 * @author Mike Brock
 */
public class Ietf13Handshake extends Ietf07Handshake {
  public Ietf13Handshake() {
    super("13");
  }
}
