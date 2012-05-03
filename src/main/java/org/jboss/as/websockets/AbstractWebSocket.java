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
import org.jboss.as.websockets.util.Assert;
import org.jboss.as.websockets.util.Hash;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Mike Brock
 */
public abstract class AbstractWebSocket implements WebSocket {
  protected final String webSocketId;
  protected final HttpServletRequest servletRequest;
  protected final InputStream inputStream;
  protected final OutputStream outputStream;
  protected final ClosingStrategy closingStrategy;

  protected AbstractWebSocket(
          final HttpServletRequest servletRequest,
          final InputStream inputStream,
          final OutputStream outputStream,
          final ClosingStrategy closingStrategy) {

    this.webSocketId = Hash.newUniqueHash();
    this.servletRequest = Assert.notNull(servletRequest, "servletRequest must NOT be null");
    this.inputStream = Assert.notNull(inputStream, "inputStream must NOT be null");
    this.outputStream = Assert.notNull(outputStream, "outputStream must NOT be null");
    this.closingStrategy = Assert.notNull(closingStrategy, "closingStrategy must NOT be null");
  }

  public final String getSocketID() {
    return webSocketId;
  }

  public HttpSession getHttpSession() {
    return servletRequest.getSession();
  }

  public HttpServletRequest getServletRequest() {

    return servletRequest;
  }

  public void closeSocket() throws IOException {
    closingStrategy.doClose();
  }
}
