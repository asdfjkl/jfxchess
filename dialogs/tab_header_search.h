#ifndef TABHEADERSEARCH_H
#define TABHEADERSEARCH_H

#include <QWidget>
#include <QLineEdit>
#include <QCheckBox>
#include <QSpinBox>
#include <QPushButton>

class TabHeaderSearch : public QWidget
{
    Q_OBJECT
public:
    explicit TabHeaderSearch(QWidget *parent = 0);

    QLineEdit* whiteSurname;
    QLineEdit* whiteFirstname;

    QLineEdit* blackSurname;
    QLineEdit* blackFirstname;

    QCheckBox *cbIgnoreColors;

    QLineEdit *event;
    QLineEdit *site;
    QCheckBox *cbYear;
    QCheckBox *cbEco;
    QCheckBox *cbMoves;

    QSpinBox *minYear;
    QSpinBox *maxYear;
    QLineEdit *startEco;
    QLineEdit *stopEco;
    QSpinBox *minMove;
    QSpinBox *maxMove;

    QSpinBox *minElo;
    QSpinBox *maxElo;

    QCheckBox *btnWhiteWins;
    QCheckBox *btnBlackWins;
    QCheckBox *btnUndecided;
    QCheckBox *btnDraw;

    QPushButton *btnReset;

signals:

public slots:
};

#endif // TABHEADERSEARCH_H
