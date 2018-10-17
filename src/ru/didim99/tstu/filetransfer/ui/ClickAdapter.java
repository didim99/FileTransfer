package ru.didim99.tstu.filetransfer.ui;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClickAdapter extends MouseAdapter {
  private ListClickListener listener;
  private JList list;

  ClickAdapter(JList list, ListClickListener listener) {
    this.listener = listener;
    this.list = list;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    int index = list.locationToIndex(e.getPoint());
    if (index != -1) {
      list.setSelectedIndex(index);
      listener.onClick(index);
    }
  }
}
