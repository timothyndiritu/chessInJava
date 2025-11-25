package com.chess.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.chessTile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.BorderFactory;
import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table {

    private final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;

    private Board chessBoard;

    private chessTile sourceTile;
    private chessTile destinationTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;

    // id of a tile to flash when an illegal move is attempted (-1 = none)
    private int illegalMoveTile = -1;
    private Timer illegalMoveTimer = null;

    private boolean highlightLegalMoves;

    // new: track whether app window is active (has focus)
    private boolean isActiveWindow;

    private final static Dimension OUTER_FRAME_DIMENSION = new Dimension(800, 650);
    private final static Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
    private final static Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);
    private static String defaultPieceImagesPath = "art/downloaded/";

    private final Color lightTileColor = new Color(0xE7E7CB);
    private final Color darkTileColor = new Color(0x8476BA);

    public Table() {
        this.gameFrame = new JFrame("Timothy's Chess in Java");
        this.gameFrame.setLayout(new BorderLayout());
        final JMenuBar tableMenuBar = createTableMenuBar();
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.chessBoard = Board.createStandardBoard();
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = false;

        // initialize active state and listen for focus changes
        this.isActiveWindow = true;
        this.gameFrame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                isActiveWindow = true;
                boardPanel.drawBoard(chessBoard);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                isActiveWindow = false;
                boardPanel.drawBoard(chessBoard);
            }
        });

        this.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        this.gameFrame.setVisible(true);
        // Ensure the board and frame request focus when first shown so the
        // initial mouse click is delivered to components (prevents first-click
        // being used only to focus the window on some platforms).
        this.boardPanel.setFocusable(true);
        this.boardPanel.requestFocusInWindow();
        this.gameFrame.requestFocus();
        this.gameFrame.toFront();

    }

    private JMenuBar createTableMenuBar() {
        final JMenuBar tableMenuBar = new JMenuBar();
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        return tableMenuBar;
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

        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(exitMenuItem);

        return fileMenu;
    }

    private JMenu createPreferencesMenu() {

        final JMenu preferenceMenu = new JMenu("Preferences");
        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip Board");
        flipBoardMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                boardDirection = boardDirection.opposite();
                boardPanel.drawBoard(chessBoard);
            }
        });
        preferenceMenu.add(flipBoardMenuItem);

        preferenceMenu.addSeparator();

        final JCheckBoxMenuItem legalMovesHighlighterCheckBox = new JCheckBoxMenuItem("Highlight Legal Moves", false);

        legalMovesHighlighterCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                highlightLegalMoves = legalMovesHighlighterCheckBox.isSelected();
                // Redraw board so highlights appear/disappear immediately
                boardPanel.drawBoard(chessBoard);
            }

        });

        preferenceMenu.add(legalMovesHighlighterCheckBox);

        return preferenceMenu;
    }

    public enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }

        }, FLIPPED {
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                final List<TilePanel> reversed = new ArrayList<>(boardTiles);
                Collections.reverse(reversed);
                return reversed;
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }

        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);

        abstract BoardDirection opposite();
    }

    private class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8, 8));
            // allow the panel to receive focus so requestFocusInWindow() works
            setFocusable(true);
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            validate();

        }

        public void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel tilePanel : boardDirection.traverse(boardTiles)) {
                tilePanel.drawTile(board);

                add(tilePanel);
            }
            validate();
            repaint();
        }
    }

    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return this.moves;
        }

        public void addMove(final Move move) {
            this.moves.add(move);
        }

        public int size() {
            return this.moves.size();
        }

        public void clear() {
            this.moves.clear();
        }

        public Move removeMove(int index) {
            return this.moves.remove(index);
        }

        public boolean removeMove(final Move move) {
            return this.moves.remove(move);
        }
    }

    private class TilePanel extends JPanel {

        private final int tileId;
        private final MouseListener tileMouseListener;

        TilePanel(final BoardPanel boardPanel,
                final int tileId) {
            super(new GridBagLayout());
            // allow tile panels to be focusable if needed
            setFocusable(true);
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);

            // store the listener so we can attach it to child components (piece image / dots)
            this.tileMouseListener = new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    System.out.println("Tile clicked: " + tileId);
                    if (isRightMouseButton(e)) {
                        sourceTile = null;
                        destinationTile = null;
                        humanMovedPiece = null;

                    } else if (isLeftMouseButton(e)) {
                        if (sourceTile == null) {
                            //pick piece to move
                            sourceTile = chessBoard.getTile(tileId);
                            humanMovedPiece = sourceTile.getPiece();
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                            // redraw immediately so legal-move highlights show for the selected piece
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    boardPanel.drawBoard(chessBoard);
                                }
                            });
                        } else {
                            //move the actual piece
                            destinationTile = chessBoard.getTile(tileId);
                            final Move move = Move.MoveFactory.createMove(chessBoard, sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);

                                    if (transition.getMoveStatus().isDone()) {
                                        chessBoard = transition.getTransitionBoard();
                                        moveLog.addMove(move);
                                    } else {
                                        // illegal move: flash the destination tile red briefly
                                        if (illegalMoveTimer != null) {
                                            illegalMoveTimer.stop();
                                            illegalMoveTimer = null;
                                        }
                                        illegalMoveTile = sourceTile.getTileCoordinate();
                                        // redraw now to show the red flash
                                        boardPanel.drawBoard(chessBoard);
                                        illegalMoveTimer = new Timer(500, new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                illegalMoveTile = -1;
                                                boardPanel.drawBoard(chessBoard);
                                                if (illegalMoveTimer != null) {
                                                    illegalMoveTimer.stop();
                                                    illegalMoveTimer = null;
                                                }
                                            }
                                        });
                                        illegalMoveTimer.setRepeats(false);
                                        illegalMoveTimer.start();
                                    }

                                    sourceTile = null;
                                    destinationTile = null;
                                    humanMovedPiece = null;
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                gameHistoryPanel.redo(chessBoard, moveLog);
                                takenPiecesPanel.redo(moveLog);
                                boardPanel.drawBoard(chessBoard);

                            }
                        });
                    }

                }

                @Override
                public void mousePressed(final MouseEvent e) {

                }

                @Override
                public void mouseReleased(final MouseEvent e) {

                }

                @Override
                public void mouseEntered(final MouseEvent e) {

                }

                @Override
                public void mouseExited(final MouseEvent e) {

                }

            };

            // attach to the tile panel itself
            addMouseListener(this.tileMouseListener);

            validate();
        }

        public void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightLegals(board);
            // if this tile is flagged for an illegal-move flash, paint it red
            if (illegalMoveTile == this.tileId) {
                setBackground(Color.RED);
            }

            // highlight the selected source tile with a visible border
            if (sourceTile != null && sourceTile.getTileCoordinate() == this.tileId) {
                // setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
                //change the background color to indicate selection to a lighter purple
                setBackground(new Color(0xBFA2DB));
                
            } else {
                setBorder(null);
            }

            validate();
            repaint();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if (board.getTile(this.tileId).isTileOccupied()) {
                try {
                    final BufferedImage image
                            = ImageIO.read(new File(defaultPieceImagesPath + board.getTile(this.tileId).getPiece().getPieceAlliance().toString().substring(0, 1)
                                    + board.getTile(this.tileId).getPiece().toString() + ".png"));
                    // use grayscale/disabled image when window is inactive, but keep selected piece visible
                    final boolean isSelected = sourceTile != null && sourceTile.getTileCoordinate() == this.tileId;
                    final Image displayImage = (isActiveWindow || isSelected) ? image : GrayFilter.createDisabledImage(image);
                    final GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.anchor = GridBagConstraints.CENTER;
                    final JLabel pieceLabel = new JLabel(new ImageIcon(displayImage));
                    // also listen on the label so clicks on the image aren't swallowed by the child
                    pieceLabel.addMouseListener(tileMouseListener);
                    add(pieceLabel, gbc);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void highlightLegals(final Board board) {
            if (highlightLegalMoves) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            Image dot = ImageIO.read(new File("art/misc/green_dot.png"));
                            if (!isActiveWindow) {
                                dot = GrayFilter.createDisabledImage(dot);
                            }
                            final GridBagConstraints gbc = new GridBagConstraints();
                            gbc.gridx = 0;
                            gbc.gridy = 1;
                            gbc.anchor = GridBagConstraints.SOUTH;
                            final JLabel dotLabel = new JLabel(new ImageIcon(dot));
                            dotLabel.addMouseListener(tileMouseListener);
                            add(dotLabel, gbc);
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if (humanMovedPiece != null && humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTileColor() {
            if (BoardUtils.EIGHTH_RANK[this.tileId]
                    || BoardUtils.SIXTH_RANK[this.tileId]
                    || BoardUtils.FOURTH_RANK[this.tileId]
                    || BoardUtils.SECOND_RANK[this.tileId]) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if (BoardUtils.SEVENTH_RANK[this.tileId]
                    || BoardUtils.FIFTH_RANK[this.tileId]
                    || BoardUtils.THIRD_RANK[this.tileId]
                    || BoardUtils.FIRST_RANK[this.tileId]) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }

        }

    }
}
