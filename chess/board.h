/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */


#ifndef BOARD_H
#define BOARD_H


#include <cstdint>
#include <QRegularExpression>
#include <QMap>
#include "move.h"

namespace chess {

// empty board
const uint8_t EMPTY_POS[120] = {
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
};


// initial board position
const uint8_t INIT_POS[120] = {
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0x04, 0x02, 0x03, 0x05, 0x06, 0x03, 0x02, 0x04, 0xFF,
    0xFF, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
    0xFF, 0x81, 0x81, 0x81, 0x81, 0x81, 0x81, 0x81, 0x81, 0xFF,
    0xFF, 0x84, 0x82, 0x83, 0x85, 0x86, 0x83, 0x82, 0x84, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
};

// attack table
// the index of this array corresponds to the distance
// between two squares of the board (note the board is
// encoded as a one dim array of size 120, where A1 = 21, H1 = 28
// A8 = 91, A8 = 98.
// the value denotes whether an enemy rook, biship knight, queen, king
// on one square can attack the other square. The following encoding
// is used:
// Bitposition    Piece
// 0              Knight
// 1              Bishop
// 2              Rook
// 3              Queen
// 4              King
// e.g. distance one, i.e. index 1 (=left, up, down, right square) has
// value 0x1C = MSB 00011100 LSB, i.e. king, queen, rook can
// potentially attack
const uint8_t ATTACK_TABLE[78] = {
    0x00, 0x1C, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x01, 0x1a,
    0x1C, 0x1A, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x01,
    0x0C, 0x01, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00,
    0x0C, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00,
    0x0C, 0x00, 0x00, 0x00, 0x0A, 0x0a, 0x00, 0x00, 0x00, 0x00,
    0x0C, 0x00, 0x00, 0x00, 0x0a, 0x0A, 0x00, 0x00, 0x00, 0x00,
    0x0C, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x00,
    0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A
};

const uint8_t IDX_BPAWN = 0;
const uint8_t IDX_WPAWN = 1;
const uint8_t IDX_KNIGHT = 2;
const uint8_t IDX_BISHOP = 3;
const uint8_t IDX_ROOK = 4;
const uint8_t IDX_QUEEN = 5;
const uint8_t IDX_KING = 6;

// first dim is for different piece types
// [piece_type[0] is DCOUNT (as in Byte Magazine paper)
// [piece_type[1] ... [piece_type][4] resp.
// [piece_type[1] ... [piece_type][8] contain
// DPOINT table
const int8_t DIR_TABLE[7][9] = {
    { 4, -10, -20, -11, -9 ,   0,   0,   0,   0 }, // max 4 black pawn directions, rest 0's
    { 4, +10, +20, +11, +9 ,   0,   0,   0,   0 }, // max 4 white pawn directions, rest 0's
    { 8, -21, -12, +8 , +19, +21, +12, -8, -19 }, // 8 knight directions
    { 4, +9 , +11, -11, -9 ,   0,   0,   0,   0 }, // 4 bishop directions
    { 4, +10, -10, +1 , -1 ,   0,   0,   0,   0 }, // 4 rook directions
    { 8, +9 , +11, -11, -9 ,   +10, -10, +1, -1 }, // 8 queen directions
    { 8, +9 , +11, -11, -9 ,   +10, -10, +1, -1 }  // 8 king directions (= queen dir's)
};

// players
const bool WHITE = false;
const bool BLACK = true;

// bit positions of flags
const uint8_t COLOR_FLAG = 7;
//const uint8_t CASTLE_FLAG = 4;
//const uint8_t MOVED_FLAG = 4;

// bit index positions for castling right uint8_t
// bit is set means castling is possible
const uint8_t CASTLE_WKING_POS = 0;
const uint8_t CASTLE_WQUEEN_POS = 1;
const uint8_t CASTLE_BKING_POS = 2;
const uint8_t CASTLE_BQUEEN_POS = 3;

const QString STARTING_FEN = QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

// board positions
const uint8_t A1 = 21;
const uint8_t A2 = 31;
const uint8_t A3 = 41;
const uint8_t A4 = 51;
const uint8_t A5 = 61;
const uint8_t A6 = 71;
const uint8_t A7 = 81;
const uint8_t A8 = 91;

const uint8_t B1 = 22;
const uint8_t B2 = 32;
const uint8_t B3 = 42;
const uint8_t B4 = 52;
const uint8_t B5 = 62;
const uint8_t B6 = 72;
const uint8_t B7 = 82;
const uint8_t B8 = 92;

const uint8_t C1 = 23;
const uint8_t C2 = 33;
const uint8_t C3 = 43;
const uint8_t C4 = 53;
const uint8_t C5 = 63;
const uint8_t C6 = 73;
const uint8_t C7 = 83;
const uint8_t C8 = 93;

const uint8_t D1 = 24;
const uint8_t D2 = 34;
const uint8_t D3 = 44;
const uint8_t D4 = 54;
const uint8_t D5 = 64;
const uint8_t D6 = 74;
const uint8_t D7 = 84;
const uint8_t D8 = 94;

const uint8_t E1 = 25;
const uint8_t E2 = 35;
const uint8_t E3 = 45;
const uint8_t E4 = 55;
const uint8_t E5 = 65;
const uint8_t E6 = 75;
const uint8_t E7 = 85;
const uint8_t E8 = 95;

const uint8_t F1 = 26;
const uint8_t F2 = 36;
const uint8_t F3 = 46;
const uint8_t F4 = 56;
const uint8_t F5 = 66;
const uint8_t F6 = 76;
const uint8_t F7 = 86;
const uint8_t F8 = 96;

const uint8_t G1 = 27;
const uint8_t G2 = 37;
const uint8_t G3 = 47;
const uint8_t G4 = 57;
const uint8_t G5 = 67;
const uint8_t G6 = 77;
const uint8_t G7 = 87;
const uint8_t G8 = 97;

const uint8_t H1 = 28;
const uint8_t H2 = 38;
const uint8_t H3 = 48;
const uint8_t H4 = 58;
const uint8_t H5 = 68;
const uint8_t H6 = 78;
const uint8_t H7 = 88;
const uint8_t H8 = 98;

const uint8_t WHITE_KING = 0x06;
const uint8_t WHITE_QUEEN = 0x05;
const uint8_t WHITE_ROOK = 0x04;
const uint8_t WHITE_BISHOP = 0x03;
const uint8_t WHITE_KNIGHT = 0x02;
const uint8_t WHITE_PAWN = 0x01;

const uint8_t BLACK_KING = 0x86;
const uint8_t BLACK_QUEEN = 0x85;
const uint8_t BLACK_ROOK = 0x84;
const uint8_t BLACK_BISHOP = 0x83;
const uint8_t BLACK_KNIGHT = 0x82;
const uint8_t BLACK_PAWN = 0x81;

const QRegularExpression FEN_CASTLES_REGEX = QRegularExpression("^-|[KQABCDEFGH]{0,2}[kqabcdefgh]{0,2}$");
const QRegularExpression SAN_REGEX = QRegularExpression("^([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(=?[nbrqNBRQ])?(\\+|#)?$");

typedef QList<Move> Moves;

class Board
{

public:

