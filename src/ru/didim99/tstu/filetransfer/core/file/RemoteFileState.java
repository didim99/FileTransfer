package ru.didim99.tstu.filetransfer.core.file;

import com.google.gson.annotations.SerializedName;

public class RemoteFileState {
  @SerializedName("fileName")
  private String fileName;

  RemoteFileState(FileState fileState) {
    this.fileName = fileState.getDisplayName();
  }

  public String getFileName() {
    return fileName;
  }
}
