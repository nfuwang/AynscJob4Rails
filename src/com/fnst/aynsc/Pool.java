package com.fnst.aynsc;


public interface Pool {
  Executor getExecutor();
  void destroy();
}
