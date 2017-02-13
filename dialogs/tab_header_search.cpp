#include "tab_header_search.h"
#include <QLineEdit>
#include <QLabel>
#include <QCheckBox>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QSpinBox>
#include <QRadioButton>
#include <QButtonGroup>
#include <QPushButton>

TabHeaderSearch::TabHeaderSearch(QWidget *parent) : QWidget(parent)
{
    QGridLayout *gridLayout = new QGridLayout();

    QLabel *nameWhite = new QLabel(tr("White:"));
    QLineEdit* whiteSurname = new QLineEdit(this);
    QLineEdit* whiteFirstname = new QLineEdit(this);
    QLabel* colonWhite = new QLabel(",", this);

    nameWhite->setBuddy(whiteSurname);
    colonWhite->setBuddy(whiteFirstname);

    QLabel* nameBlack = new QLabel(tr("Black:"), this);
    QLineEdit* blackSurname = new QLineEdit(this);
    QLineEdit* blackFirstname = new QLineEdit(this);
    QLabel* colonBlack = new QLabel(",", this);

    nameBlack->setBuddy(blackSurname);
    colonBlack->setBuddy(blackFirstname);

    QCheckBox *cbIgnoreColors = new QCheckBox(tr("Ignore Colors"), this);

    gridLayout->addWidget(nameWhite, 0, 0);
    gridLayout->addWidget(whiteSurname, 0, 1);
    gridLayout->addWidget(colonWhite, 0, 2);
    gridLayout->addWidget(whiteFirstname, 0, 3);

    gridLayout->addWidget(nameBlack, 1, 0);
    gridLayout->addWidget(blackSurname, 1, 1);
    gridLayout->addWidget(colonBlack, 1, 2);
    gridLayout->addWidget(blackFirstname, 1, 3);

    gridLayout->addWidget(cbIgnoreColors, 2, 1);

    QLabel *lblEvent = new QLabel(tr("Event:"));
    QLineEdit *event = new QLineEdit(this);
    lblEvent->setBuddy(event);

    QLabel *lblSite = new QLabel(tr("Site:"));
    QLineEdit *site = new QLineEdit(this);
    lblSite->setBuddy(site);

    gridLayout->addWidget(lblEvent, 3,0);
    gridLayout->addWidget(event, 3,1);

    gridLayout->addWidget(lblSite, 4,0);
    gridLayout->addWidget(site, 4,1);

    QGroupBox *gbElo = new QGroupBox("Elo");
    QSpinBox *minElo = new QSpinBox();
    QSpinBox *maxElo = new QSpinBox();
    minElo->setAlignment(Qt::AlignLeft);
    maxElo->setAlignment(Qt::AlignLeft);
    QButtonGroup *eloButtons = new QButtonGroup();
    QRadioButton *btnIgnoreElo = new QRadioButton(tr("Ignore"));
    QRadioButton *btnBothElo = new QRadioButton(tr("Both"));
    QRadioButton *btnOneElo = new QRadioButton(tr("One"));
    QRadioButton *btnAverageElo = new QRadioButton("Average");
    eloButtons->addButton(btnIgnoreElo);
    eloButtons->addButton(btnBothElo);
    eloButtons->addButton(btnOneElo);
    eloButtons->addButton(btnAverageElo);
    QGridLayout *layoutElo = new QGridLayout();
    layoutElo->addWidget(minElo, 0, 0);
    layoutElo->addWidget(new QLabel("-"), 0, 1);
    layoutElo->addWidget(maxElo, 0, 2);
    layoutElo->addWidget(btnIgnoreElo, 1, 0);
    layoutElo->addWidget(btnOneElo, 1, 2);
    layoutElo->addWidget(btnBothElo, 2, 0);
    layoutElo->addWidget(btnAverageElo, 2, 2);
    gbElo->setLayout(layoutElo);

    QCheckBox *cbYear = new QCheckBox(tr("Year:"));
    QCheckBox *cbEco = new QCheckBox("ECO:");
    QCheckBox *cbMoves = new QCheckBox("Moves:");

    QSpinBox *minYear = new QSpinBox();
    QSpinBox *maxYear = new QSpinBox();
    QLineEdit *startEco = new QLineEdit();
    QLineEdit *stopEco = new QLineEdit();
    QSpinBox *minMove = new QSpinBox();
    QSpinBox *maxMove = new QSpinBox();

    gridLayout->addWidget(cbYear, 5, 0);
    gridLayout->addWidget(minYear, 5, 1);
    gridLayout->addWidget(new QLabel("-"), 5, 2);
    gridLayout->addWidget(maxYear, 5, 3);


    gridLayout->addWidget(cbEco, 6, 0);
    gridLayout->addWidget(startEco, 6, 1);
    gridLayout->addWidget(new QLabel("-"), 6, 2);
    gridLayout->addWidget(stopEco, 6, 3);


    gridLayout->addWidget(cbMoves, 7, 0);
    gridLayout->addWidget(minMove, 7, 1);
    gridLayout->addWidget(new QLabel("-"), 7, 2);
    gridLayout->addWidget(maxMove, 7, 3);

    QPushButton *btnReset = new QPushButton(tr("Reset"));


    gridLayout->addWidget(gbElo, 3, 5, 3, 1);

    gridLayout->addWidget(lblEvent, 4,0);
    gridLayout->addWidget(event, 4,1);

    QGroupBox *gbResult = new QGroupBox(tr("Result"));
    QButtonGroup *resultButtons = new QButtonGroup();
    QRadioButton *btnWhiteWins = new QRadioButton("1-0");
    QRadioButton *btnBlackWins = new QRadioButton("0-1");
    QRadioButton *btnDraw = new QRadioButton("1/2-1/2");
    QRadioButton *btnUndecided = new QRadioButton("*");
    resultButtons->addButton(btnWhiteWins);
    resultButtons->addButton(btnBlackWins);
    resultButtons->addButton(btnDraw);
    resultButtons->addButton(btnUndecided);
    QGridLayout *layoutResult = new QGridLayout();
    layoutResult->addWidget(btnWhiteWins, 0, 0);
    layoutResult->addWidget(btnBlackWins, 0, 1);
    layoutResult->addWidget(btnDraw, 1, 0);
    layoutResult->addWidget(btnUndecided, 1, 1);
    gbResult->setLayout(layoutResult);

    gridLayout->addWidget(gbResult, 6, 5, 3, 1);

    gridLayout->addWidget(btnReset, 10, 5);


    this->setLayout(gridLayout);
}
