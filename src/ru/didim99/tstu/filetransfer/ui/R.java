package ru.didim99.tstu.filetransfer.ui;

public final class R {
  public static final String CHOOSE_FILE = "Choose file";
  public static final String CHOOSE_SAVE_PATH = "Save path";
  public static final String ALREADY_EXISTS = "Already exists";
  public static final String OVERWRITE = "File %s already exists. Overwrite it?";
  public static final String SAVE = "Save";
  static final String WINDOW_TITLE = "File transfer";
  static final String STATUS = "Status: %s";
  static final String CONNECT = "Connect";
  static final String DISCONNECT = "Disconnect";
  static final String DOWNLOAD = "Download";
  static final String REMOVE = "Remove";

  static final int DEFAULT_WIDTH = 400;
  static final int DEFAULT_HEIGHT = 400;

  public static final class Status {
    public static final String NOT_CONNECTED = "Not connected";
    public static final String SEARCHING_PEERS = "Searching peers";
    public static final String CONNECTING = "Connecting";
    public static final String CONNECTED = "Connected: %d";
  }
}
