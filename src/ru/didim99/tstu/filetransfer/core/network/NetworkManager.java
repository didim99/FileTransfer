package ru.didim99.tstu.filetransfer.core.network;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import ru.didim99.tstu.filetransfer.core.StatusPublisher;
import ru.didim99.tstu.filetransfer.core.file.FileManager;
import ru.didim99.tstu.filetransfer.ui.R;

public class NetworkManager implements NetworkEventListener {
  private enum Status { NOT_CONNECTED, SEARCHING_PEERS, CONNECTING, CONNECTED }
  static final int BROADCAST_PORT = 9848;
  static final int TRANSFER_PORT = 9849;

  // Application level
  private StatusPublisher publisher;
  // Peers control
  private BroadcastSender broadcastSender;
  private PeerSearcher peerSearcher;
  private PeerManager peerManager;
  private Status status;

  public NetworkManager(StatusPublisher publisher, FileManager fileManager) {
    this.publisher = publisher;
    peerManager = new PeerManager(this, fileManager);
    updateStatus(Status.NOT_CONNECTED);
  }

  @Override
  public void onNewPeerDetected(PeerInfo info) {
    try {
      if (status == Status.SEARCHING_PEERS)
        broadcastSender.setPaused(true);
      updateStatus(Status.CONNECTING);
      peerManager.add(info);
      broadcastSender.finish();
    } catch (IOException e) {
      updateStatus(Status.CONNECTED);
      e.printStackTrace();
    }
  }

  @Override
  public void onNewPeerConnected(PeerInfo info, Socket clientSocket) {
    try {
      peerManager.onNewPeerConnected(info, clientSocket);
      updateStatus(Status.CONNECTED);
      broadcastSender.finish();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
      updateStatus(Status.CONNECTED);
    }
  }

  @Override
  public void onPeerListReceived(PeerInfo[] peers) {
    try {
      peerManager.onPeerListReceived(peers);
      updateStatus(Status.CONNECTED);
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onRemoteFileListUpdated(Peer peer) {
    if (peerManager.isPeerActive(peer))
      publisher.updateRemoteFileList(peer.getInfo().getFileList());
  }

  @Override
  public void onPeerDisconnected(Peer peer) {
    peerManager.onPeerDisconnected(peer);
    updateStatus(Status.CONNECTED);
  }

  public boolean isConnected() {
    return status != Status.NOT_CONNECTED;
  }

  public void onPeerSelected(int peerIndex) {
    peerManager.setActivePeer(peerIndex);
    publisher.updateRemoteFileList(
      peerManager.getPeersInfo().get(peerIndex).getFileList());
  }

  public void searchPeers() throws IOException {
    broadcastSender = new BroadcastSender(peerManager.getMyPeer());
    peerSearcher = new PeerSearcher(this, peerManager);
    publisher.updateConnectionStatus(true);
    updateStatus(Status.SEARCHING_PEERS);
    peerManager.startListen();
    broadcastSender.start();
    peerSearcher.start();
  }

  public void disconnectAll() throws IOException {
    broadcastSender.finish();
    peerSearcher.finish();
    peerManager.stopListen();
    publisher.updateConnectionStatus(false);
    updateStatus(Status.NOT_CONNECTED);
  }

  public void dispatchRemoteFileEvent(FileManager.Event event, File file)
    throws IOException {
    peerManager.dispatchRemoteFileEvent(event, file);
  }

  private void updateStatus(Status newStatus) {
    this.status = newStatus;
    String statusStr = null;

    if (status == Status.CONNECTED) {
      publisher.updatePeerList(peerManager.getPeersInfo());
      if (peerManager.getPeerCount() == 0) {
        status = Status.SEARCHING_PEERS;
        broadcastSender.setPaused(false);
      }
    }

    switch (status) {
      case NOT_CONNECTED:
        statusStr = R.Status.NOT_CONNECTED;
        break;
      case SEARCHING_PEERS:
        statusStr = R.Status.SEARCHING_PEERS;
        break;
      case CONNECTING:
        statusStr = R.Status.CONNECTING;
        break;
      case CONNECTED:
        statusStr = String.format(R.Status.CONNECTED, peerManager.getPeerCount());
        break;
    }

    publisher.publishStatus(statusStr);
  }
}
