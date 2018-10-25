package ru.didim99.tstu.filetransfer.core.network;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import ru.didim99.tstu.filetransfer.core.file.FileState;
import ru.didim99.tstu.filetransfer.core.utils.Logger;
import ru.didim99.tstu.filetransfer.core.utils.Utils;

public class TransferController extends Thread {
  private static final String LOG_TAG = "TC";
  private static final long IDLE_SLEEP = 50;
  private static final int FRAME_SIZE = 1024 * 32;
  private static final int BUFF_SIZE = 512;
  private static final int MAX_RETRY_COUNT = 3;

  private enum State { IDLE, WAITING,
    PREPARE_DOWNLOADING, DOWNLOADING,
    PREPARE_UPLOADING, UPLOADING }

  private static final class Header {
    static final byte TRN_START   = 0x01;
    static final byte TRN_END     = 0x02;
    static final byte TRN_FAILED  = 0x10;
    static final byte CRC_FAILED  = 0x11;
    static final byte READY       = 0x20;
    static final byte CRC         = 0x21;
    static final byte CRC_OK      = 0x22;
  }

  private PeerInfo info;
  private State state;
  private ErrorListener errListener;
  private EventListener listener;
  private ServerSocket server;
  private int localPort;
  private boolean connected;
  private Socket socket;
  private BufferedInputStream in;
  private BufferedOutputStream out;
  private ByteBuffer inBuffer, outBuffer;
  private FileInputStream fileIn;
  private FileOutputStream fileOut;
  private FileState currentFile;
  private byte[] buffer, crc;
  private long dataSize;
  private int partCount;
  private int retryCount;

  private TransferController() {
    inBuffer = ByteBuffer.allocate(BUFF_SIZE);
    outBuffer = ByteBuffer.allocate(BUFF_SIZE);
    buffer = new byte[FRAME_SIZE];
    state = State.IDLE;
    retryCount = 0;
  }

  TransferController(Peer peer) throws IOException {
    this();
    this.info = peer.getInfo();
    this.errListener = peer;
    server = new ServerSocket(0);
    localPort = server.getLocalPort();
    start();
  }

  TransferController(Peer peer, InetAddress remoteAddr, int remotePort)
    throws IOException {
    this();
    this.info = peer.getInfo();
    this.errListener = peer;
    socket = new Socket(remoteAddr, remotePort);
    connectToRemote();
    start();
  }

  public void registerEventListener(EventListener listener) {
    this.listener = listener;
  }

  public void unregisterEventListener() {
    this.listener = null;
  }

