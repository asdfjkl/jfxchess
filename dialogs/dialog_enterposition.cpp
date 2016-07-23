#include "dialog_enterposition.h"
#include <QPainter>
#include <QDebug>
#include <assert.h>
#include <QDesktopWidget>
#include <QApplication>
#include <QCheckBox>
#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>

DialogEnterPosition::DialogEnterPosition(chess::Board *board, ColorStyle *style,
                                         QWidget *parent) :
    QDialog(parent)
{
    this->resizeTo(0.8);
    this->setWindowTitle(tr("Enter Position"));

    this->sbv = new EnterPosBoard(style, board, parent);


         this->cbWhiteShort = new QCheckBox(tr("White O-O"));
         this->cbWhiteLong = new QCheckBox(tr("White O-O-O"));
         this->cbBlackShort = new QCheckBox(tr("Black O-O"));
         this->cbBlackLong = new QCheckBox(tr("Black O-O-O"));
         QGroupBox *grpBox_castle = new QGroupBox(tr("Castling Rights"));
         QVBoxLayout *vbox_castle = new QVBoxLayout();
         vbox_castle->addWidget(this->cbWhiteShort);
         vbox_castle->addWidget(this->cbWhiteLong);
         vbox_castle->addWidget(this->cbBlackShort);
         vbox_castle->addWidget(this->cbBlackLong);
         vbox_castle->addStretch(1);
         grpBox_castle->setLayout(vbox_castle);

         this->rbWhite = new QRadioButton(tr("White To Move"));
         this->rbBlack = new QRadioButton(tr("Black To Move"));
         QGroupBox *grpBox_turn = new QGroupBox(tr("Turn"));
         QVBoxLayout *vbox_radio = new QVBoxLayout();
         vbox_radio->addWidget(this->rbWhite);
         vbox_radio->addWidget(this->rbBlack);
         vbox_radio->addStretch(1);
         grpBox_turn->setLayout(vbox_radio);

         this->buttonInit = new QPushButton(tr("Initial Position"));
         this->buttonClear = new QPushButton(tr("Clear Board"));
         this->buttonCurrent = new QPushButton(tr("Current Position"));

         QVBoxLayout *vbox_config = new QVBoxLayout();
         vbox_config->addWidget(grpBox_castle);
         vbox_config->addWidget(grpBox_turn);
         vbox_config->addStretch(1);
         vbox_config->addWidget(this->buttonInit);
         vbox_config->addWidget(this->buttonClear);
         vbox_config->addWidget(this->buttonCurrent);

         QHBoxLayout *hbox = new QHBoxLayout();
         hbox->addWidget(this->sbv);
         hbox->addLayout(vbox_config);

         QVBoxLayout *vbox = new QVBoxLayout();
         vbox->addLayout(hbox);
         this->buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);
         vbox->addWidget(this->buttonBox);

         this->setLayout(vbox);

         connect(this->buttonBox, &QDialogButtonBox::accepted, this, &QDialog::accept);
         connect(this->buttonBox, &QDialogButtonBox::rejected, this, &QDialog::reject);

         connect(this->buttonInit, &QPushButton::clicked, this, &DialogEnterPosition::setToInitialPosition);
         connect(this->buttonClear, &QPushButton::clicked, this, &DialogEnterPosition::clearBoard);
         connect(this->buttonCurrent, &QPushButton::clicked, this, &DialogEnterPosition::setToCurrentBoard);

         connect(this->sbv, &EnterPosBoard::squareChanged, this, &DialogEnterPosition::checkConsistency);

         connect(this->cbBlackLong, &QCheckBox::toggled, this, &DialogEnterPosition::setCastlingRights);
         connect(this->cbBlackShort, &QCheckBox::toggled, this, &DialogEnterPosition::setCastlingRights);
         connect(this->cbWhiteLong, &QCheckBox::toggled, this, &DialogEnterPosition::setCastlingRights);
         connect(this->cbWhiteShort, &QCheckBox::toggled, this, &DialogEnterPosition::setCastlingRights);

         connect(this->rbWhite, &QRadioButton::toggled, this, &DialogEnterPosition::setTurn);
         connect(this->rbBlack, &QRadioButton::toggled, this, &DialogEnterPosition::setTurn);

         this->rbWhite->toggle();
         this->setTurn();
