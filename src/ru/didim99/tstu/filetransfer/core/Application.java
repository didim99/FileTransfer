package ru.didim99.tstu.filetransfer.core;

import java.io.IOException;
import javax.swing.JFileChooser;
import ru.didim99.tstu.filetransfer.core.file.FileEventListener;
import ru.didim99.tstu.filetransfer.core.file.FileListInfo;
import ru.didim99.tstu.filetransfer.core.file.FileManager;
import ru.didim99.tstu.filetransfer.core.network.NetworkManager;
import ru.didim99.tstu.filetransfer.ui.MainWindow;
import ru.didim99.tstu.filetransfer.ui.R;

public class Application implements AppEventListener, FileEventListener {
  public enum Event { CONNECT, ADD_FILE, CLEAR_FILES }

  private MainWindow mainWindow;
  private StatusPublisher publisher;
  private NetworkManager networkManager;
  private FileManager fileManager;

  private Application() {
    mainWindow = new MainWindow(this);
    publisher = mainWindow;
    fileManager = new FileManager(this);
    networkManager = new NetworkManager(publisher, fileManager);
  }

  @Override
  public void onAppEvent(Event event) {
    switch (event) {
      case CONNECT:
        try {
          if (!networkManager.isConnected())
            networkManager.searchPeers();
          else
            networkManager.disconnectAll();
        } catch (IOException e) {
          publisher.publishStatus(e.getMessage());
        }
        break;
      case ADD_FILE:
        addNewFile();
        break;
      case CLEAR_FILES:
        fileManager.clear();
        break;
    }
  }

  @Override
  public void onFileEvent(FileManager.Event event) {
    if (event.isLocal())
      fileManager.dispatchEvent(event);
  }

  @Override
  public void onPeerSelected(int peerIndex) {
    networkManager.onPeerSelected(peerIndex);
  }

  @Override
  public void onFileEvent(FileListInfo info) {
    mainWindow.updateMyFileList(fileManager.getFileList());
  }

  private void addNewFile() {
    JFileChooser chooser = new JFileChooser();
    int ret = chooser.showDialog(null, R.CHOOSE_FILE);
    if (ret == JFileChooser.APPROVE_OPTION)
      fileManager.addFile(chooser.getSelectedFile());
  }

  public static void main(String[] args) {
    new Application();
  }
}
