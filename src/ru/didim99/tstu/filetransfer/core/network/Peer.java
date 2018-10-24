package ru.didim99.tstu.filetransfer.core.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import ru.didim99.tstu.filetransfer.core.file.*;
import ru.didim99.tstu.filetransfer.core.utils.Logger;

class Peer extends Thread implements FileEventListener {
  private static final String LOG_TAG = "Peer";

  private static final class MSGType {
    private static final int UNTYPED    = -1;
    private static final int DISCONNECT = 1;
    private static final int PEER_LIST  = 2;
    private static final int FILE_LIST  = 3;
    private static final int TRANS_CFG  = 4;
    private static final int GET_FILE   = 5;
    private static final int ERROR      = 6;
  }

  private static final class Error {
    private static final int TRANSFER_NOT_STARTED = 1;
  }

  private NetworkEventListener listener;
  private FileManager fileManager;
  private TransferController tc;
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
            case MSGType.TRANS_CFG:
              try {
                int remotePort = Integer.parseInt(msg.substring(1));
                Logger.write(LOG_TAG, "Received transfer config: " + info);
                initTransfer(remotePort);
              } catch (NumberFormatException e) {
                throw new IllegalMessageException();
              } catch (IOException e) {
                sendError(Error.TRANSFER_NOT_STARTED);
              }
              break;
            case MSGType.GET_FILE:
              RemoteFileState state = gson.fromJson(msg, RemoteFileState.class);
              if (state == null)
                throw new IllegalMessageException();
              Logger.write(LOG_TAG, "Received file request: " + info);
              fileManager.onFileRequested(state, info);
              break;
            case MSGType.ERROR:
              ErrorMsg errorMsg = gson.fromJson(msg, ErrorMsg.class);
              if (errorMsg == null)
                throw new IllegalMessageException();
              onErrorReeived(errorMsg);
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

  void initTransfer() throws IOException {
    tc = new TransferController(this);
    fileManager.registerTC(tc);
    send(String.valueOf(tc.getLocalPort()), MSGType.TRANS_CFG);
  }

  private void initTransfer(int remotePort) throws IOException {
    tc = new TransferController(this, socket.getInetAddress(), remotePort);
    fileManager.registerTC(tc);
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

  void dispatchFileEvent(FileManager.Event event) {
    switch (event.getType()) {
      case DOWNLOAD:
        RemoteFileState state = info.getFileList().get(event.getFileIndex());
        String msg = gson.toJson(state, RemoteFileState.class);
        send(msg, MSGType.GET_FILE);
        break;
    }
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

  void onTransferFailed(FileState file) {
    
  }

  private void sendError(int errCode) {
    send(gson.toJson(new ErrorMsg(errCode), ErrorMsg.class), MSGType.ERROR);
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

  private void onErrorReeived(ErrorMsg msg) {
    Logger.write(LOG_TAG, "Received error message: " + info);
    switch (msg.getErrorCode()) {
      case Error.TRANSFER_NOT_STARTED:
        break;
    }
  }

  void disconnect() throws IOException {
    if (socket != null && !socket.isClosed()) {
      send("Good bye!", MSGType.DISCONNECT);
      fileManager.unregisterEventListener(this);
      fileManager.unregisterTC(tc);
      tc.disconnect();
      socket.close();
    }
  }

  void onDisconnectedRemote() {
    try {
      Logger.write(LOG_TAG, "Disconnected: " + info);
      tc.disconnect();
      fileManager.unregisterTC(tc);
      fileManager.unregisterEventListener(this);
      listener.onPeerDisconnected(this);
      if (!socket.isClosed())
        socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class IllegalMessageException extends IllegalArgumentException {}
}
