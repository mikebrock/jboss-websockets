/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.websockets;

import org.jboss.as.websockets.protocol.ClosingStrategy;
import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

  public abstract WebSocket getWebSocket(HttpServletRequest request,
                                         HttpServletResponse response,
                                         ClosingStrategy closingStrategy) throws IOException;

  public abstract boolean matches(HttpServletRequest request);

  public abstract byte[] generateResponse(HttpEvent event) throws IOException;
}