  @Override
  public void run() {
    try {
      if (server != null) {
        Logger.write(LOG_TAG, "Waiting for connection: " + localPort);
        socket = server.accept();
        Logger.write(LOG_TAG, "Connection received: " + socket);
        connectToRemote();
        connected = true;
        server.close();
      }

      while (connected) {
        try {
          switch (state) {
            case IDLE:        sleep(IDLE_SLEEP);  continue;
            case WAITING:     onWaiting();        break;
            case UPLOADING:   onUploading();      break;
            case DOWNLOADING: onDownloading();    break;
          }
        } catch (TransferException e) {
          e.printStackTrace();
          switch (state) {
            case PREPARE_DOWNLOADING:
            case DOWNLOADING:
            case WAITING:
              fileOut.close();
              currentFile.getFile().delete();
              break;
            case PREPARE_UPLOADING:
            case UPLOADING:
              currentFile.setBusy(false);
              currentFile.nextRequest();
              break;
          }
          resetState();
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      errListener.onTransferError();
    }
  }

  private void connectToRemote() throws IOException {
    Logger.write(LOG_TAG, String.format("Connecting: %s:%d",
      socket.getInetAddress(), socket.getPort()));
    in = new BufferedInputStream(socket.getInputStream(), FRAME_SIZE);
    out = new BufferedOutputStream(socket.getOutputStream(), FRAME_SIZE);
    Logger.write(LOG_TAG, String.format("Connected to: %s:%d",
      socket.getInetAddress(), socket.getPort()));
    connected = true;
  }

  public void waitForFile(File file) throws IOException {
    state = State.PREPARE_DOWNLOADING;
    if (retryCount > MAX_RETRY_COUNT) {
      errListener.onTransferFailed(currentFile);
      resetState();
      return;
    }

    this.currentFile = new FileState(file);
    if (file.exists() || file.createNewFile()) {
      fileOut = new FileOutputStream(file);
      Logger.write(LOG_TAG, "Waiting for: " + file.getName());
      state = State.WAITING;
    } else
      errListener.onTransferFailed(currentFile);
  }

  public void startUploading(FileState file) {
    try {
      if (!connected)
        throw new IOException("Not connected");
      state = State.PREPARE_UPLOADING;
      this.currentFile = file;
      currentFile.setBusy(true);
      crc = Utils.md5(currentFile.getFile());
      onStartUploading();
    } catch (IOException e) {
      errListener.onTransferError();
    }
  }

  private void onStartUploading() throws IOException {
    File file = currentFile.getFile();
    dataSize = file.length();
    partCount = (int) Math.ceil((double) dataSize / FRAME_SIZE);
    fileIn = new FileInputStream(file);
    Logger.write(LOG_TAG, String.format("Uploading: %s [%d/%d]",
      currentFile.getDisplayName(), dataSize, partCount));
    outBuffer.put(Header.TRN_START)
      .putLong(dataSize).putInt(partCount);
    sendOutBuffer();
    if (checkHeader(Header.READY)) {
      Logger.write(LOG_TAG, "Uploading accepted");
      state = State.UPLOADING;
    } else {
      Logger.write(LOG_TAG, "Uploading not accepted");
      restartUploading();
    }
  }

  private void restartUploading() throws IOException {
    if (retryCount++ > MAX_RETRY_COUNT) {
      Logger.write(LOG_TAG, "Unable to transfer file");
      currentFile.nextRequest();
      resetState();
      errListener.onTransferFailed(currentFile);
      listener.onTransferFinished(currentFile, false);
    } else {
      state = State.PREPARE_UPLOADING;
      onStartUploading();
    }
  }

  private void onWaiting()
    throws IOException, TransferException {
    in.read(inBuffer.array());
    if (inBuffer.get() == Header.TRN_START) {
      dataSize = inBuffer.getLong();
      partCount = inBuffer.getInt();
      Logger.write(LOG_TAG, String.format(
        "Start condition received: %d/%d", dataSize, partCount));
      sendHeader(Header.READY);
      state = State.DOWNLOADING;
      inBuffer.clear();
    } else {
      throw new TransferException("Corrupted start condition");
    }
  }

  private void onUploading()
    throws IOException, TransferException {
    Logger.write(LOG_TAG, "Uploading started");
    while (dataSize > 0) {
      int read = fileIn.read(buffer);
      out.write(buffer, 0, read);
      dataSize -= read;
      out.flush();
      partCount--;
    }

    if (partCount == 0 && dataSize == 0) {
      outBuffer.put(Header.CRC)
        .putInt(crc.length).put(crc);
      sendOutBuffer();
      fileIn.close();
      if (checkHeader(Header.CRC_OK)) {
        Logger.write(LOG_TAG, "Uploading completed");
        sendHeader(Header.TRN_END);
        onTransferCompleted();
      } else {
        Logger.write(LOG_TAG, "Uploading failed");
        restartUploading();
      }
    } else
      throw new TransferException("Uploading failed");
  }

  private void onDownloading() throws IOException {
    Logger.write(LOG_TAG, "Downloading started");
    while (dataSize > 0) {
      int read = in.read(buffer, 0, Math.min(buffer.length, (int) dataSize));
      fileOut.write(buffer, 0, read);
      dataSize -= read;
      partCount--;
    }

    if (dataSize == 0 && partCount <= 0) {
      Logger.write(LOG_TAG, "Downloading finished. Checking CRC");
      fileOut.close();

      in.read(inBuffer.array());
      if (inBuffer.get() == Header.CRC) {
        int count = inBuffer.getInt();
        crc = new byte[count];
        inBuffer.get(crc, 0, count);

        if (Arrays.equals(crc, Utils.md5(currentFile.getFile()))) {
          Logger.write(LOG_TAG, "CRC status: OK");
          sendHeader(Header.CRC_OK);
          if (checkHeader(Header.TRN_END)) {
            Logger.write(LOG_TAG, "Downloading completed");
            onTransferCompleted();
          }
        } else {
          Logger.write(LOG_TAG, "CRC status: FAILED");
          sendHeader(Header.CRC_FAILED);
        }
      }
    } else {
      retryCount++;
      Logger.write(LOG_TAG, String.format(
        "Downloading failed: [%d/%d]", dataSize, partCount));
      sendHeader(Header.TRN_FAILED);
      waitForFile(currentFile.getFile());
    }
  }

  private void onTransferCompleted() throws IOException {
    currentFile.nextRequest();
    currentFile.setBusy(false);
    listener.onTransferFinished(currentFile, true);
    resetState();
  }

  private void resetState() throws IOException {
    if (fileIn != null)
      fileIn.close();
    if (fileOut != null)
      fileOut.close();
    outBuffer.clear();
    inBuffer.clear();
    crc = null;
    dataSize = 0;
    partCount = 0;
    retryCount = 0;
    state = State.IDLE;
  }

  public PeerInfo getInfo() {
    return info;
  }

  int getLocalPort() {
    return localPort;
  }

  private boolean checkHeader(byte expected) throws IOException {
    return (byte) in.read() == expected;
  }

  private void sendOutBuffer() throws IOException {
    out.write(outBuffer.array(), 0, outBuffer.position());
    outBuffer.clear();
    out.flush();
  }

  private void sendHeader(byte header) throws IOException {
    out.write(header);
    out.flush();
  }

  void disconnect() {
    connected = false;
    if (socket != null) {
      Logger.write(LOG_TAG, "Disconnected: " + socket.getInetAddress());
    }
  }

  public interface EventListener {
    void onTransferFinished(FileState file, boolean success);
  }

  interface ErrorListener {
    void onTransferError();
    void onTransferFailed(FileState file);
  }

  private class TransferException extends Exception {
    TransferException(String msg) { super(msg); }
  }
}
