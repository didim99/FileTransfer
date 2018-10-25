package ru.didim99.tstu.filetransfer.core.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import ru.didim99.tstu.filetransfer.core.network.PeerInfo;
import ru.didim99.tstu.filetransfer.core.network.TransferController;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

public class FileManager implements TransferController.EventListener {
  private static final String LOG_TAG = "FileManager";

  private LinkedList<FileEventListener> listeners;
  private LinkedList<TransferController> tcl;
  private ArrayList<FileState> files;

  public FileManager(FileEventListener listener) {
    tcl = new LinkedList<>();
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

  public void registerTC(TransferController tc) {
    tc.registerEventListener(this);
    tcl.add(tc);
  }

  public void unregisterTC(TransferController tc) {
    tc.unregisterEventListener();
    tcl.remove(tc);
  }

  public void addFile(File file) {
    if (file.canRead())
      addFileInternal(new FileState(file));
    else
      Logger.write(LOG_TAG, "File not readable: " + file);
  }

  private void addFileInternal(FileState newState) {
    File file = newState.getFile();
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

  public void prepareTransfer(PeerInfo info, File file)
    throws IOException {
    for (TransferController tc : tcl) {
      if (tc.getInfo().equals(info)) {
        tc.waitForFile(file);
      }
    }
  }

  public void onFileRequested(RemoteFileState state, PeerInfo info) {
    for (FileState file : files) {
      if (file.equals(state)) {
        if (!file.addRequest(info.getPeerID()))
          return;
        if (!file.isBusy()) {
          for (TransferController tc : tcl) {
            if (tc.getInfo().equals(info)) {
              tc.startUploading(file);
            }
          }
        }
      }
    }
  }

  @Override
  public void onTransferFinished(FileState file, boolean success) {
    if (success)
      addFileInternal(file);
    Long nextPeer = file.peekReuquest();
    if (nextPeer != null) {
      for (TransferController tc : tcl) {
        if (tc.getInfo().getPeerID() == nextPeer) {
          tc.startUploading(file);
        }
      }
    }
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
    private String fileName;

    public Event(Type type, int peerIndex, int fileIndex) {
      this(type, peerIndex, fileIndex, null);
    }

    public Event(Type type, int peerIndex, int fileIndex, String fileName) {
      this.type = type;
      this.peerIndex = peerIndex;
      this.fileIndex = fileIndex;
      this.fileName = fileName;
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

    public String getFileName() {
      return fileName;
    }

    public boolean isLocal() {
      return peerIndex == LOCAL_PEER;
    }
  }
}
