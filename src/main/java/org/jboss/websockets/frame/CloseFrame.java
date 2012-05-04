package org.jboss.websockets.frame;

import org.jboss.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class CloseFrame extends AbstractFrame {
  public CloseFrame() {
    super(FrameType.ConnectionClose);
  }
}
