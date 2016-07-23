#ifndef DIALOG_ENTERPOSITION_H
#define DIALOG_ENTERPOSITION_H

#include <QDialog>
#include "chess/board.h"
#include "viewController/colorstyle.h"
#include "viewController/piece_images.h"
#include "viewController/enterposboard.h"
#include <QCheckBox>
#include <QRadioButton>
#include <QPushButton>
#include <QDialogButtonBox>

class DialogEnterPosition : public QDialog
{
    Q_OBJECT
public:
    explicit DialogEnterPosition(chess::Board *board, ColorStyle *style,
                                 QWidget *parent = 0);

    chess::Board* getCurrentBoard();

private:
    void resizeTo(float ratio);
    //SimpleBoardView *sbv;
    EnterPosBoard *sbv;
    QCheckBox *cbWhiteShort;
    QCheckBox *cbWhiteLong;
    QCheckBox *cbBlackShort;
    QCheckBox *cbBlackLong;

    QRadioButton *rbWhite;
    QRadioButton *rbBlack;

    QPushButton *buttonInit;
    QPushButton *buttonClear;
    QPushButton *buttonCurrent;

    QDialogButtonBox *buttonBox;

protected:

signals:

public slots:
    void setToInitialPosition();
    void clearBoard();
    void setToCurrentBoard();
    void checkConsistency();
    void setCastlingRights();
    void setTurn();
};

#endif // DIALOG_ENTERPOSITION_H
