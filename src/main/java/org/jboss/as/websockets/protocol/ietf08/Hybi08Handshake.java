package org.jboss.as.websockets.protocol.ietf08;

import org.jboss.as.websockets.protocol.ietf07.Hybi07Handshake;

/**
 * The handshaking protocol impelemtation for Hybi-07, which is identical to Hybi-08, and thus is just a thin
 * subclass of {@link Hybi07Handshake} that sets a different version number.
 * @author Mike Brock
 */
public class Hybi08Handshake extends Hybi07Handshake {
  public Hybi08Handshake() {
    super("8");
  }
}
