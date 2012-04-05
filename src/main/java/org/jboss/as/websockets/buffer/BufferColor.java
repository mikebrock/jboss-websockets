package org.jboss.as.websockets.buffer;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Mike Brock
 */
public class BufferColor {
  // an automatic counter to ensure each buffer has a unique color
  private static final AtomicInteger bufferColorCounter = new AtomicInteger();

  private static final BufferColor allBuffersColor = new BufferColor(Short.MIN_VALUE);

  /**
   * The current tail position for this buffer color.
   */
  final AtomicLong sequence = new AtomicLong();

  /**
   * The color.
   */
  final short color;

  /**
   * Lock for reads and writes on this buffer color.
   */
  final ReentrantLock lock = new ReentrantLock(false);

  /**
   * Condition used for notifying waiting read locks when new data is available.
   */
  final Condition dataWaiting = lock.newCondition();

  public short getColor() {
    return color;
  }

  public AtomicLong getSequence() {
    return sequence;
  }

  public void wake() {
    dataWaiting.signalAll();
  }

  public ReentrantLock getLock() {
    return lock;
  }

  private BufferColor(int color) {
    this.color = (short) color;
  }

  private BufferColor(short color) {
    this.color = color;
  }

  public static BufferColor getNewColor() {
    return new BufferColor(bufferColorCounter.incrementAndGet());
  }

  public static BufferColor getNewColorFromHead(TransmissionBuffer buffer) {
    final BufferColor color = getNewColor();
    color.sequence.set(buffer.getHeadSequence());
    return color;
  }

  public static BufferColor getAllBuffersColor() {
    return allBuffersColor;
  }
}
