/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * 线程锁测试
 */
public class LockTest {

  @Test
  public static void main(String[] args) {
    Object co = new Object();
    System.out.println(co);

    for (int i = 0; i < 5; i++) {
      MyThread t = new MyThread("Thread" + i, co);
      t.start();
    }

    try {
      TimeUnit.SECONDS.sleep(2);
      System.out.println("-----Main Thread notify-----");
      synchronized (co) {
        co.notify();
      }

      TimeUnit.SECONDS.sleep(2);
      System.out.println("Main Thread is end.");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static class MyThread extends Thread {
    private String name;
    private Object co;

    public MyThread(String name, Object o) {
      this.name = name;
      this.co = o;
    }

    @Override
    public void run() {
      System.out.println(name + " is waiting.");
      try {
        synchronized (co) {
          co.wait();
        }
        System.out.println(name + " has been notified.");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}