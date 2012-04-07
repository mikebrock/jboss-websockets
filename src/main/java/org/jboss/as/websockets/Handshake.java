package org.jboss.as.websockets;

import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Mike Brock
 */
public abstract class Handshake {
  private final String version;
  private final String hashAlgorithm;
  private final String magicNumber;

  public Handshake(String version, String hashAlgorithm, String magicNumber) {
    this.version = version;
    this.hashAlgorithm = hashAlgorithm;
    this.magicNumber = magicNumber;
  }

  public String getVersion() {
    return this.version;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public String getMagicNumber() {
    return magicNumber;
  }

  protected String getWebSocketLocation(HttpServletRequest request) {
    return "ws://" + request.getHeader("Host") + request.getRequestURI();
  }

  public abstract WebSocket getWebSocket(HttpEvent event) throws IOException;

  public abstract boolean matches(HttpServletRequest request);

  public abstract byte[] generateResponse(HttpEvent event) throws IOException;
}
