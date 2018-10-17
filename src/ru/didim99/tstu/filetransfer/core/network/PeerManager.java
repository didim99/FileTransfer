package ru.didim99.tstu.filetransfer.core.network;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import ru.didim99.tstu.filetransfer.core.file.FileManager;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

class PeerManager {
  private static final String LOG_TAG = "PeerManager";

  private NetworkEventListener listener;
  private FileManager fileManager;
  private PeerListener peerListener;
  private LinkedList<Peer> peers;
  private int activePeerIndex;
  private Peer myPeer;

  PeerManager(NetworkEventListener listener, FileManager fileManager) {
    myPeer = new Peer(new PeerInfo(true), listener);
    this.fileManager = fileManager;
    peers = new LinkedList<>();
    this.listener = listener;
  }

  void setActivePeer(int peerIndex) {
    this.activePeerIndex = peerIndex;
  }

  void startListen() throws IOException {
    peerListener = new PeerListener(listener, this);
    peerListener.start();
  }

  void stopListen() throws IOException {
    peerListener.finish();
    for (Peer peer : peers)
      peer.disconnect();
    peers.clear();
  }

  void onNewPeerConnected(PeerInfo info, Socket clientSocket) throws IOException {
    for (Peer peer : peers) {
      if (peer.getInfo().equals(info)) {
        Logger.write(LOG_TAG, "Already connected: " + info.toString());
        throw new IllegalStateException("Already connected");
      }
    }

    Logger.write(LOG_TAG, "Binding: " + info.toString());
    Peer peer = new Peer(info, listener, fileManager, clientSocket);
    peer.openSocket();
    peer.sendFileList();
    peer.sendPeerList(peers);
    peers.add(peer);
  }

  void onPeerDisconnected(Peer peer) {
    peers.remove(peer);
  }

  void add(PeerInfo info) throws IOException {
    Peer peer = new Peer(info, listener, fileManager);
    peers.add(peer);
    peer.connect(myPeer.getInfo());
  }

  void onPeerListReceived(PeerInfo[] peersInfo) {
    for (PeerInfo info : peersInfo) {
      if (info.equals(myPeer.getInfo()))
        continue;
      if (isPeerConnected(info))
        continue;
      listener.onNewPeerDetected(info);
    }
  }

  Peer getMyPeer() {
    return myPeer;
  }

  int getPeerCount() {
    return peers.size();
  }

  ArrayList<PeerInfo> getPeersInfo() {
    ArrayList<PeerInfo> info = new ArrayList<>();
    for (Peer peer : peers)
      info.add(peer.getInfo());
    return info;
  }

  boolean isPeerConnected(PeerInfo info) {
    for (Peer peer : peers) {
      if (peer.getInfo().equals(info))
        return true;
    }

    return false;
  }

  boolean isPeerActive(Peer peer) {
    return peer == peers.get(activePeerIndex);
  }
}
