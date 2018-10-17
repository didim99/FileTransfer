package ru.didim99.tstu.filetransfer.core.file;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class FileListInfo {
  public static class Action {
    public static final int CREATE  = 1;
    public static final int ADD     = 2;
    public static final int REMOVE  = 3;
    public static final int CLEAR   = 4;
  }

  @SerializedName("action")
  private int action;
  @SerializedName("files")
  private RemoteFileState[] files;
  @SerializedName("newFile")
  private RemoteFileState newFile;
  @SerializedName("removeIndex")
  private int removeIndex;

  private FileListInfo() {}

  public int getAction() { return action; }
  public RemoteFileState[] getFiles() { return files; }
  public RemoteFileState getNewFile() { return newFile; }
  public int getRemoveIndex() { return removeIndex; }

  public static class Builder {
    private FileListInfo info;

    public Builder() {
      info = new FileListInfo();
    }

    public Builder action(int action) {
      info.action = action;
      return this;
    }

    public Builder files(ArrayList<FileState> files) {
      info.files = new RemoteFileState[files.size()];
      for (int i = 0; i < files.size(); i++)
        info.files[i] = new RemoteFileState(files.get(i));
      return this;
    }

    public Builder newName(FileState newFile) {
      info.newFile = new RemoteFileState(newFile);
      return this;
    }

    public Builder removeIndex(int removeIndex) {
      info.removeIndex = removeIndex;
      return this;
    }

    public FileListInfo build() {
      return info;
    }
  }
}
