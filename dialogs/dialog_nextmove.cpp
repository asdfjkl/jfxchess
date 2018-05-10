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


#include <QKeyEvent>
#include <QDialogButtonBox>
#include <QGridLayout>
#include <QSizePolicy>
#include "dialog_nextmove.h"
#include "chess/move.h"
#include "chess/game_node.h"


DialogNextMove::DialogNextMove(chess::GameNode *node, QWidget *parent) :
    QDialog(parent)
{
    this->setWindowTitle(this->tr("Next Move"));
    this->moveList = new QListWidget();
    this->selectedIndex = 0;
    int cnt_items = node->getVariations().count();
    chess::Board b = node->getBoard();
    for(int i=0;i<cnt_items;i++) {
        chess::Move mi = node->getVariation(i)->getMove();
        QString san_i = b.san(mi);
        this->moveList->addItem(san_i);
    }

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);
    QGridLayout *layout = new QGridLayout();
    layout->addWidget(this->moveList,0,1);
    layout->addWidget(buttonBox, 3, 0, 1, 3);
    this->setLayout(layout);
    this->moveList->item(0)->setSelected(true);

    /* adjusting the dialog size (resp. the listwidget size
     * to a reasonable minimum size seems to be not trivial
     * leave for now default size
     * #todo improve size adjustment
     */
    //int optimal_height = this->moveList->sizeHintForRow(0) *
    //        this->moveList->count() + 2 * this->moveList->frameWidth();
    //int optimal_width = this->moveList->sizeHintForColumn(0) + 2 * this->moveList->frameWidth();
    //this->moveList->setMinimumHeight(optimal_height);
    //this->moveList->setFixedSize(optimal_width, optimal_height);
    //this->moveList->setFixedHeight(optimal_height);
    //this->moveList->setSizePolicy(QSizePolicy::Minimum);
    //this->setMinimumWidth(this->moveList->size().width());
    //this->setMaximumWidth(this->moveList->size().height());

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogNextMove::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogNextMove::reject);
    connect(this->moveList, &QListWidget::itemDoubleClicked, this, &DialogNextMove::accept);
    connect(this->moveList, &QListWidget::currentItemChanged, this, &DialogNextMove::onItemChanged);
    connect(this, &DialogNextMove::okSelected, this, &DialogNextMove::accept);
    connect(this, &DialogNextMove::cancelSelected, this, &DialogNextMove::reject);

/*
    self.connect(buttonBox, SIGNAL("accepted()"),
             self, SLOT("accept()"))
    self.connect(buttonBox, SIGNAL("rejected()"),
             self, SLOT("reject()"))

    self.connect(self,SIGNAL("rightclick()"), SLOT("accept()") )
    self.connect(self,SIGNAL("leftclick()"), SLOT("reject()") )

    self.listWidget.itemDoubleClicked.connect(self.accept)
    self.listWidget.currentItemChanged.connect(self.on_item_changed)
*/
}

void DialogNextMove::onItemChanged() {
    this->selectedIndex = this->moveList->currentRow();
}

void DialogNextMove::keyPressEvent(QKeyEvent *e) {
    int key = e->key();
    if(key == Qt::Key_Left || key == Qt::Key_Escape) {
        emit(cancelSelected());
    } else if(key == Qt::Key_Right || key == Qt::Key_Return) {
        emit(okSelected());
    }

}
