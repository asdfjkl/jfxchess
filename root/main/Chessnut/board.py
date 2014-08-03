"""
The board class manages the position of pieces, and conversion to and from
Forsyth-Edwards Notation (FEN). This class is only used internally by the
`Game` class.
"""


class Board(object):
    """
    This class manages the position of all pieces in a chess game. The
    position is stored as a list of single-character strings.
    """

    def __init__(self, position=' ' * 64):
        self._position = []
        self.set_position(position)

    def __str__(self):
        """
        Convert the piece placement array to a FEN string.
        """
        pos = []
        for idx, piece in enumerate(self._position):

            # add a '/' at the end of each row
            if idx > 0 and idx % 8 == 0:
                pos.append('/')

            # blank spaces must be converted to numbers in the final FEN
            if not piece.isspace():
                pos.append(piece)
            elif pos and pos[-1].isdigit():
                pos[-1] = str(int(pos[-1]) + 1)
            else:
                pos.append('1')
        return ''.join(pos)

    def set_position(self, position):
        """
        Convert a FEN position string into a piece placement array.
        """
        self._position = []
        for char in position:
            if char is '/':  # skip row separator character
                continue
            elif char.isdigit():
                # replace numbers characters with that number of spaces
                self._position.extend([' '] * int(char))
            else:
                self._position.append(char)

    def get_piece(self, index):
        """Get the piece at the given index in the position array."""
        return self._position[index]

    def get_owner(self, index):
        """
        Get the owner of the piece at the given index in the position array.
        """
        piece = self.get_piece(index)
        if not piece.isspace():
            return 'w' if piece.isupper() else 'b'
        return None

    def move_piece(self, start, end, piece):
        """
        Move a piece by removing it from the starting position and adding it
        to the end position. If a different piece is provided, that piece will
        be placed at the end index instead.
        """
        self._position[end] = piece
        self._position[start] = ' '

    def find_piece(self, symbol):
        """
        Find the index of the specified piece on the board, returns -1 if the
        piece is not on the board.
        """
        return ''.join(self._position).find(symbol)
