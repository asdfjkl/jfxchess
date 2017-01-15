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


#ifndef GAME_NODE_H
#define GAME_NODE_H

#include "board.h"
#include "move.h"
#include <QtGui/QColor>
#include <QPoint>

namespace chess {

struct Arrow {
    QPoint from;
    QPoint to;
    QColor color;
};

struct ColoredField {
    QPoint field;
    QColor color;
};

class GameNode
{

public:

    GameNode();

    /**
     * @brief The destructor does NOT delete child nodes. You
     *        are responsible yourself for deleting child nodes.
     *        In general, member functions from Game() to manage
     *        the tree should be used.
     */
    ~GameNode();

    /**
     * @brief getId each game node is assigned a unique id
     *        automatically during construction.
     * @return the unique id of this node
     */
    int getId();

    /**
     * @brief getBoard
     * @return Board of current node
     */
    Board* getBoard();

    /**
     * @brief setBoard deletes the old board of this node, and sets
     *                 the supplied board as the new one. Does no
     *                 validity checks of the board position
     * @param b The board. Must not be null.
     */
    void setBoard(Board *b);

    /**
     * @brief getSan returns san string of move that
     *               lead to this node.
     * @return san string or null for move node.
     */
    QString getSan();

    /**
     * @brief root returns root node of the game
     * @return the root node
     */
    GameNode* root();

    /**
     * @brief getParent returns the parent of the node. null
     *                  if there is no parent (e.g. root node or
     *                  freshly created)
     * @return parent node or null
     */
    GameNode* getParent();

    /**
     * @brief getMove returns move leading to this node. Null
     *                if there is no move (e.g. root node)
     * @return pointer to Move or null.
     */
    Move* getMove();

    /**
     * @brief setMove set the move that leads to this
     *                game node to m. There is no validity
     *                or consistency check.
     * @param m Pointer to the move.
     */
    void setMove(Move *m);

    /**
     * @brief setParent Set the parent to the supplied Game Node.
     *                  No validity / consistency checks. Old parent
     *                  is not deleted.
     * @param g Pointer to the (new) parent node.
     */
    void setParent(GameNode *g);

    /**
     * @brief setComment Set comment for this node to supplied textstring.
     * @param c The comment.
     */
    void setComment(QString &c);

    /**
     * @brief getComment returns the comment for this node. Empty text string
     *                   if there is no comment.
     * @return The comment.
     */
    QString getComment();

    /**
     * @brief getVariation get the variation (i.e. the child) at index i
     * @param i index position of variation. MUST be a legal index.
     * @return the game node at position i
     */
    GameNode* getVariation(int i);

    /**
     * @brief getVariations returns list of all child nodes, i.e.
     *                      all variations starting in this position.
     * @return list with all child nodes.
     */
    QList<GameNode*>* getVariations();

    /**
     * @brief addVariation adds a new variation by putting the supplied
     *                     game node at the end of the list of all variations.
     *                     sets parent of g to this node. Does NOT check whether
     *                     the variation already exists.
     * @param g the (new) child node.
     */
    void addVariation(GameNode *g);

    /**
     * @brief hasVariations checks whether the node as variations, i.e. more
     *                      than one (mainline) variations
     * @return true if there are at least two (mainline + x) childs, false otherwise.
     */
    bool hasVariations();

    /**
     * @brief isLeaf checks whether node is leaf.
     * @return true if node has no children, false otherwise.
     */
    bool isLeaf();

    /**
     * @brief addNag add numeric annotation glyph (see PGN standard)
     * @param n NAG code
     */
    void addNag(int n);

    /**
     * @brief getNags returns all numeric annotation glyphs (see PGN standard)
     * @return list with all NAGs
     */
    QList<int> *getNags();

    /**
     * @brief getArrows returns a list with all arrows for this node.
     *        Arrows are just annotations done by the user for illustrations.
     * @return list of arrows
     */
    QList<Arrow*>* getArrows();

    /**
     * @brief getColoredFields returns list of colored fields. Such fields
     *        are juts highlighted fields done by the user for illustration.
     * @return list of color fields
     */
    QList<ColoredField*> *getColoredFields();

    /**
     * @brief addOrDelArrow adds (if the supplied arrow does not exist) or removes
     *             (if the node has that arrow already) and arrow from the board
     * @param a the arrow that is supposed to be deleted or added
     */
    void addOrDelArrow(Arrow *a);

    /**
     * @brief addOrDelColoredField deletes color (field is already highlighted) or
     *               colorizes (field is plain) a board field.
     * @param c the colored field that is supposed to be deleted or added
     */
    void addOrDelColoredField(ColoredField *c);

    int getDepth();

    bool userWasInformedAboutResult;

protected:
    static int initId() { return id++; }

private:
    QList<Arrow*> *arrows;
    QList<ColoredField*> *coloredFields;
    QString san_cache;
    static int id;
    int nodeId;
    Move* m;
    QList<GameNode*> *variations;
    QList<int> *nags;
    Board* board;
    QString comment;
    GameNode* parent;
    int depthCache;

};

}

#endif // GAME_NODE_H
