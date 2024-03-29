package com.chess.gui;

import java.awt.Dimension;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Table {
    
    private final JFrame gameFrame;
    private static Dimension OUTER_FRAME_DIMENSION = new Dimension(650, 650);
    
    public Table() {
        this.gameFrame = new JFrame("Timothy's Chess in Java");
        final JMenuBar tableMenuBar = new JMenuBar();
        populateMenuBar(tableMenuBar);
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.gameFrame.setVisible(true);
        
    }
    
    private void populateMenuBar(final JMenuBar tableMenuBar) {
        tableMenuBar.add(createFileMenu());
    }
    
    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        
        final JMenuItem openPGN = new JMenuItem("Load PGN file");
        openPGN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("open up that pgn file");
            }
        });
        fileMenu.add(openPGN);
        
        return fileMenu;
    }
    
}