    /**
     * @brief turn is either == WHITE or == BLACK
     */
    bool turn;

    /**
     * @brief halfmove_clock number of halfmoves from beginning.
     *                       automatically updated after applying a move
     */
    int halfmove_clock;

    /**
     * @brief fullmove_number
     */
    int fullmove_number;

    /**
     * @brief last_was_null set to true, if last the last move leading to
     *                      this board position was a null move
     */
    bool last_was_null;

    /**
     * @brief Board creates empty board, no castling rights
     */
    Board();

    /**
     * @brief Board creates board w/ initial position, castling rights set if called with true
     *        creates empty board board, no castling rights if called with false
     * @param initial_position triggers wether initial position or empty should be created
     */
    Board(bool initial_position);

    /**
     * @brief Board
     * @param fen_string creates board from FEN string
     */
    Board(const QString &fen_string);

    /**
     * @brief Board creates new Board copying position of the pieces of the supplied
     *              board. Parameters (i.e. undo history, move numbers etc. are _not_
     *              copied, just the position of the pieces
     * @param board The board where the position of pieces is taken from
     */
    Board(Board *board);

    /**
     *@brief destructor, clean up memory
     */
    ~Board();

    /**
     * @brief fen returns FEN string of current board
     * @return
     */
    QString fen();

