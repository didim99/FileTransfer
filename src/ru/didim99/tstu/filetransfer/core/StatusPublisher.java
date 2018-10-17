package ru.didim99.tstu.filetransfer.core;

import java.util.List;
import ru.didim99.tstu.filetransfer.core.file.RemoteFileState;
import ru.didim99.tstu.filetransfer.core.network.PeerInfo;

public interface StatusPublisher {
  void publishStatus(String status);
  void updateConnectionStatus(boolean connected);
  void updatePeerList(List<PeerInfo> peersInfo);
  void updateRemoteFileList(List<RemoteFileState> peersInfo);
}
