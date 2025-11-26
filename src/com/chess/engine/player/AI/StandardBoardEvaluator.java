package com.chess.engine.player.AI;

import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;

public final class StandardBoardEvaluator implements BoardEvaluator {

  private static final int CHECK_BONUS = 50;
  private static final int CHECKMATE_BONUS = 10000;
  private static final int DEPTH_BONUS = 100;
  private static final int CASTLEBONUS = 60;

  @Override
  public int evaluate(final Board board, final int depth) {

    return scorePlayer(board, board.whitePlayer(), depth) - scorePlayer(board, board.blackPlayer(), depth);

  }

  private int scorePlayer(final Board board, final Player player, final int depth) {
    return pieceValue(player) + mobility(player) + check(player) + checkMate(player, depth) + castled(player);
    // +checkmate , check, castled, mobility
  }

  private int castled(Player player) {

    // Being castled is a lasting, useful feature (safer king, activated rook), so
    // the evaluator gives a positive bonus whenever the player’s king is already
    // castled. That encourages the AI to reach and keep castled positions.

    return player.isCastled() ? CASTLEBONUS : 0;
  }

  private static int checkMate(Player player, int depth) {
    // MinMax search passes a decreasing remaining-depth into the evaluator. A
    // larger remaining depth at the evaluation point means the mate is achieved
    // sooner from the root. Scaling the checkmate bonus by remaining depth
    // therefore makes the engine favor faster wins.
    return player.getOpponent().isInCheckMate() ? CHECKMATE_BONUS * depthBonus(depth) : 0;
  }

  private static int depthBonus(int depth) {
    return depth == 0 ? 1 : DEPTH_BONUS * depth;
  }

  private static int check(final Player player) {
    return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
  }

  private static int mobility(final Player player) {
    return player.getLegalMoves().size();
  }

  private static int pieceValue(final Player player) {
    int pieceValueScore = 0;
    for (final Piece piece : player.getActivePieces()) {
      pieceValueScore += piece.getPieceValue();
    }
    return pieceValueScore;
  }

}
