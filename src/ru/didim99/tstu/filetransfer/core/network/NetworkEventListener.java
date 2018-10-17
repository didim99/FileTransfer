package ru.didim99.tstu.filetransfer.core.network;

import java.net.Socket;

interface NetworkEventListener {
  void onNewPeerDetected(PeerInfo info);
  void onNewPeerConnected(PeerInfo info, Socket clientSocket);
  void onPeerListReceived(PeerInfo[] peers);
  void onRemoteFileListUpdated(Peer peer);
  void onPeerDisconnected(Peer peer);
}
