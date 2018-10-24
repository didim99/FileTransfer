package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.annotations.SerializedName;

class ErrorMsg {
  @SerializedName("errorCode")
  private int errorCode;

  ErrorMsg(int errorCode) {
    this.errorCode = errorCode;
  }

  int getErrorCode() {
    return errorCode;
  }
}
