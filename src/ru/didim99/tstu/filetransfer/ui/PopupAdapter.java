package ru.didim99.tstu.filetransfer.ui;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PopupAdapter extends MouseAdapter {
  private JPopupMenu menu;
  private JList list;

  PopupAdapter(JList list, JPopupMenu menu) {
    this.list = list;
    this.menu = menu;
  }

  @Override
  public void mouseReleased(MouseEvent e) { checkEvent(e); }

  @Override
  public void mousePressed(MouseEvent e) { checkEvent(e); }

  private void checkEvent(MouseEvent e) {
    if (e.isPopupTrigger()) {
      int index = list.locationToIndex(e.getPoint());
      if (index != -1) {
        list.setSelectedIndex(index);
        menu.show(list, e.getX(), e.getY());
      }
    }
  }
}