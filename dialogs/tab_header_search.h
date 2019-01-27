#ifndef TAB_HEADER_SEARCH_H
#define TAB_HEADER_SEARCH_H

#include <QWidget>
#include <QCheckBox>
#include <QLineEdit>
#include <QSpinBox>
#include <QPushButton>

class TabHeaderSearch : public QWidget
{
    Q_OBJECT
public:
    explicit TabHeaderSearch(QWidget *parent = nullptr);
    QLineEdit* whiteName;
    QLineEdit* blackName;

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

#endif // TAB_HEADER_SEARCH_H
