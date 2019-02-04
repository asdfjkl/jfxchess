#ifndef TAB_HEADER_SEARCH_H
#define TAB_HEADER_SEARCH_H

#include <QWidget>
#include <QCheckBox>
#include <QLineEdit>
#include <QSpinBox>
#include <QPushButton>
#include <QRadioButton>

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

    QSpinBox *minYear;
    QSpinBox *maxYear;
    QLineEdit *startEco;
    QLineEdit *stopEco;

    QSpinBox *minElo;
    QSpinBox *maxElo;

    QRadioButton *btnIgnoreElo;
    QRadioButton *btnBothElo;
    QRadioButton *btnOneElo;
    QRadioButton *btnAverageElo;

    QCheckBox *btnWhiteWins;
    QCheckBox *btnBlackWins;
    QCheckBox *btnUndecided;
    QCheckBox *btnDraw;

    QPushButton *btnReset;

signals:

public slots:
    void onReset();
};

#endif // TAB_HEADER_SEARCH_H