    /**
     * @brief copy_and_apply applies move and returns a deep copy of current board
     *        no check of legality. always call board.is_legal(m) before applying move
     * @param m move to apply
     * @return copy of board
     */
    Board* copy_and_apply(const Move &m);

    /**
     * @brief apply applies supplied move. doesn't check for legality
     *        no check of legality. always call board.is_legal(m) before applying move
     * @param m move to apply
     */
    void apply(const Move &m);

    /**
     * @brief undo undoes the very last move. undoing can only be done once for the very
     *             last move that was applied before, i.e. apply undo apply undo is ok,
     *             but apply apply undo undo is not. throws logic error if called
     *             in wrong fashion. check with is_undo_available() when in doubt
     */
    void undo();

    /**
     * @brief pseudo_legal_moves returns move list with all pseudo-legal moves of
     *                           current position
     * @return
     */
    Moves* pseudo_legal_moves();

    /**
     * @brief pseudo_legal_moves_from returns move list with pseudo legal moves
     *                                from supplied square index
     * @param from_square_idx         square from which move originates
     *                                must be in internal board representation, i.e.
     *                                in range 21 ... 98
     * @param with_castles            include castling in returned list
     * @param turn_color              either WHITE or BLACK, i.e. the player to move
     * @return pseudo legal move list
     */
    Moves* pseudo_legal_moves_from(int from_square_idx, bool with_castles, bool turn_color);

    /**
     * @brief legal_moves returns move list of all legal moves in position
     * @return move list
     */
    Moves* legal_moves();

    /**
     * @brief legal_moves_from computes all legal moves originating in from square
     * @param from_square  move originates from this square. must be in range 21...98
     * @return move list of legal moves
     */
    Moves* legal_moves_from(int from_square);

    /**
     * @brief pseudo_is_legal_move checks whether supplied pseudo legal move is legal
     *              in current position. Does NOT check whether supplied move is pseudo legal!!!
     * @return result of checking legality
     */
    bool pseudo_is_legal_move(const Move &);

    /**
     * @brief is_legal_move checks whether the supplied move is legal in the board
     *                      position. Always call before applying a move on a board!
     * @return true, if the move is legal, otherwise false
     */
    bool is_legal_move(const Move&);

    /**
     * @brief is_legal_and_promotes checks whether supplied move is legal and is
     *                              a pawn move promoting to another piece
     * @return true only if both move is legal and pawn move and promotes. false, otherwise.
     */
    bool is_legal_and_promotes(const Move&);

    /**
     * @brief is_check checks if the player whose on the move in the current position
     *                 is in check
     * @return true, if player in check, false otherwise.
     */
    bool is_check();

    /**
     * @brief is_checkmate tests whether player who is on the move in current position
     *                     is in checkmate (i.e. is in check but has not legal move
     *                     escaping the check)
     * @return true, if player in checkmate, false otherwise.
     */
    bool is_checkmate();

    /**
     * @brief is_stalemate tests whether player who is on the move in current position
     *                     is in stalemate (i.e. is not in check but all legal moves
     *                     would result in check)
     * @return true, if position is stalemate, false otherwise.
     */
    bool is_stalemate();

    /**
     * @brief san computes the standard algebraic notation for the supplied move
     *        given the current position. the supplied move MUST be legal on this
     *        board
     * @param m Move to get the san for
     * @return string containing san representation of move (no move number!)
     */
    QString san(const Move &m);

    /**
     * @brief parse_san Given board position and san string, parses the san string
     *        and computes a move for it. Throws std::invalid_argument if the
     *        supplied san string cannot be parsed successfully (i.e. illegal move,
     *        illegal formatted string etc.)
     * @param s string containing a san representation of a move (no move number!)
     * @return move object (if parsed successfully)
     */
    Move parse_san(QString s);

