package ru.didim99.tstu.filetransfer.core.file;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

public class FileManager {
  private static final String LOG_TAG = "FileManager";

  private LinkedList<FileEventListener> listeners;
  private ArrayList<FileState> files;

  public FileManager(FileEventListener listener) {
    listeners = new LinkedList<>();
    registerEventListener(listener);
    files = new ArrayList<>();
  }

  public void registerEventListener(FileEventListener listener) {
    listeners.add(listener);
  }

  public void unregisterEventListener(FileEventListener listener) {
    listeners.remove(listener);
  }

  public void addFile(File file) {
    if (!file.canRead()) {
      Logger.write(LOG_TAG, "File not readable: " + file);
      return;
    }

    FileState newState = new FileState(file);
    for (FileState state : files) {
      if (newState.equals(state)) {
        Logger.write(LOG_TAG, "File already added: " + file);
        return;
      }
    }

    Logger.write(LOG_TAG, "Adding: " + file);
    files.add(newState);
    notifyAll(new FileListInfo.Builder()
      .action(FileListInfo.Action.ADD)
      .newName(newState)
      .build());
  }

  public void clear() {
    Logger.write(LOG_TAG, "Deleting all files");
    files.clear();
    notifyAll(new FileListInfo.Builder()
      .action(FileListInfo.Action.CLEAR)
      .build());
  }

  public ArrayList<FileState> getFileList() {
    return files;
  }

  private void notifyAll(FileListInfo info) {
    for (FileEventListener listener : listeners)
      listener.onFileEvent(info);
  }

  public void dispatchEvent(Event event) {
    switch (event.getType()) {
      case REMOVE:
        FileState file = files.get(event.getFileIndex());
        Logger.write(LOG_TAG, "Deleting local file: " + file.getDisplayName());
        files.remove(event.getFileIndex());
        notifyAll(new FileListInfo.Builder()
          .action(FileListInfo.Action.REMOVE)
          .removeIndex(event.getFileIndex())
          .build());
        break;
    }
  }

  public static class Event {
    public static final int LOCAL_PEER = -1;
    public enum Type { REMOVE, DOWNLOAD }

    private Type type;
    private int peerIndex;
    private int fileIndex;

    public Event(Type type, int peerIndex, int fileIndex) {
      this.type = type;
      this.peerIndex = peerIndex;
      this.fileIndex = fileIndex;
    }

    public Type getType() {
      return type;
    }

    public int getPeerIndex() {
      return peerIndex;
    }

    public int getFileIndex() {
      return fileIndex;
    }

    public boolean isLocal() {
      return peerIndex == LOCAL_PEER;
    }
  }
}
