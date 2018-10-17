package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

public class BroadcastSender extends Thread {
  private static final String LOG_TAG = "BroadcastSender";

  private static final long SEND_TIMEOUT = 1000;
  private static final long MAX_SEND_CNT = 10;

  private DatagramSocket socket;
  private InetAddress address;
  private byte[] data;
  private boolean running;
  private boolean paused;
  private int attempts;

  BroadcastSender(Peer myPeer) throws SocketException {
    String message = new Gson().toJson(myPeer.getInfo(), PeerInfo.class);
    address = getBroadCastAddress();
    socket = new DatagramSocket();
    socket.setBroadcast(true);
    data = message.getBytes();
    attempts = 0;
  }

  @Override
  public void start() {
    running = true;
    paused = false;
    super.start();
  }

  @Override
  public void run() {
    Logger.write(LOG_TAG, "Started");
    while (running) {
      try {
        if (!paused) {
          DatagramPacket packet = new DatagramPacket(
            data, data.length, address, NetworkManager.BROADCAST_PORT);
          socket.send(packet);
          if (++attempts > MAX_SEND_CNT)
            finish();
        }

        sleep(SEND_TIMEOUT);
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        running = false;
      }
    }
  }

  void finish() {
    if (!running) return;
    Logger.write(LOG_TAG, "Shutting down");
    running = false;
    socket.close();
  }

  void setPaused(boolean paused) {
    if (!running) return;
    Logger.write(LOG_TAG, paused ? "Paused" : "Unpaused");
    this.paused = paused;
  }

  private InetAddress getBroadCastAddress() throws SocketException {
    List<InetAddress> broadcastList = new ArrayList<>();
    Enumeration<NetworkInterface> interfaces
      = NetworkInterface.getNetworkInterfaces();

    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();

      if (networkInterface.isLoopback() || !networkInterface.isUp()) {
        continue;
      }

      networkInterface.getInterfaceAddresses().stream()
        .map(InterfaceAddress::getBroadcast)
        .filter(Objects::nonNull)
        .forEach(broadcastList::add);
    }

    return broadcastList.get(0);
  }
}