    /**
     * @brief movePromotes checks if the supplied move (ignoring the promotion value stored
     *                     in the move is a pawn move to the 8th / 1st rank, i.e. promoting)
     * @param m move of concern
     * @return true if move promotes, false otherwise
     */
    bool movePromotes(const Move&m);

    /**
     * @brief is_initial_position checks whether the current placement of the pieces
     *                            corresponds to the inital chess position.
     * @return true if initial position, false otherwise.
     */
    bool is_initial_position();

    /**
     * @brief can_castle_wking checks whether the castling rights for the
     *                         position are such that the White king is allowed
     *                         to castle kingside in this position.
     * @return true if king may castle, false otherwise.
     */
    bool can_castle_wking();

    /**
     * @brief can_castle_bking see can_castle_wking
     * @return
     */
    bool can_castle_bking();

    /**
     * @brief can_castle_wqueen see can_castle_wking
     * @return
     */
    bool can_castle_wqueen();

    /**
     * @brief can_castle_bqueen see can_castle_wking
     * @return
     */
    bool can_castle_bqueen();

    /**
     * @brief is_undo_available checks whether the current board position
     *                          has enough information to apply the undo()
     *                          operation, i.e. take back the last move
     *                          and return to the previous board state.
     * @return true if info is available (i.e. undo() may be called), false otherwise
     */
    bool is_undo_available();

    /**
     * @brief set_castle_wking set/unset the right of the white king to castle
     *                         kingside.
     * @param can_do true to allow white to castle, false otherwise.
     */
    void set_castle_wking(bool can_do);

    /**
     * @brief set_castle_bking see set_castle_wking
     * @param can_do
     */
    void set_castle_bking(bool can_do);

    /**
     * @brief set_castle_wqueen see set_castle_wking
     * @param can_do
     */
    void set_castle_wqueen(bool can_do);

    /**
     * @brief set_castle_bqueen see set_castle_wking
     * @param can_do
     */
    void set_castle_bqueen(bool can_do);

    /**
     * @brief set_piece_at sets a piece a the supplied board position (x,y)
     * @param x int in the range (0,7) representing the column (i.e. a - h)
     * @param y int in the range (0,7) representing the row (i.e. 0 - 7)
     * @param piece the piece type (constants such as WHITE_KING, BLACK_QUEEN, EMPTY etc.)
     */
    void set_piece_at(int x, int y, uint8_t piece);

    /**
     * @brief get_piece_at gets piece a the supplied board position (x,y)
     * @param x int in the range (0,7) representing the column (i.e. a - h)
     * @param y int in the range (0,7) representing the row (i.e. 0 - 7)
     * @return piece type encoded as uint8_t (i.e. BLACK_QUEEN or EMPTY)
     */
    uint8_t get_piece_at(int x, int y);

    /**
     * @brief get_piece_type_at get the piece type as uint8_t at supplied
     *                          position. piece type is always the piece
     *                          encoded as if it were are white piece (see
     *                          KING, QUEEN, or empty).
     * @param x int in the range (0,7) representing the column (i.e. a - h)
     * @param y int in the range (0,7) representing the column (i.e. a - h)
     * @return piece encoding
     */
    uint8_t get_piece_type_at(int x, int y);

    /**
     * @brief get_piece_color_at returns color (i.e. WHITE or BLACK)
     *                           at supplied position. Don't call if there is
     *                           is no piece at the position! (check with
     *                           piece_type first)
     * @param x column
     * @param y row
     * @return piece color
     */
    bool get_piece_color_at(int x, int y);

    /**
     * @brief piece_color same as get_piece_color_at but uses here
     *                    the internal position encoding to specify the
     *                    field of the board. see this header file
     * @param idx internal board encoding specifying field position
     * @return piece color
     */
    bool piece_color(uint8_t idx);

    /**
     * @brief piece_at returns the piece at idx in internal board format
     *                    i.e. WHITE_PAWN = 0x01, BLACK_PAWN etc.
     * @param idx internal board encoding specifying field position
     * @return piece in internal format (i.e. 0x00 ... 0x06 and 0x81 ... 0x86
     */
    uint8_t piece_at(uint8_t idx);

