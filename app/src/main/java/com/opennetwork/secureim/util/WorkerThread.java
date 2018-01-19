package com.opennetwork.secureim.util;

import java.util.List;

public class WorkerThread extends Thread {

  private final List<Runnable> workQueue;

  public WorkerThread(List<Runnable> workQueue, String name) {
    super(name);
    this.workQueue = workQueue;
  }

  private Runnable getWork() {
    synchronized (workQueue) {
      try {
        while (workQueue.isEmpty())
          workQueue.wait();

        return workQueue.remove(0);
      } catch (InterruptedException ie) {
        throw new AssertionError(ie);
      }
    }
  }

  @Override
  public void run() {
    for (;;)
      getWork().run();
  }
}
