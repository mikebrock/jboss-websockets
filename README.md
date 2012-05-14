WebSockets for JBoss AS 7.1.2+
------------------------------

NOTE: This does not currently work on versions of AS older than 7.1.2. It also requires use of the Apache Portable
Runtime connector (APR). This limitation will be addressed in a future version of AS.

To Configure APR in JBoss AS 7.1.x:

1. In domain/configuration/domain.xml: Change Line:

         <subsystem xmlns="urn:jboss:domain:web:1.1" default-virtual-server="default-host" native="false">           

    to:

         <subsystem xmlns="urn:jboss:domain:web:1.1" default-virtual-server="default-host" native="true">

2. Okay, done. 



Using the WebsocketServlet:
---------------------------

Once AS 7.1.2+ is configured as specified above, using websockets is as simple as implementing a Servlet which
extends org.jboss.as.websockets.servlet.WebsocketServlet and implements its abstract methods. You map the servlet
as you normally would, and that servlet mapping will become the upgradable WebSocket path.

Example Implementation:

    @WebServlet("/websocket/")
    public class MyWebSocketServlet extends WebSocketServlet {

      @Override
      protected void onSocketOpened(WebSocket socket) throws IOException {
        System.out.println("Websocket opened :)");
      }

      @Override
      protected void onSocketClosed(WebSocket socket) throws IOException {
        System.out.println("Websocket closed :(");
      }

      @Override
      protected void onReceivedFrame(WebSocket socket) throws IOException {
        final Frame frame = socket.readFrame();
        if (frame instanceof TextFrame) {
          final String text = ((TextFrame) frame).getText();
          if ("Hello".equals(text)) {
            socket.writeFrame(TextFrame.from("Hey, there!"));
          }
        }
      }
    }


Errata:
-------

1. Message fragmentation is not yet supported.

Known Compatibility
----------------------
- Confirmed Working:
 - Chrome 18
 - Firefox 11
 - Safari 5.1
 - MobileSafari on iOS 5.1

Want to Contribute?
-------------------

Just fork. 
