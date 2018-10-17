package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import ru.didim99.tstu.filetransfer.core.file.FileEventListener;
import ru.didim99.tstu.filetransfer.core.file.FileListInfo;
import ru.didim99.tstu.filetransfer.core.file.FileManager;
import ru.didim99.tstu.filetransfer.core.file.RemoteFileState;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

class Peer extends Thread implements FileEventListener {
  private static final String LOG_TAG = "Peer";

  private static final class MSGType {
    private static final int UNTYPED    = -1;
    private static final int DISCONNECT = 1;
    private static final int PEER_LIST  = 2;
    private static final int FILE_LIST  = 3;
  }

  private NetworkEventListener listener;
  private FileManager fileManager;
  private PeerInfo info;
  private Socket socket;
  private BufferedReader in;
  private BufferedWriter out;
  private Gson gson;

  Peer(PeerInfo info, NetworkEventListener listener) {
    this(info, listener, null, null);
  }

  Peer(PeerInfo info, NetworkEventListener listener, FileManager fileManager) {
    this(info, listener, fileManager, null);
  }

  Peer(PeerInfo info, NetworkEventListener listener,
       FileManager fileManager, Socket clientSocket) {
    this.socket = clientSocket;
    this.fileManager = fileManager;
    this.listener = listener;
    this.info = info;
    this.gson = new Gson();

    if (fileManager != null)
      fileManager.registerEventListener(this);
  }

  @Override
  public void run() {
    String msg = null;
    try {
      while (true) {
        try {
          msg = in.readLine();
          Logger.write(LOG_TAG, "Received message: " + msg);

          if (msg == null)
            throw new SocketException("Received: null");
          int delimiter = msg.indexOf(' ');
          if (delimiter == -1)
            throw new IllegalMessageException();
          int msgType = Integer.parseInt(msg.substring(0, delimiter));
          msg = msg.substring(delimiter);
          //Logger.write(LOG_TAG, "Body: " + msg);

          switch (msgType) {
            case MSGType.DISCONNECT:
              throw new SocketException("Connection closed by remote host");
            case MSGType.PEER_LIST:
              PeerInfo[] peers = gson.fromJson(msg, PeerInfo[].class);
              if (peers == null)
                throw new IllegalMessageException();

              Logger.write(LOG_TAG, "Received peer list from: " + info);
              listener.onPeerListReceived(peers);
              break;
            case MSGType.FILE_LIST:
              FileListInfo fileListInfo = gson.fromJson(msg, FileListInfo.class);
              if (fileListInfo == null)
                throw new IllegalMessageException();
              onFileEventReceived(fileListInfo);
              break;
          }
        } catch (IllegalMessageException e) {
          Logger.write(LOG_TAG, "Corrupted message: " + msg);
        } catch (SocketException e) {
          onDisconnectedRemote();
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  PeerInfo getInfo() {
    return info;
  }

  void connect(PeerInfo myPeerInfo) throws IOException {
    socket = new Socket(info.getPeerAddress(), NetworkManager.TRANSFER_PORT);
    openSocket();
    send(gson.toJson(myPeerInfo, PeerInfo.class), MSGType.UNTYPED);
  }

  void openSocket() throws IOException {
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    Logger.write(LOG_TAG, "Connecting to remote peer: " + info);
    start();
  }

  void sendFileList() {
    FileListInfo fileListInfo = new FileListInfo.Builder()
      .action(FileListInfo.Action.CREATE)
      .files(fileManager.getFileList())
      .build();
    String msg = gson.toJson(fileListInfo, FileListInfo.class);
    send(msg, MSGType.FILE_LIST);
  }

  void sendPeerList(LinkedList<Peer> peers) {
    ArrayList<PeerInfo> info = new ArrayList<>();
    for (Peer peer : peers)
      info.add(peer.getInfo());
    String msg = gson.toJson(info.toArray(new PeerInfo[0]), PeerInfo[].class);
    send(msg, MSGType.PEER_LIST);
  }

  @Override
  public void onFileEvent(FileListInfo info) {
    String msg = gson.toJson(info, FileListInfo.class);
    send(msg, MSGType.FILE_LIST);
  }

  private void onFileEventReceived(FileListInfo fileListInfo) {
    Logger.write(LOG_TAG, "Received file list event from: " + info);
    boolean needUpdate = false;

    switch (fileListInfo.getAction()) {
      case FileListInfo.Action.CREATE:
        if (info.getFileList() == null) {
          ArrayList<RemoteFileState> remoteFiles = new ArrayList<>();
          Collections.addAll(remoteFiles, fileListInfo.getFiles());
          info.setFileList(remoteFiles);
          sendFileList();
        }
        break;
      case FileListInfo.Action.ADD:
        info.getFileList().add(fileListInfo.getNewFile());
        needUpdate = true;
        break;
      case FileListInfo.Action.REMOVE:
        info.getFileList().remove(fileListInfo.getRemoveIndex());
        needUpdate = true;
        break;
      case FileListInfo.Action.CLEAR:
        info.getFileList().clear();
        needUpdate = true;
        break;
    }

    if (needUpdate)
      listener.onRemoteFileListUpdated(this);
  }

  private void send(String str, int type) {
    try {
      if (type == MSGType.UNTYPED)
        str = str.concat("\n");
      else
        str = String.format("%d %s\n", type, str);
      Logger.write(LOG_TAG, "Sending to "
        + info.toString() + ": " + str.trim());
      out.write(str);
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void disconnect() throws IOException {
    if (socket != null && !socket.isClosed()) {
      send("Good bye!", MSGType.DISCONNECT);
      socket.close();
    }
  }

  private void onDisconnectedRemote() throws IOException {
    Logger.write(LOG_TAG, "Disconnected: " + info);
    fileManager.unregisterEventListener(this);
    listener.onPeerDisconnected(this);
    if (!socket.isClosed())
      socket.close();
  }

  private static class IllegalMessageException extends IllegalArgumentException {}
}
