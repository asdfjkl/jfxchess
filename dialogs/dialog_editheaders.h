#ifndef DIALOG_EDITHEADERS_H
#define DIALOG_EDITHEADERS_H

#include <QDialog>
#include <QLineEdit>
#include <QRadioButton>
#include <QLabel>
#include "chess/game.h"

class DialogEditHeaders : public QDialog
{
    Q_OBJECT
public:
    explicit DialogEditHeaders(chess::Game &g, QWidget *parent = 0);
    QLineEdit *leEvent;
    QLineEdit *leSite;
    QLineEdit *leDate;
    QLineEdit *leRound;
    QLineEdit *leWhite;
    QLineEdit *leBlack;
    QLineEdit *leECO;
    QRadioButton *rbWhiteWins;
    QRadioButton *rbBlackWins;
    QRadioButton *rbDraw;
    QRadioButton *rbUndefined;

private:
    QLabel *lblEvent;
    QLabel *lblSite;
    QLabel *lblDate;
    QLabel *lblRound;
    QLabel *lblWhite;
    QLabel *lblBlack;
    QLabel *lblResult;
    QLabel *lblEco;


signals:

public slots:

};

#endif // DIALOG_EDITHEADERS_H
