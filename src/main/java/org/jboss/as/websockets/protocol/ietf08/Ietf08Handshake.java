package org.jboss.as.websockets.protocol.ietf08;

import org.jboss.as.websockets.protocol.ietf07.Ietf07Handshake;

/**
 * @author Mike Brock
 */
public class Ietf08Handshake extends Ietf07Handshake {
  public Ietf08Handshake() {
    super("8");
  }
}
