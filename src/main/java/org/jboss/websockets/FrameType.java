package org.jboss.websockets;

import java.io.InputStream;

/**
 * @author Mike Brock
 */
public enum FrameType {
  Continuation, Text, Binary, Ping, Pong, ConnectionClose, Unknown
}
