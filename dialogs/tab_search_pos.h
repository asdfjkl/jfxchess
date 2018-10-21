#ifndef TABSEARCHPOS_H
#define TABSEARCHPOS_H

#include <QWidget>
#include <QPushButton>
#include <QSpinBox>
#include "model/game_model.h"
#include "viewController/enterposboard.h"

class TabSearchPos : public QWidget
{
    Q_OBJECT
public:
    explicit TabSearchPos(GameModel* model, QWidget *parent = 0);

    QSpinBox *firstMove;
    QSpinBox *lastMove;
    QSpinBox *occursAtLeast;

    QPushButton *buttonInit;
    QPushButton *buttonClear;
    QPushButton *buttonCurrent;

    chess::Board getBoard();

private:
    EnterPosBoard *enterPos;
    chess::Board board;

signals:

public slots:
};

#endif // TABSEARCHPOS_H
