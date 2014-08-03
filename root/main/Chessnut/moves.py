"""
Generates and returns a dictionary containing the superset of (i.e., all)
legal moves on a chessboard.

The structure returned is a dictionary using single-character symbols for keys
(representing each type of chess piece, e.g., 'k', 'Q', 'N', 'r', etc. -- with
lowercase letters for black pieces and uppercase for white), and whose values
are 64-element lists.

The list indices correspond to a raster-style index of a chessboard (i.e.,
'a8'=0, 'h8'=8, 'a7'=8,...'h1'=63) representing the starting square of the
piece. Each element of the list is another list that contains 8 or fewer
elements that represent vectors for the 8 possible directions ("rays") that a
chesspiece could move. Each vector is a list containing integers that
represent the ending index of a legal move, sorted by increasing distance
from the starting point. Empty vectors are removed from the list.

For example: A queen on 'h8' (idx = 7) can move to the left (West) to each
of the indices 0, 1, 2, 3, 4, 5, 6, and cannot move right (East), right/up
(Northeast), up (North), up/left (Northwest), or down/right (Southeast)
because the piece is at the corner of the board. Thus,

len(MOVES['q'][7]) == 3  # West, Southwest, & South

 - and -

MOVES['q'][7][0] = [6, 5, 4, 3, 2, 1, 0]  # sorted by distance from idx = 7

Which says that a black queen at 'h8' can move in a line to 'g8', 'f8',...'a8'.

Generalizing:

MOVES[<piece>][<starting index>][<direction>] = [list of moves]

This list of moves assumes that there are no other pieces on the board, so the
actual set of legal moves for a particular board will be a subset of those
returned here. Organizing the moves this way allows implicit validation when
searching for legal moves on a particular board because any illegal move
(e.g., blocked position) will short-circuit testing the remainder of the ray.
It isn't a significant computational savings, but it simplifies the logic for
determining legal moves.
"""

from math import atan2
from copy import deepcopy


# Precalculate angles for index pairs that form legal moves - straight lines
# (up, down, left, right, diagonal) and the 8 directions a knight can move;
# the index of the angle within this list will be used as the ray index to
# group moves that lie in the same direction.
DIRECTIONS = [(1, 0), (1, 1), (0, 1), (-1, 1),  # straight lines
              (-1, 0), (-1, -1), (0, -1), (1, -1),
              (2, 1), (1, 2), (-1, 2), (-2, 1),  # knights
              (-2, -1), (-1, -2), (1, -2), (2, -1),
              ]
RAYS = [atan2(d[1], d[0]) for d in DIRECTIONS]


# These keys are chess piece names, and the values are functions that verify
# legality of moving in a particular direction for that type of piece. Symbols
# for the white piece ('K', 'Q', 'N', 'B', and 'R') do not need to be
# explicitly tested because their moves are the same as the black pieces for
# all pieces *except* pawns, which differ because they are the only pieces
# that cannot move backwards.
PIECES = {'k': lambda y, dx, dy: abs(dx) <= 1 and abs(dy) <= 1,
          'q': lambda y, dx, dy: dx == 0 or dy == 0 or abs(dx) == abs(dy),
          'n': lambda y, dx, dy: (abs(dx) >= 1 and
                                  abs(dy) >= 1 and
                                  abs(dx) + abs(dy) == 3),
          'b': lambda y, dx, dy: abs(dx) == abs(dy),
          'r': lambda y, dx, dy: dx == 0 or dy == 0,
          'p': lambda y, dx, dy: (y < 8 and abs(dx) <= 1 and dy == -1),
          'P': lambda y, dx, dy: (y > 1 and abs(dx) <= 1 and dy == 1),
          }

MOVES = dict()

for sym, is_legal in list(PIECES.items()):

    MOVES[sym] = list()

    for idx in range(64):

        # Initialize arrays for each of the 8 possible directions that a
        # piece could be moved; some of these will be empty and
        # removed later
        MOVES[sym].append([list() for _ in range(8)])

        # Sorting the list of end points by distance from the starting
        # point ensures that the ouptut order is properly sorted
        for end in sorted(list(range(64)), key=lambda x: abs(x - idx)):

            # Determine the row, change in column, and change in row
            # of the start/end point pair for move validation
            y = 8 - idx // 8
            dx = (end % 8) - (idx % 8)
            dy = (8 - end // 8) - y

            if idx == end or not is_legal(y, dx, dy):
                continue

            angle = atan2(dy, dx)
            if angle in RAYS:

                # Mod by 8 to shift the ray index of knight moves down
                # by 8 from the index found in DIRECTIONS; the ray index of
                # all other pieces will be unchanged
                ray_num = RAYS.index(angle) % 8
                MOVES[sym][idx][ray_num].append(end)

        # Remove unused (empty) lists
        MOVES[sym][idx] = [r for r in MOVES[sym][idx] if r]

# Create references to remaining pieces - the original set is only
# minimally covering; Pawns are already included.
for sym in ['K', 'Q', 'N', 'B', 'R']:
    MOVES[sym] = deepcopy(MOVES[sym.lower()])

# Directly add castling for kings
MOVES['k'][4][0].append(6)
MOVES['k'][4][1].append(2)
MOVES['K'][60][0].append(62)
MOVES['K'][60][4].append(58)

# Directly add double-space pawn opening moves
IDX = 0
for i in range(8):
    MOVES['p'][8 + i][IDX].append(24 + i)
    MOVES['P'][55 - i][IDX].append(39 - i)
    IDX = 1
