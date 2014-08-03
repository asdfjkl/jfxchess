
"""
The game module implements core Chessnut class, `Game`, to control a chess
game.

Two additional classes are defined: `InvalidMove` -- a subclass of the base
`Exception` class, and `State` -- a namedtuple for handling game state
information.

Chessnut has neither an *engine*, nor a *GUI*, and it cannot currently
handle any chess variants (e.g., Chess960) that are not equivalent to standard
chess rules.
"""

from collections import namedtuple

from Chessnut.board import Board
from Chessnut.moves import MOVES

# Define a named tuple with FEN field names to hold game state information
State = namedtuple('State', ['player', 'rights', 'en_passant', 'ply', 'turn'])


class InvalidMove(Exception):
    """
    Subclass base `Exception` so that exception handling doesn't have to
    be generic.
    """
    pass


class Game(object):
    """
    This class manages a chess game instance -- it stores an internal
    representation of the position of each piece on the board in an instance
    of the `Board` class, and the additional state information in an instance
    of the `State` namedtuple class.
    """

    default_fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

    def __init__(self, fen=default_fen, validate=True):
        """
        Initialize the game board to the supplied FEN state (or the default
        starting state if none is supplied), and determine whether to check
        the validity of moves returned by `get_moves()`.
        """
        self.board = Board()
        self.state = State(' ', ' ', ' ', ' ', ' ')
        self.move_history = []
        self.fen_history = []
        self.validate = validate
        self.set_fen(fen=fen)

    def __str__(self):
        """Return the current FEN representation of the game."""
        return ' '.join(str(x) for x in [self.board] + list(self.state))

    @staticmethod
    def i2xy(pos_idx):
        """
        Convert a board index to algebraic notation.
        """
        return chr(97 + pos_idx % 8) + str(8 - pos_idx // 8)

    @staticmethod
    def xy2i(pos_xy):
        """
        Convert algebraic notation to board index.
        """
        return (8 - int(pos_xy[1])) * 8 + (ord(pos_xy[0]) - ord('a'))

    def set_fen(self, fen):
        """
        Parse a FEN string into components and store in the `board` and `state`
        properties, and append the FEN string to the game history *without*
        clearing it first.
        """
        self.fen_history.append(fen)
        fields = fen.split(' ')
        fields[4] = int(fields[4])
        fields[5] = int(fields[5])
        self.state = State(*fields[1:])
        self.board.set_position(fields[0])

    def reset(self, fen=default_fen):
        """
        Clear the game history and set the board to the default starting
        position.
        """
        self.move_history = []
        self.fen_history = []
        self.set_fen(fen)

    # def _translate(self, move):
        # """
        # Translate FEN castling notation to simple algebraic move notation.
        # """
        # if move == 'O-O':
        #     move = 'e1g1' if self.state.player == 'w' else 'e8g8'
        # elif move == 'O-O-O':
        #     move = 'e1c1' if self.state.player == 'w' else 'e8c8'
        # return move

    def apply_move(self, move):
        """
        Take a move in simple algebraic notation and apply it to the game.
        Note that simple algebraic notation differs from FEN move notation
        in that castling is not given any special notation, and pawn promotion
        piece is always lowercase.

        Update the state information (player, castling rights, en passant
        target, ply, and turn), apply the move to the game board, and
        update the game history.
        """

        # declare the status fields using default parameters
        fields = ['w', 'KQkq', '-', 0, 1]
        # move = self._translate(move)

        start = Game.xy2i(move[:2])
        end = Game.xy2i(move[2:4])
        piece = self.board.get_piece(start)
        target = self.board.get_piece(end)

        if self.validate and move not in self.get_moves(idx_list=[start]):
            raise InvalidMove("\nIllegal move: {}\nfen: {}".format(move,
                                                                   str(self)))

        # toggle the active player
        fields[0] = {'w': 'b', 'b': 'w'}[self.state.player]

        # modify castling rights - the set of castling rights that *might*
        # be voided by a move is uniquely determined by the starting index
        # of the move - regardless of what piece moves from that position
        # (excluding chess variants like chess960).
        rights_map = {0: 'q', 4: 'kq', 7: 'k',
                      56: 'Q', 60: 'KQ', 63: 'K'}
        void_set = ''.join([rights_map.get(start, ''),
                           rights_map.get(end, '')])
        new_rights = [r for r in self.state.rights if r not in void_set]
        fields[1] = ''.join(new_rights) or '-'

        # set en passant target square when a pawn advances two spaces
        if piece.lower() == 'p' and abs(start - end) == 16:
            fields[2] = Game.i2xy((start + end) // 2)

        # reset the half move counter when a pawn moves or is captured
        fields[3] = self.state.ply + 1
        if piece.lower() == 'p' or target.lower() != ' ':
            fields[3] = 0

        # Increment the turn counter when the next move is from white, i.e.,
        # the current player is black
        fields[4] = self.state.turn
        if self.state.player == 'b':
            fields[4] = self.state.turn + 1

        # check for pawn promotion
        if len(move) == 5:
            piece = move[4]
            if self.state.player == 'w':
                piece = piece.upper()

        # record the move in the game history and apply it to the board
        self.move_history.append(move)
        self.board.move_piece(start, end, piece)

        # move the rook to the other side of the king in case of castling
        c_type = {62: 'K', 58: 'Q', 6: 'k', 2: 'q'}.get(end, None)
        if piece.lower() == 'k' and c_type and c_type in self.state.rights:
            coords = {'K': (63, 61), 'Q': (56, 59),
                      'k': (7, 5), 'q': (0, 3)}[c_type]
            r_piece = self.board.get_piece(coords[0])
            self.board.move_piece(coords[0], coords[1], r_piece)

        # in en passant remove the piece that is captured
        if piece.lower() == 'p' and self.state.en_passant != '-' \
                and Game.xy2i(self.state.en_passant) == end:
            ep_tgt = Game.xy2i(self.state.en_passant)
            if ep_tgt < 24:
                self.board.move_piece(end + 8, end + 8, ' ')
            elif ep_tgt > 32:
                self.board.move_piece(end - 8, end - 8, ' ')

        # state update must happen after castling
        self.set_fen(' '.join(str(x) for x in [self.board] + list(fields)))

    def get_moves(self, player=None, idx_list=range(64)):
        """
        Get a list containing the legal moves for pieces owned by the
        specified player that are located at positions included in the
        idx_list. By default, it compiles the list for the active player
        (i.e., self.state.player) by filtering the list of _all_moves() to
        eliminate any that would expose the player's king to check.
        """
        if not self.validate:
            return self._all_moves(player=player, idx_list=idx_list)

        if not player:
            player = self.state.player

        res_moves = []

        # This is the most inefficient part of the model - there is no cache
        # for previously computed move lists, so creating a new test board
        # each time and applying the moves incurs high overhead, and throws
        # away the result at the end of each pass through the loop
        test_board = Game(fen=str(self), validate=False)
        for move in self._all_moves(player=player, idx_list=idx_list):

            test_board.reset(fen=str(self))

            # Don't allow castling out of or through the king in check
            k_sym, opp = {'w': ('K', 'b'), 'b': ('k', 'w')}.get(player)
            k_loc = Game.i2xy(self.board.find_piece(k_sym))
            op_moves = set([m[2:4] for m in test_board.get_moves(player=opp)])
            castle_gap = {'e1g1': 'e1f1', 'e1c1': 'e1d1',
                          'e8g8': 'e8f8', 'e8c8': 'e8d8'}.get(move, '')
            if (k_loc in {'k': 'e8', 'K': 'e1'}.get(k_sym, '') and
                    (k_loc in op_moves or castle_gap
                        and castle_gap not in res_moves)):
                continue

            # Apply the move to the test board to ensure that the king does
            # not end up in check
            test_board.apply_move(move)
            tgts = set([m[2:4] for m in test_board.get_moves()])

            if Game.i2xy(test_board.board.find_piece(k_sym)) not in tgts:
                res_moves.append(move)

        return res_moves

    def _all_moves(self, player=None, idx_list=range(64)):
        """
        Get a list containing all reachable moves for pieces owned by the
        specified player (including moves that would expose the player's king
        to check) that are located at positions included in the idx_list. By
        default, it compiles the list for the active player (i.e.,
        self.state.player) by checking every square on the board.
        """
        res_moves = []
        for start in idx_list:
            if self.board.get_owner(start) != (player or self.state.player):
                continue

            # MOVES contains the list of all possible moves for a piece of
            # the specified type on an empty chess board.
            piece = self.board.get_piece(start)
            rays = MOVES.get(piece, [''] * 64)

            for ray in rays[start]:
                # Trace each of the 8 (or fewer) possible directions that a
                # piece at the given starting index could move

                res_moves.extend(self._trace_ray(start, piece, ray))

        return res_moves

    def _trace_ray(self, start, piece, ray):
        """
        Return a list of moves by filtering the supplied ray (a list of
        indices corresponding to end points that lie on a common line from
        the starting index) based on the state of the chess board (e.g.,
        castling, capturing, en passant, etc.). Moves are in simple algebraic
        notation, e.g., 'a2a4', 'g7h8q', etc.

        Each ray should be an element from Chessnut.MOVES, representing all
        the moves that a piece could make from the starting square on an
        otherwise blank chessboard. This function filters the moves in a ray
        by enforcing the rules of chess for the legality of capturing pieces,
        castling, en passant, and pawn promotion.
        """
        res_moves = []

        for end in ray:

            sym = piece.lower()
            del_x = abs(end - start) % 8
            move = [Game.i2xy(start) + Game.i2xy(end)]
            tgt_owner = self.board.get_owner(end)

            # Abort if the current player owns the piece at the end point
            if tgt_owner == self.state.player:
                break

            # Test castling exception for king
            if sym == 'k' and del_x == 2:
                gap_owner = self.board.get_owner((start + end) // 2)
                out_owner = self.board.get_owner(end - 1)
                rights = {62: 'K', 58: 'Q', 6: 'k', 2: 'q'}.get(end, ' ')
                if (tgt_owner or gap_owner or rights not in self.state.rights
                        or (rights.lower() == 'q' and out_owner)):
                    # Abort castling because missing castling rights
                    # or piece in the way
                    break

            if sym == 'p':
                # Pawns cannot move forward to a non-empty square
                if del_x == 0 and tgt_owner:
                    break

                # Test en passant exception for pawn
                elif del_x != 0 and not tgt_owner:
                    ep_coords = self.state.en_passant
                    if ep_coords == '-' or end != Game.xy2i(ep_coords):
                        break

                # Pawn promotions should list all possible promotions
                if (end < 8 or end > 55):
                    move = [move[0] + s for s in ['b', 'n', 'r', 'q']]

            res_moves.extend(move)

            # break after capturing an enemy piece
            if tgt_owner:
                break

        return res_moves
