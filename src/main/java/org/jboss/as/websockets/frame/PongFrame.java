package org.jboss.as.websockets.frame;

import org.jboss.as.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class PongFrame extends AbstractFrame {
  public PongFrame() {
    super(FrameType.Pong);
  }
}
