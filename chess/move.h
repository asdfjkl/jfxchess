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


#ifndef MOVE_H
#define MOVE_H

#include <QString>
#include <tuple>
#include <QPoint>


namespace chess {

// general piece encodings
const uint8_t EMPTY = 0;
const uint8_t PAWN = 1;
const uint8_t KNIGHT = 2;
const uint8_t BISHOP = 3;
const uint8_t ROOK = 4;
const uint8_t QUEEN = 5;
const uint8_t KING = 6;


class Move
{

public:

    uint8_t from;
    uint8_t to;
    uint8_t promotion_piece;
    QString uci_string;
    bool is_null;

    /**
     * @brief Move creates a null move
     */
    Move();
    /**
     * @brief Move creates move, supplied parameters in internal
     *             board coordinate format, i.e. in range 21...98.
     *             Does not check any validity
     * @param from index of source field
     * @param to index of target field
     */
    Move(uint8_t from, uint8_t to);

    /**
     * @brief Move creates a move where the piece transforms
     *             into promotion piece upon placement on target square.
     *             Does not check any validity.
     *             supplied parameters in internal
     *             board coordinate format, i.e. in range 21...98.
     * @param from index of source field
     * @param to   index of target field
     * @param promotion_piece piece type of promotion piece. doesn't encode color, i.e.
     *              must be in range (>= 0, <= 5), cf. general piece encodings above
     */
    Move(uint8_t from, uint8_t to, uint8_t promotion_piece);

    /**
     * @brief Move creates a move with en passent capture
     *             Does not check any validity (i.e. whether
     *             en passent is possible, source is a pawn, move is legal etc.)
     *             supplied parameters in internal
     *             board coordinate format, i.e. in range 21...98.     * @param from
     * @param to
     * @param en_passent
     */
    Move(uint8_t from, uint8_t to, bool en_passent);

    /**
     * @brief Move creates move from uci string (e.g. g1f3, d7d8Q etc.)
     * @param uci supplied uci string
     */
    Move(QString uci);

    /**
     * @brief Move creates a deep copy of supplied move
     * @param m the move to copy
     */
    Move(const Move& m);

    /**
     * @brief uci get uci string (e.g. g1f3, d7d8Q etc.) of current move
     * @return uci string
     */
    QString uci();

    /**
     * @brief operator == compares two moves by checking whether they
     *                    are semantically the same, i.e. same source square,
     *                    same target, same promotion, both (or both not) en passent.
     * @param other
     * @return true if moves are semantically same (does not care about memory)
     */
    bool operator==(const Move &other) const;

    /**
     * @brief operator !=  see ==
     * @param other
     * @return
     */
    bool operator!=(const Move &other) const;

    QPoint fromAsXY();
    QPoint toAsXY();

private:

    uint8_t alpha_to_pos(QChar alpha);
    std::tuple<char, uint8_t> get_alphanum_coord(uint8_t internal_pos);
    friend std::ostream& operator<<(std::ostream& strm, const Move &m);

};

}
#endif // MOVE_H
