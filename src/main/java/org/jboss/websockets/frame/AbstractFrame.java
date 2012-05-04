package org.jboss.websockets.frame;

import org.jboss.websockets.Frame;
import org.jboss.websockets.FrameType;

/**
 * @author Mike Brock
 */
public class AbstractFrame implements Frame {
  private final FrameType type;

  protected AbstractFrame(final FrameType type) {
    this.type = type;
  }

  public FrameType getType() {
    return type;
  }
}