/*
         self.connect(self.buttonBox, SIGNAL("accepted()"),
                  self, SLOT("accept()"))
         self.connect(self.buttonBox, SIGNAL("rejected()"),
                  self, SLOT("reject()"))

         self.cbWhiteShort.toggled.connect(self.set_castling_rights)
         self.cbWhiteLong.toggled.connect(self.set_castling_rights)
         self.cbBlackShort.toggled.connect(self.set_castling_rights)
         self.cbBlackLong.toggled.connect(self.set_castling_rights)

         self.rbWhite.toggle()
         self.rbWhite.toggled.connect(self.set_turn)
         self.rbBlack.toggled.connect(self.set_turn)

         self.buttonInit.clicked.connect(self.initial_position)
         self.buttonClear.clicked.connect(self.clear_board)
         self.buttonCurrent.clicked.connect(self.set_current)

         # reset who's current turn it is and the current
         # castling rights of the position
         self.set_castling_rights()
         self.set_turn()
*/
}

void DialogEnterPosition::resizeTo(float ratio) {

    int height = 0;
    int width = 0;
    if(this->parentWidget() != 0) {
        int w_height = this->parentWidget()->size().height();
        height = w_height * ratio;
        //width = (w_width * ratio);
        width = height * 1.1;
    } else {
        QDesktopWidget *desktop = qApp->desktop();
        QSize availableSize = desktop->availableGeometry().size();
        int w_height = availableSize.height();
        height = w_height * (ratio*0.6);
        //width = w_width * (ratio*0.6);
        width = height * 1.1;
    }
    QSize newSize( width, height );
    this->resize(newSize);
}

void DialogEnterPosition::checkConsistency() {
    if(this->sbv->getCurrentBoard()->is_consistent()) {
        this->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(true);
    } else {
        this->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(false);
    }
}

void DialogEnterPosition::setToInitialPosition() {
    this->sbv->setToInitialPosition();
    this->checkConsistency();
}

void DialogEnterPosition::clearBoard() {
    this->sbv->clearBoard();
    this->checkConsistency();
}

void DialogEnterPosition::setToCurrentBoard() {
    this->sbv->setToCurrentBoard();
    this->checkConsistency();
}

void DialogEnterPosition::setTurn() {
    if(this->rbWhite->isChecked()) {
        this->sbv->getCurrentBoard()->turn = chess::WHITE;
    } else {
        this->sbv->getCurrentBoard()->turn = chess::BLACK;
    }
    this->checkConsistency();
}

void DialogEnterPosition::setCastlingRights() {
    if(this->cbWhiteShort->isChecked()) {
        this->sbv->getCurrentBoard()->set_castle_wking(true);
    } else {
        this->sbv->getCurrentBoard()->set_castle_wking(false);
    }

    if(this->cbWhiteLong->isChecked()) {
        this->sbv->getCurrentBoard()->set_castle_wqueen(true);
    } else {
        this->sbv->getCurrentBoard()->set_castle_wqueen(false);
    }

    if(this->cbBlackShort->isChecked()) {
        this->sbv->getCurrentBoard()->set_castle_bking(true);
    } else {
        this->sbv->getCurrentBoard()->set_castle_bking(false);
    }

    if(this->cbBlackLong->isChecked()) {
        this->sbv->getCurrentBoard()->set_castle_bqueen(true);
    } else {
        this->sbv->getCurrentBoard()->set_castle_bqueen(false);
    }

    this->checkConsistency();
}

chess::Board* DialogEnterPosition::getCurrentBoard() {
    return this->sbv->getCurrentBoard();
}
