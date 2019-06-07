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


#include "dialog_guioptions.h"
#include "viewController/colorstyle.h"
#include <QDialogButtonBox>
#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>
#include <QDebug>
#include "various/resource_finder.h"

DialogGuiOptions::DialogGuiOptions(ColorStyle *currentStyle, QWidget *parent) :
    QDialog(parent) {
    this->setWindowTitle(this->tr("Set Options"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);
    this->displayBoard = new PickColorBoard;
    this->displayBoard->setColorStyle(currentStyle);

    QGroupBox *groupBoxPieces = new QGroupBox(this->tr("Piece Style"));
    QGroupBox *groupBoxBoard = new QGroupBox(this->tr("Board Style"));

    QRadioButton *pcs_merida = new QRadioButton(tr("Merida (Default)"));
    QRadioButton *pcs_old = new QRadioButton(tr("Old Style"));
    QRadioButton *pcs_uscf = new QRadioButton(tr("USCF"));

    QRadioButton *brd_blue = new QRadioButton(tr("Blue (Default)"));
    QRadioButton *brd_green = new QRadioButton(tr("Green"));
    QRadioButton *brd_brown = new QRadioButton(tr("Brown"));

    QRadioButton *brd_wood = new QRadioButton(tr("Wood"));
    QRadioButton *brd_blue_marbles = new QRadioButton(tr("Blue Marbles"));
    QRadioButton *brd_green_marbles = new QRadioButton(tr("Green Marbles"));

    switch(currentStyle->pieceType) {
    case PIECE_STYLE_OLD:
        pcs_old->setChecked(true);
        break;
    case PIECE_STYLE_USCF:
        pcs_uscf->setChecked(true);
        break;
    default:
        pcs_merida->setChecked(true);
    }

    if(currentStyle->boardStyle == BOARD_STYLE_TEXTURE) {
        if(currentStyle->borderColor == BORDER_WOOD) {
            brd_wood->setChecked(true);
        }
        if(currentStyle->borderColor == BORDER_MARBLE_GREEN) {
            brd_green_marbles->setChecked(true);
        }
        if(currentStyle->borderColor == BORDER_MARLBE_BLUE) {
            brd_blue_marbles->setChecked(true);
        }
    } else {
        if(currentStyle->borderColor == BORDER_BLUE) {
            brd_blue->setChecked(true);
        }
        if(currentStyle->borderColor == BORDER_GREEN) {
            brd_green->setChecked(true);
        }
        if(currentStyle->borderColor == BORDER_BROWN) {
            brd_brown->setChecked(true);
        }
    }



    QVBoxLayout *vboxPcs = new QVBoxLayout();
    vboxPcs->addWidget(pcs_merida);
    vboxPcs->addWidget(pcs_old);
    vboxPcs->addWidget(pcs_uscf);
    //vboxPcs->addStretch(1);
    groupBoxPieces->setLayout(vboxPcs);

    QVBoxLayout *vboxBrd = new QVBoxLayout();
    vboxBrd->addWidget(brd_blue);
    vboxBrd->addWidget(brd_green);
    vboxBrd->addWidget(brd_brown);
    vboxBrd->addWidget(brd_wood);
    vboxBrd->addWidget(brd_blue_marbles);
    vboxBrd->addWidget(brd_green_marbles);
    vboxBrd->addStretch(1);
    groupBoxBoard->setLayout(vboxBrd);

    QVBoxLayout *right = new QVBoxLayout();
    right->addWidget(groupBoxPieces);
    right->addWidget(groupBoxBoard);
    right->addStretch();

    QHBoxLayout *main = new QHBoxLayout();
    main->addWidget(this->displayBoard);
    main->addLayout(right);

    QVBoxLayout *layout = new QVBoxLayout();
    layout->addLayout(main);
    layout->addWidget(buttonBox);
    this->setLayout(layout);
    this->update();

    int h = parent->height();
    this->resize(h*0.7, 1);


    connect(pcs_uscf, &QRadioButton::toggled, this, &DialogGuiOptions::onUSCFPieces);
    connect(pcs_old, &QRadioButton::toggled, this, &DialogGuiOptions::onOldPieces);
    connect(pcs_merida, &QRadioButton::toggled, this, &DialogGuiOptions::onMeridaPieces);

    connect(brd_blue, &QRadioButton::toggled, this, &DialogGuiOptions::onBlueColor);
    connect(brd_green, &QRadioButton::toggled, this, &DialogGuiOptions::onGreenColor);
    connect(brd_brown, &QRadioButton::toggled, this, &DialogGuiOptions::onBrownColor);

    connect(brd_blue_marbles, &QRadioButton::toggled, this, &DialogGuiOptions::onBlueMarbles);
    connect(brd_green_marbles, &QRadioButton::toggled, this, &DialogGuiOptions::onGreenMarbles);
    connect(brd_wood, &QRadioButton::toggled, this, &DialogGuiOptions::onWood);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogGuiOptions::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogGuiOptions::reject);
}


void DialogGuiOptions::onMeridaPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_MERIDA);
    this->displayBoard->update();
}

void DialogGuiOptions::onUSCFPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_USCF);
    this->displayBoard->update();
}

void DialogGuiOptions::onOldPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_OLD);
    this->displayBoard->update();
}

void DialogGuiOptions::onBlueColor() {
    this->displayBoard->setBoardColors(BORDER_BLUE, DARK_SQUARE_BLUE,
                                       LIGHT_SQUARE_BLUE, COORDINATE_COLOR_BLUE, STYLE_BLUE);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void DialogGuiOptions::onGreenColor() {
    this->displayBoard->setBoardColors(BORDER_GREEN, DARK_SQUARE_GREEN,
                                       LIGHT_SQUARE_GREEN, COORDINATE_COLOR_GREEN, STYLE_GREEN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void DialogGuiOptions::onBrownColor() {
    this->displayBoard->setBoardColors(BORDER_BROWN, DARK_SQUARE_BROWN,
                                       LIGHT_SQUARE_BROWN, COORDINATE_COLOR_BROWN, STYLE_BROWN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void DialogGuiOptions::onWood() {
    this->displayBoard->setBoardColors(BORDER_WOOD, QPixmap(DARK_SQUARE_WOOD),
                                       QPixmap(LIGHT_SQUARE_WOOD), COORDINATE_COLOR_WOOD, STYLE_WOOD);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}

void DialogGuiOptions::onBlueMarbles() {
    this->displayBoard->setBoardColors(BORDER_MARLBE_BLUE, QPixmap(DARK_SQUARE_MARBLE_BLUE),
                                       QPixmap(LIGHT_SQUARE_MARBLE_BLUE), COORDINATE_COLOR_MARBLE_BLUE, STYLE_MARBLE_BLUE);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}

void DialogGuiOptions::onGreenMarbles() {
    this->displayBoard->setBoardColors(BORDER_MARBLE_GREEN, QPixmap(DARK_SQUARE_MARBLE_GREEN),
                                       QPixmap(LIGHT_SQUARE_MARBLE_GREEN), COORDINATE_COLOR_MARBLE_GREEN, STYLE_MARBLE_GREEN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}
