package pools;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

class MyJob implements Callable<String> {
  private static int nextID = 0;
  private int myID = nextID++;

  @Override
  public String call() throws Exception {
    System.out.println(Thread.currentThread().getName()
        + " starting job " + myID);
    if (Thread.interrupted()) {
      // arrange for shutdown...
    }
    try {
      Thread.sleep(1000 + (int)(Math.random() * 2000));
    } catch(InterruptedException ie) {
      // arrange shutdown
      System.out.println("Shutdown of job " + myID + " by request");
      return null;
    }
    System.out.println(Thread.currentThread().getName()
        + " ending job " + myID);
    if (Math.random() > 0.7) {
      System.out.println("Job " + myID + " throwing exception");
      throw new SQLException("DB broke");
    }
    return "Job ID " + myID;
  }
}

public class UseExecutorService {
  public static void main(String[] args) throws Throwable {
    ExecutorService es = Executors.newFixedThreadPool(2);
    List<Future<String>> lfs = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Future<String> handle = es.submit(new MyJob());
      lfs.add(handle);
      if (i == 1 || i == 5) {
        Thread.sleep(10);
        System.out.println("canceling job " + i);
        handle.cancel(true);
      }
    }
//    es.shutdown();
//    es.submit(new MyJob());
    System.out.println("Jobs submitted...");

    while (lfs.size() > 0) {
      Iterator<Future<String>> ifs = lfs.iterator();
      while (ifs.hasNext()) {
        Future<String> fs = ifs.next();
        if (fs.isDone()) {
          try {
            String result = fs.get();
            System.out.println("Job returned: " + result);
          } catch (InterruptedException e) {
            System.out.println("Ouch, interrupted, that shouldn't happen!");
          } catch (ExecutionException e) {
            System.out.println("Job threw an exception: " + e.getCause());
          } catch (CancellationException ce) {
            System.out.println("Job threw cancellation exception "
                + ce.getMessage());
          }
          ifs.remove();
        }
      }
    }
  }
}
