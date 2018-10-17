package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.annotations.SerializedName;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import ru.didim99.tstu.filetransfer.core.file.RemoteFileState;

public class PeerInfo {
  @SerializedName("peerID")
  private final long peerID;
  @SerializedName("peerName")
  private String peerName;
  @SerializedName("peerAddress")
  private InetAddress peerAddress;

  private ArrayList<RemoteFileState> fileList;

  public PeerInfo(boolean local) {
    peerID = new Random().nextLong();
    peerName = "Peer " + Math.abs(peerID % 1000);
  }

  void setPeerAddress(InetAddress peerAddress) {
    this.peerAddress = peerAddress;
  }

  void setFileList(ArrayList<RemoteFileState> fileList) {
    this.fileList = fileList;
  }

  InetAddress getPeerAddress() {
    return peerAddress;
  }

  String getPeerName() {
    return peerName;
  }

  ArrayList<RemoteFileState> getFileList() {
    return fileList;
  }

  public String getDisplayName() {
    return peerAddress.getHostAddress() + " [" + peerName + "]";
  }

  @Override
  public String toString() {
    return peerAddress + " [" + peerName + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (!(obj instanceof PeerInfo))
      return false;
    PeerInfo other = (PeerInfo) obj;
    return other.peerID == peerID;
  }
}
