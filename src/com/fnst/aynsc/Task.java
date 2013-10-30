package com.fnst.aynsc;

public interface Task {//这个接口也比较简单，可以执行，可以取到执行结果
    void execute();
    byte[] getResult();
}
