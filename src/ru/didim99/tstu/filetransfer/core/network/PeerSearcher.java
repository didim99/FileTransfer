package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

public class PeerSearcher extends Thread {
  private static final String LOG_TAG = "PeerSearcher";

  private static final int BUFFER_LENGTH = 4096;
  private NetworkEventListener listener;
  private PeerManager peerManager;
  private DatagramSocket server;
  private PeerInfo myPeerInfo;
  private boolean running;
  private byte[] buffer;

  PeerSearcher(NetworkEventListener listener,
               PeerManager peerManager)
    throws IOException {
    server = new DatagramSocket(NetworkManager.BROADCAST_PORT);
    buffer = new byte[BUFFER_LENGTH];
    this.myPeerInfo = peerManager.getMyPeer().getInfo();
    this.peerManager = peerManager;
    this.listener = listener;
  }

  @Override
  public void start() {
    running = true;
    super.start();
  }

  @Override
  public void run() {
    Logger.write(LOG_TAG, "Started");
    while (running) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        server.receive(packet);

        String data = new String(packet.getData(), 0, packet.getLength());
        PeerInfo info = new Gson().fromJson(data, PeerInfo.class);

        if (info != null && !info.equals(myPeerInfo)) {
          if (!peerManager.isPeerConnected(info)) {
            Logger.write(LOG_TAG, "Received: " + info.getPeerName());
            info.setPeerAddress(packet.getAddress());
            listener.onNewPeerDetected(info);
          } else {
            Logger.write(LOG_TAG, "Exists: " + info.getPeerName());
          }
        }
      } catch (IOException e) {
        Logger.write(LOG_TAG, e.toString());
      }
    }
  }

  void finish() {
    if (!running) return;
    Logger.write(LOG_TAG, "Shutting down");
    running = false;
    server.close();
  }
}
