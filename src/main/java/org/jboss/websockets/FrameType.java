package org.jboss.websockets;

/**
 * @author Mike Brock
 */
public enum FrameType {
  Continuation,
  Text,
  Binary,
  Ping,
  Pong,
  ConnectionClose,
  Unknown
}
