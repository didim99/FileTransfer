package ru.didim99.tstu.filetransfer.ui;

import java.awt.*;
import javax.swing.*;
import ru.didim99.tstu.filetransfer.core.file.FileState;

class FileListRenderer implements ListCellRenderer<FileState> {
  private final DefaultListCellRenderer defaultRenderer;

  FileListRenderer() {
    defaultRenderer = new DefaultListCellRenderer();
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends FileState> list, FileState value,
                                                int index, boolean isSelected, boolean cellHasFocus) {
    Component c = defaultRenderer.getListCellRendererComponent(
      list, value, index, isSelected, cellHasFocus);

    ((JLabel) c).setText(value.getDisplayName());

    return c;
  }
}
