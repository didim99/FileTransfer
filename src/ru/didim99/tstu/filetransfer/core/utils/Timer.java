package ru.didim99.tstu.filetransfer.core.utils;

import java.util.Locale;

/**
 * simple timer class
 * Created by didim99 on 28.01.18.
 */
public class Timer {
  private static final long TICKS_PER_MS = 1000000;
  private static final long TICKS_PER_US = 1000;
  private boolean running;
  private long timer;

  public void start() {
    timer = System.nanoTime();
    running = true;
  }

  public void stop() {
    timer = System.nanoTime() - timer;
    running = false;
  }

  public long getNano() {
    return running ? System.nanoTime() - timer : timer;
  }

  public long getMicro() {
    return getNano() / TICKS_PER_US;
  }

  public long getMillis() {
    return getNano() / TICKS_PER_MS;
  }

  public String getStr() {
    return longToTime(getNano());
  }

  private String longToTime(long time) {
    time /= TICKS_PER_MS;
    if (time < 60000)
      return String.format(Locale.US, "%.3f сек.", time / 1000f);
    else {
      String out;
      long millis = time % 1000;
      time /= 1000;
      long sec = time % 60;
      time /= 60;
      long min = time % 60;
      time /= 60;
      if (time == 0)
        out = String.format(Locale.US, "%d:%02d.%03d", min, sec, millis);
      else
        out = String.format(Locale.US, "%d:%02d:%02d.%03d", time, min, sec, millis);
      return out;
    }
  }
}
