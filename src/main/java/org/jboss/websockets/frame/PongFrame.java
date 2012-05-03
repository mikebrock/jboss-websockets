package org.jboss.websockets.frame;

import org.jboss.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class PongFrame extends AbstractFrame {
  public PongFrame() {
    super(FrameType.Pong);
  }
}
