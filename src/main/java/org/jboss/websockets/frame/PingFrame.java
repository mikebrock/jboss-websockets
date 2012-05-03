package org.jboss.websockets.frame;

import org.jboss.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class PingFrame extends AbstractFrame {
  public PingFrame() {
    super(FrameType.Ping);
  }
}
