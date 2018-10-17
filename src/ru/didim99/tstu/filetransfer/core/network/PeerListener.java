package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

class PeerListener extends Thread {
  private static final String LOG_TAG = "PeerListener";

  private NetworkEventListener listener;
  private ServerSocket server;
  private PeerInfo myPeerInfo;
  private Gson gsonParser;
  private boolean running;

  PeerListener(NetworkEventListener listener, PeerManager manager)
    throws IOException {
    server = new ServerSocket(NetworkManager.TRANSFER_PORT);
    this.myPeerInfo = manager.getMyPeer().getInfo();
    this.listener = listener;
    gsonParser = new Gson();
    this.running = false;
  }

  @Override
  public void start() {
    running = true;
    super.start();
  }

  @Override
  public void run() {
    Logger.write(LOG_TAG, "Started");
    try {
      while (running) {
        Socket clientSocket = server.accept();
        Logger.write(LOG_TAG, "Connection received: " + clientSocket.getInetAddress());
        PeerInfo info = gsonParser.fromJson(readData(clientSocket), PeerInfo.class);
        if (!running) continue;

        if (info == null || info.equals(myPeerInfo)) {
          clientSocket.close();
          continue;
        }

        info.setPeerAddress(clientSocket.getInetAddress());
        listener.onNewPeerConnected(info, clientSocket);
      }
    } catch (IOException e) {
      Logger.write(LOG_TAG, e.toString());
    }
  }

  private String readData(Socket client) throws IOException {
    byte[] buffer = new byte[1024];
    int count = client.getInputStream().read(buffer);
    return new String(buffer, 0, count);
  }

  void finish() throws IOException {
    if (!running) return;
    Logger.write(LOG_TAG, "Shutting down");
    running = false;
    server.close();
  }
}
