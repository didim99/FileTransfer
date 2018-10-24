package ru.didim99.tstu.filetransfer.core.file;

import com.google.gson.annotations.SerializedName;

public class RemoteFileState {
  @SerializedName("fileName")
  private String fileName;
  @SerializedName("id")
  private String id;

  RemoteFileState(FileState fileState) {
    this.fileName = fileState.getDisplayName();
    this.id = fileState.getId();
  }

  public String getFileName() {
    return fileName;
  }

  public String getId() {
    return id;
  }
}
