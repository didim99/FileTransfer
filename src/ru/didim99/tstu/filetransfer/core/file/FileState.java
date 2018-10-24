package ru.didim99.tstu.filetransfer.core.file;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import ru.didim99.tstu.filetransfer.core.utils.Utils;

public class FileState {
  private final String id;
  private final File file;
  private Queue<Long> requests;
  private boolean busy;

  public FileState(File file) {
    this.requests = new LinkedList<>();
    this.id = Utils.md5(file.getAbsolutePath());
    this.file = file;
    this.busy = false;
  }

  boolean addRequest(long remoteID) {
    if (!requests.contains(remoteID)) {
      requests.add(remoteID);
      return true;
    } else
      return false;
  }

  public void nextRequest() {
    requests.poll();
  }

  Long peekReuquest() {
    return requests.peek();
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj instanceof FileState) {
      FileState other = (FileState) obj;
      return other.file.equals(file);
    } else if (obj instanceof RemoteFileState) {
      RemoteFileState remote = (RemoteFileState) obj;
      return id.equals(remote.getId());
    } else
      return false;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return file.getName();
  }

  public boolean isBusy() {
    return busy;
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
  }
}
