package org.jboss.as.websocket;

import org.jboss.websockets.oio.internal.util.Hash;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Brock
 */
public class ContentionTest {
  private AtomicInteger counter = new AtomicInteger();

  class HashRunner implements Runnable {
    public void run() {
      Hash.getRandomBytes(new byte[512]);
      counter.incrementAndGet();
    }
  }

  @Test
  public void testContention() throws Exception {
    final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(5);

    for (int i = 0; i < 4; i++) {
      scheduledExecutorService.scheduleAtFixedRate(new HashRunner(), 0, 50, TimeUnit.MICROSECONDS) ;
    }

    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      public void run() {
        System.out.println("hashes: " + counter.get());
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);

    scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS);
  }
}
