#include "tab_board_style.h"
#include <QDialogButtonBox>
#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>


TabBoardStyle::TabBoardStyle(ColorStyle *currentStyle, QWidget *parent)
    : QWidget(parent)
{

    //this->setWindowTitle(this->tr("Set Options"));

    //QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);
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

    /*
    QGroupBox *groupBox = new QGroupBox(tr("Exclusive Radio Buttons"));

    QRadioButton *radio1 = new QRadioButton(tr("&Radio button 1"));
    QRadioButton *radio2 = new QRadioButton(tr("R&adio button 2"));
    QRadioButton *radio3 = new QRadioButton(tr("Ra&dio button 3"));

    radio1->setChecked(true);
    QVBoxLayout *vboxRadioButtons = new QVBoxLayout;
    vboxRadioButtons->addWidget(radio1);
    vboxRadioButtons->addWidget(radio2);
    vboxRadioButtons->addWidget(radio3);
    vboxRadioButtons->addStretch(1);
    groupBox->setLayout(vboxRadioButtons);
*/

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
    //layout->addWidget(buttonBox);

    connect(pcs_uscf, &QRadioButton::toggled, this, &TabBoardStyle::onUSCFPieces);
    connect(pcs_old, &QRadioButton::toggled, this, &TabBoardStyle::onOldPieces);
    connect(pcs_merida, &QRadioButton::toggled, this, &TabBoardStyle::onMeridaPieces);

    connect(brd_blue, &QRadioButton::toggled, this, &TabBoardStyle::onBlueColor);
    connect(brd_green, &QRadioButton::toggled, this, &TabBoardStyle::onGreenColor);
    connect(brd_brown, &QRadioButton::toggled, this, &TabBoardStyle::onBrownColor);

    connect(brd_blue_marbles, &QRadioButton::toggled, this, &TabBoardStyle::onBlueMarbles);
    connect(brd_green_marbles, &QRadioButton::toggled, this, &TabBoardStyle::onGreenMarbles);
    connect(brd_wood, &QRadioButton::toggled, this, &TabBoardStyle::onWood);

    this->setLayout(layout);
    //this->update();

}

void TabBoardStyle::TabBoardStyle::onMeridaPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_MERIDA);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onUSCFPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_USCF);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onOldPieces() {
    this->displayBoard->setPieceType(PIECE_STYLE_OLD);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onBlueColor() {
    this->displayBoard->setBoardColors(BORDER_BLUE, DARK_SQUARE_BLUE,
                                       LIGHT_SQUARE_BLUE, COORDINATE_COLOR_BLUE, STYLE_BLUE);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onGreenColor() {
    this->displayBoard->setBoardColors(BORDER_GREEN, DARK_SQUARE_GREEN,
                                       LIGHT_SQUARE_GREEN, COORDINATE_COLOR_GREEN, STYLE_GREEN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onBrownColor() {
    this->displayBoard->setBoardColors(BORDER_BROWN, DARK_SQUARE_BROWN,
                                       LIGHT_SQUARE_BROWN, COORDINATE_COLOR_BROWN, STYLE_BROWN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_COLOR);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onWood() {
    this->displayBoard->setBoardColors(BORDER_WOOD, QPixmap(DARK_SQUARE_WOOD),
                                       QPixmap(LIGHT_SQUARE_WOOD), COORDINATE_COLOR_WOOD, STYLE_WOOD);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onBlueMarbles() {
    this->displayBoard->setBoardColors(BORDER_MARLBE_BLUE, QPixmap(DARK_SQUARE_MARBLE_BLUE),
                                       QPixmap(LIGHT_SQUARE_MARBLE_BLUE), COORDINATE_COLOR_MARBLE_BLUE, STYLE_MARBLE_BLUE);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}

void TabBoardStyle::TabBoardStyle::onGreenMarbles() {
    this->displayBoard->setBoardColors(BORDER_MARBLE_GREEN, QPixmap(DARK_SQUARE_MARBLE_GREEN),
                                       QPixmap(LIGHT_SQUARE_MARBLE_GREEN), COORDINATE_COLOR_MARBLE_GREEN, STYLE_MARBLE_GREEN);
    this->displayBoard->setBoardStyle(BOARD_STYLE_TEXTURE);
    this->displayBoard->update();
}

