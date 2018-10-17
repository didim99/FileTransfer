package ru.didim99.tstu.filetransfer.ui;

import java.awt.*;
import javax.swing.*;
import ru.didim99.tstu.filetransfer.core.file.RemoteFileState;

class RemoteFileListRenderer implements ListCellRenderer<RemoteFileState> {
  private final DefaultListCellRenderer defaultRenderer;

  RemoteFileListRenderer() {
    defaultRenderer = new DefaultListCellRenderer();
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends RemoteFileState> list,
                                                RemoteFileState value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
    Component c = defaultRenderer.getListCellRendererComponent(
      list, value, index, isSelected, cellHasFocus);

    ((JLabel) c).setText(value.getFileName());

    return c;
  }
}
