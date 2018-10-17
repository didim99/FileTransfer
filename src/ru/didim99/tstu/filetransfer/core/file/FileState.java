package ru.didim99.tstu.filetransfer.core.file;

import java.io.File;

public class FileState {
  private File file;

  FileState(File file) {
    this.file = file;
  }

  public String getDisplayName() {
    return file.getName();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (!(obj instanceof FileState))
      return false;
    FileState other = (FileState) obj;
    return other.file.equals(file);
  }
}
