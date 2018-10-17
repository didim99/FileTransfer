package ru.didim99.tstu.filetransfer.ui;

import java.awt.*;
import javax.swing.*;
import ru.didim99.tstu.filetransfer.core.network.PeerInfo;

class PeerListRenderer implements ListCellRenderer<PeerInfo> {
  private final DefaultListCellRenderer defaultRenderer;

  PeerListRenderer() {
    defaultRenderer = new DefaultListCellRenderer();;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends PeerInfo> list, PeerInfo value,
                                                int index, boolean isSelected, boolean cellHasFocus) {
    Component c = defaultRenderer.getListCellRendererComponent(
      list, value, index, isSelected, cellHasFocus);

    ((JLabel) c).setText(value.getDisplayName());

    return c;
  }
}
