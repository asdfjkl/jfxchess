"""
Chessnut is a simple chess board model written in Python. Chessnut is
*not* a chess engine -- it has no AI to play games, and it has no GUI. It is
a simple package that can import/export games in Forsyth-Edwards Notation
(FEN), generate a list of legal moves for the current board position,
intelligently validate & apply moves (including en passant, castling, etc.),
and keep track of the game with a history of both moves and corresponding
FEN representation.

Chessnut is not written for speed, it is written for simplicity (there are
only two real classes, and only about 200 lines of code). By adding a custom
move evaluation function, Chessnut could be used as a chess engine. The
simplicity of the model lends itself well to studying the construction of a
chess engine without worrying about implementing a chess model, or to easily
find the set of legal moves for either player on a particular chess board for
use in conjunction with another chess application.

To use Chessnut, import the Game class from the module:

    from Chessnut import Game

    chessgame = Game()  # Initialize a game in the standard opening position

    chessgame.get_moves()  # List of the 20 legal opening moves for white

    chessgame.apply_move('e2e4')  # Advance the pawn from e2 to e4

    chessgame.apply_move('e2e4')  # raise InvalidMove (no piece on e2)
"""

# import module functions and promote into the package namespace
#from Chessnut.game import Game