    /**
     * @brief piece_type see piece_color and get_piece_type_at()
     * @param idx
     * @return
     */
    uint8_t piece_type(uint8_t idx);

    /**
     * @brief is_consistent rudimentary check of position consistency
     * @return true if the conditions below are true, false otherwise
     *
     * NOTE: this doesn't capture _all_ invalid positions, but
     *       the most common reasons
     * there exists one white and one black king [ok]
     * kings are >= 1 field apart                [ok]
     * side not to move is not in check          [ok]
     * side to move has less than three attackers who give check
     * if side to move is in check w/ two attackers:
     *     following must not hold:
     *         pawn+(pawn, bishop, knight), bishop+bishop, knight+knight
     * each side has less than 8 pawns          [ok]
     * no pawns in first or last row            [ok]
     * extra pieces = Math.max(0, num_queens-1) + Math.max(0, num_rooks-2)...
     *             and extra_pieces <= (8-num_pawns))
     * no more than 5 pawns in a or h line
     * checks consistency of castling rights. if set, then verify w/
     * is_black_castle_right_lost() and is_white_castle_lost()
     */
    bool is_consistent();

    /**
     * @brief is_black_king_castle_right_lost does not return castling
     *                  rights of current position (for that call can_castle_bking() etc. )
     *                  instead checks whether black king and rook are in initial position
     *                  or have moved
     * @return false, if black king or rook have moved from inital pos, true otherwise.
     */
    bool is_black_king_castle_right_lost();

    /**
     * @brief is_black_queen_castle_right_lost see is_black_king_castle_right_lost()
     * @return
     */
    bool is_black_queen_castle_right_lost();

    /**
     * @brief is_white_king_castle_right_lost see is_black_king_castle_right_lost()
     * @return
     */
    bool is_white_king_castle_right_lost();

    /**
     * @brief is_white_queen_castle_right_lost see is_black_king_castle_right_lost()
     * @return
     */
    bool is_white_queen_castle_right_lost();

    uint8_t get_ep_target();

    bool can_claim_fifty_moves();
    bool is_threefold_repetition();

    quint64 zobrist();

private:

    /**
     * @brief init_pos
     * is the inital board position
     * for encodings and the general approach
     * see "First Steps in Computer Chess Programming"
     * BYTE Magazine, October 1978
     */
    static const uint8_t init_pos [120];
    /**
     * @brief board stores the current position
     * essentially linearized 10x12 array
     */
    uint8_t board[120];
    /**
     * @brief old_board stores the previous position for undo()
     */
    uint8_t old_board[120];

    /**
     * @brief turn is either WHITE or BLACK
     */
    bool undo_available;

    /**
     * @brief castling_rights stores the castling rights
     * by using bit positions within byte. Bit positions are
     * CASTLE_WKING_POS, CASTLE_WQUEEN_POS, CASTLE_BKING_POS
     * and CASTLE_BQUEEN_POS
     */
    uint8_t castling_rights;
    uint8_t prev_castling_rights;

    uint8_t en_passent_target;
    uint8_t prev_en_passent_target;

    int prev_halfmove_clock;

    bool is_empty(uint8_t idx);
    bool is_offside(uint8_t idx);
    bool is_white_at(uint8_t idx);
    bool is_attacked(int idx, bool attacker_color);
    bool castles_wking(const Move &m);
    bool castles_bking(const Move &m);
    bool castles_wqueen(const Move &m);
    bool castles_bqueen(const Move &m);
    uint8_t piece_from_symbol(QChar c);
    QChar piece_to_symbol(uint8_t idx);
    QString idx_to_str(int idx);
    uint8_t alpha_to_pos(QChar alpha);

    QMap<quint64, int> *transpositionTable;

    int zobrist_piece_type(uint8_t piece);

    void update_transposition_table();

    friend std::ostream& operator<<(std::ostream& strm, const Board &b);

};

}
#endif // BOARD_H
