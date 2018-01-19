package com.opennetwork.secureim.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * FutureTask with a reference identifier tag.
 */
public class TaggedFutureTask<V> extends FutureTask<V> {
  private final Object tag;
  public TaggedFutureTask(Runnable runnable, V result, Object tag) {
    super(runnable, result);
    this.tag = tag;
  }

  public TaggedFutureTask(Callable<V> callable, Object tag) {
    super(callable);
    this.tag = tag;
  }

  public Object getTag() {
    return tag;
  }
}
