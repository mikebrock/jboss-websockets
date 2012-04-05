package org.jboss.as.websockets.servlet;

import org.jboss.servlet.http.HttpEvent;

import javax.servlet.http.HttpSession;

/**
 * @author Mike Brock
 */
public interface WebsocketFrameListener {
  public void handleReceivedEvent(HttpEvent session, String text);
}
