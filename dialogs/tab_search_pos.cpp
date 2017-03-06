#include "tab_search_pos.h"
#include "viewController/enterposboard.h"
#include "various/resource_finder.h"
#include <QDebug>
#include <QPushButton>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QDialogButtonBox>
#include <QSpinBox>
#include <QLabel>

TabSearchPos::TabSearchPos(GameModel* model, QWidget *parent) : QWidget(parent)
{
    QFontMetrics f = this->fontMetrics();
    int len_moves = f.width("99")*3;

    QSpinBox *firstMove = new QSpinBox();
    QSpinBox *lastMove = new QSpinBox();
    QSpinBox *occursAtLeast = new QSpinBox();

    firstMove->setFixedWidth(len_moves);
    lastMove->setFixedWidth(len_moves);
    occursAtLeast->setFixedWidth(len_moves);

    firstMove->setRange(0,999);
    lastMove->setRange(0,999);
    occursAtLeast->setRange(0,999);

    QLabel *lblFirstMove = new QLabel("after ply:");
    QLabel *lblLastMove = new QLabel("before ply:");
    QLabel *lblOccursLast = new QLabel("occurs at least:");

    firstMove->setValue(1);
    lastMove->setValue(40);
    occursAtLeast->setValue(1);

    chess::Board *board = new chess::Board(true);
    ColorStyle *cs = model->colorStyle;
    EnterPosBoard *enterPos = new EnterPosBoard(cs, board, this);

    QPushButton *buttonInit = new QPushButton(tr("Initial Position"));
    QPushButton *buttonClear = new QPushButton(tr("Clear Board"));
    QPushButton *buttonCurrent = new QPushButton(tr("Current Position"));

    QGridLayout *layoutSpinBoxes = new QGridLayout();
    layoutSpinBoxes->addWidget(lblFirstMove, 0, 0);
    layoutSpinBoxes->addWidget(firstMove, 0, 1);
    layoutSpinBoxes->addWidget(lblLastMove, 1, 0);
    layoutSpinBoxes->addWidget(lastMove, 1, 1);
    layoutSpinBoxes->addWidget(lblOccursLast, 2, 0);
    layoutSpinBoxes->addWidget(occursAtLeast, 2, 1);


    QVBoxLayout *vbox_config = new QVBoxLayout();
    vbox_config->addLayout(layoutSpinBoxes);
    vbox_config->addStretch(1);
    vbox_config->addWidget(buttonInit);
    vbox_config->addWidget(buttonClear);
    vbox_config->addWidget(buttonCurrent);

    QHBoxLayout *hbox = new QHBoxLayout();
    hbox->addWidget(enterPos);
    hbox->addLayout(vbox_config);

    this->setLayout(hbox);


}

