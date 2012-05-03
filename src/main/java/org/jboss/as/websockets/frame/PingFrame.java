package org.jboss.as.websockets.frame;

import org.jboss.as.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class PingFrame extends AbstractFrame {
  public PingFrame() {
    super(FrameType.Ping);
  }
}
