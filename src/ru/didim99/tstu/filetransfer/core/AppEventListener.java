package ru.didim99.tstu.filetransfer.core;

import ru.didim99.tstu.filetransfer.core.file.FileManager;

public interface AppEventListener {
  void onAppEvent(Application.Event event);
  void onFileEvent(FileManager.Event event);
  void onPeerSelected(int peerIndex);
}
