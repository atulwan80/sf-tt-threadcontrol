package usereentrantlock;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyQueue<E> {
  private E[] data = (E[]) (new Object[10]);
  private int count = 0;

  private ReentrantLock lock = new ReentrantLock();
  private Condition notFull = lock.newCondition();
  private Condition notEmpty = lock.newCondition();

  public void put(E e) throws InterruptedException {

    lock.lock();
    try {
      while (count >= 10) {
        notFull.await();
      }
      data[count++] = e;
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
  }

  public E take() throws InterruptedException {
    lock.lock();
    try {
      while (count <= 0) {
        notEmpty.await();
      }
      E result = data[0];
      System.arraycopy(data, 1, data, 0, --count);
      notFull.signal();
      return result;
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    MyQueue<int[]> queue = new MyQueue<>();
    new Thread(() -> {
      System.out.println("Producer starting...");
      for (int i = 0; i < 1_000; i++) {
        try {
          int[] data = {-1, i};
          if (i < 100) {
            Thread.sleep(1);
          }
          data[0] = i;
          if (i == 500) {
            data[1] = -99;
          }
          queue.put(data); data = null;
        } catch (InterruptedException ie) {
          System.out.println("Ouch, that shouldn't happen!");
        }
      }
      System.out.println("Producer finishing...");
    }).start();

    new Thread(() -> {
      System.out.println("Consumer starting...");
      for (int i = 0; i < 1_000; i++) {
        try {
          int [] data = queue.take();
          if (data[0] != data[1] || data[0] != i) {
            System.out.println("***** ERROR at "
                + i + " values " + Arrays.toString(data));
          }
          if (i > 900) {
            Thread.sleep(1);
          }
        } catch (InterruptedException ie) {
          System.out.println("Ouch, that hurt the consumer...");
        }
      }
      System.out.println("Consumer finishing...");
    }).start();
    System.out.println("Workers started...");
  }
}
