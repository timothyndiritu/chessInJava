package com.chess.tests;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MoveFactory;
// import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.AI.MinMax;
import com.chess.engine.player.AI.MoveStrategy;

// import com.chess.engine.player.ai.StandardBoardEvaluator;
// import com.chess.pgn.FenUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestBoard {

        @Test
        public void testFoolsMate() {
                final Board board = Board.createStandardBoard();
                final MoveTransition move1 = board.currentPlayer().makeMove(Move.MoveFactory.createMove(board,
                                BoardUtils.getCoordinateAtPosition("f2"), BoardUtils.getCoordinateAtPosition("f3")));

                Assert.assertTrue(move1.getMoveStatus().isDone());

                final MoveTransition move2 = move1.getTransitionBoard().currentPlayer()
                                .makeMove(Move.MoveFactory.createMove(
                                                move1.getTransitionBoard(), BoardUtils.getCoordinateAtPosition("e7"),
                                                BoardUtils.getCoordinateAtPosition("e5")));

                Assert.assertTrue(move2.getMoveStatus().isDone());

                final MoveTransition move3 = move2.getTransitionBoard().currentPlayer()
                                .makeMove(Move.MoveFactory.createMove(
                                                move2.getTransitionBoard(), BoardUtils.getCoordinateAtPosition("g2"),
                                                BoardUtils.getCoordinateAtPosition("g4")));

                Assert.assertTrue(move3.getMoveStatus().isDone());

                final MoveStrategy strategy = new MinMax(4);
                final Move aiMove = strategy.execute(move3.getTransitionBoard());

                final Move bestMove = Move.MoveFactory.createMove(
                                move3.getTransitionBoard(), BoardUtils.getCoordinateAtPosition("d8"),
                                BoardUtils.getCoordinateAtPosition("h4"));
                assertEquals(bestMove, aiMove);
        }

}