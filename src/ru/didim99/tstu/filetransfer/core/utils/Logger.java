package ru.didim99.tstu.filetransfer.core.utils;

public class Logger {

  public static synchronized void write(String tag, String msg) {
    System.out.println(String.format("%s: %s", tag, msg));
  }
}
