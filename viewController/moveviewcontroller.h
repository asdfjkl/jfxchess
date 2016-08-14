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


#ifndef MOVEVIEWCONTROLLER_H
#define MOVEVIEWCONTROLLER_H

#include <QTextEdit>
#include <QTextBrowser>
#include <QTextDocument>
#include <QKeyEvent>
#include "model/game_model.h"
#include "chess/gui_printer.h"

class MoveViewController : public QTextBrowser
{
    Q_OBJECT
public:
    explicit MoveViewController(GameModel *gameModel, QWidget *parent = 0);

private:
    QPoint *pos_rightclick;
    GameModel *gameModel;
    QTextDocument *document;
    chess::GuiPrinter *guiPrinter;
    void selectAndMarkAnchor(const QString &link);

protected:
    void setSource(const QUrl&);
    void keyPressEvent(QKeyEvent *e);

signals:

public slots:
    void onStateChange();
    void onAnchorClicked(const QUrl &link);
    void onForwardClick();
    void onBackwardClick();
    void showContextMenu(const QPoint &pos);
    void annotation(int nag, int min, int max);
    void removeAnnotations();
    void addComment();
    void deleteComment();
    void variantUp();
    void variantDown();
    void deleteVariant();
    void deleteFromHere();
    void removeAllComments();
    void removeAllVariants();
    void onScrollBack();
    void onScrollForward();
    void onSeekToBeginning();
    void onSeekToEnd();
    chess::GameNode* findNodeOnRightclick();

};

#endif // MOVEVIEWCONTROLLER_H
