package ru.didim99.tstu.filetransfer.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import ru.didim99.tstu.filetransfer.core.AppEventListener;
import ru.didim99.tstu.filetransfer.core.Application;
import ru.didim99.tstu.filetransfer.core.StatusPublisher;
import ru.didim99.tstu.filetransfer.core.file.FileManager;
import ru.didim99.tstu.filetransfer.core.file.FileState;
import ru.didim99.tstu.filetransfer.core.file.RemoteFileState;
import ru.didim99.tstu.filetransfer.core.network.PeerInfo;

public class MainWindow extends JFrame implements StatusPublisher {
  //Application level
  private AppEventListener listener;
  //UI components
  private JPanel rootPanel;
  private JLabel statusLabel;
  private JButton btnConnect;
  private JButton btnAddFile;
  private JButton btnClear;
  private JList<PeerInfo> peerList;
  private JList<FileState> myFileList;
  private JList<RemoteFileState> remoteFileList;

  public MainWindow(AppEventListener listener) {
    this.listener = listener;
    setContentPane(rootPanel);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(R.DEFAULT_WIDTH, R.DEFAULT_HEIGHT);
    setTitle(R.WINDOW_TITLE);
    initComponents();
    setVisible(true);
  }

  private void initComponents() {
    JPopupMenu fileMenu = new JPopupMenu();
    JMenuItem itemRemove = new JMenuItem(R.REMOVE);
    itemRemove.addActionListener(e -> listener.onFileEvent(
      new FileManager.Event(FileManager.Event.Type.REMOVE,
        FileManager.Event.LOCAL_PEER, myFileList.getSelectedIndex())));
    fileMenu.add(itemRemove);

    JPopupMenu remoteFileMenu = new JPopupMenu();
    JMenuItem itemDownload = new JMenuItem(R.DOWNLOAD);
    itemDownload.addActionListener(e -> listener.onFileEvent(
      new FileManager.Event(FileManager.Event.Type.DOWNLOAD,
        peerList.getSelectedIndex(), remoteFileList.getSelectedIndex(),
        remoteFileList.getSelectedValue().getFileName())));
    remoteFileMenu.add(itemDownload);

    btnConnect.addActionListener(e ->
      listener.onAppEvent(Application.Event.CONNECT));
    btnAddFile.addActionListener(e ->
      listener.onAppEvent(Application.Event.ADD_FILE));
    btnClear.addActionListener(e ->
      listener.onAppEvent(Application.Event.CLEAR_FILES));
    myFileList.setLayoutOrientation(JList.VERTICAL);
    myFileList.setModel(new DefaultListModel<>());
    myFileList.setCellRenderer(new FileListRenderer());
    myFileList.addMouseListener(
      new PopupAdapter(myFileList, fileMenu));
    peerList.setLayoutOrientation(JList.VERTICAL);
    peerList.setModel(new DefaultListModel<>());
    peerList.setCellRenderer(new PeerListRenderer());
    peerList.addMouseListener(new ClickAdapter(peerList,
      index -> listener.onPeerSelected(index)));
    remoteFileList.setLayoutOrientation(JList.VERTICAL);
    remoteFileList.setModel(new DefaultListModel<>());
    remoteFileList.setCellRenderer(new RemoteFileListRenderer());
    remoteFileList.addMouseListener(
      new PopupAdapter(remoteFileList, remoteFileMenu));
  }

  public void updateMyFileList(List<FileState> fileState) {
    DefaultListModel<FileState> model =
      (DefaultListModel<FileState>) myFileList.getModel();
    model.removeAllElements();

    for (FileState state : fileState) {
      model.addElement(state);
    }
  }

  public void updateRemoteFileList(List<RemoteFileState> fileState) {
    DefaultListModel<RemoteFileState> model =
      (DefaultListModel<RemoteFileState>) remoteFileList.getModel();
    model.removeAllElements();

    for (RemoteFileState state : fileState) {
      model.addElement(state);
    }
  }

  @Override
  public void updatePeerList(List<PeerInfo> peersInfo) {
    DefaultListModel<PeerInfo> model =
      (DefaultListModel<PeerInfo>) peerList.getModel();
    if (peersInfo.size() < model.size())
      updateRemoteFileList(new ArrayList<>());
    model.removeAllElements();

    for (PeerInfo info : peersInfo) {
      model.addElement(info);
    }
  }

  @Override
  public void updateConnectionStatus(boolean connected) {
    btnConnect.setText(connected ? R.DISCONNECT : R.CONNECT);
    if (!connected)
      updatePeerList(new ArrayList<>());
  }

  @Override
  public void publishStatus(String status) {
    statusLabel.setText(String.format(R.STATUS, status));
  }
}
