package org.jboss.as.websocket;

import org.jboss.websockets.oio.internal.protocol.ietf00.Hybi00Handshake;
import org.junit.Test;

/**
 * @author Mike Brock
 */
public class HandshakeTests {
  @Test
  public void testHandshake() {
    System.out.println(new String(Hybi00Handshake.solve("MD5", Hybi00Handshake.decodeKey("254 23  8 87[ 65"),
            Hybi00Handshake.decodeKey("h 52 X 6U'7t?!j@24]6s52^"),
            "B..r..\\8".getBytes())));
  }

}
