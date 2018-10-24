package ru.didim99.tstu.filetransfer.core.utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Utils {
  private static final Random random = new Random();

  public static String md5(String str) {
    try {
      // Create MD5 Hash
      byte[] hash = MessageDigest.getInstance("MD5")
        .digest(str.getBytes());

      // Create Hex String
      StringBuilder hexStr = new StringBuilder();
      for (byte digit : hash)
        hexStr.append(String.format("%02x", digit & 0xFF));
      return hexStr.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return "";
    }
  }

  public static byte[] md5(File file) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      InputStream is = new FileInputStream(file);
      byte[] buffer = new byte[8192];
      int read;

      while ((read = is.read(buffer)) > 0)
        md.update(buffer, 0, read);
      return md.digest();
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static int randInRange(int minValue, int maxValue) {
    return maxValue - random.nextInt(maxValue - minValue + 1);
  }
}
