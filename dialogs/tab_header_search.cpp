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
    QFontMetrics f = this->fontMetrics();
    int len_surname = f.width(this->tr("abcdefghjlmnopqrstuvw"));
    int len_firstname = f.width(this->tr("abcdefghijklmnop"));
    int len_vertical_space = f.height();
    int len_year = f.width("2222")*2;
    int len_eco = f.width("EEE")*2;
    int len_moves = f.width("99")*3;
    int len_space = f.width("  ");

    QLabel *nameWhite = new QLabel(tr("White:"));
    QLineEdit* whiteSurname = new QLineEdit(this);
    QLineEdit* whiteFirstname = new QLineEdit(this);
    QLabel* colonWhite = new QLabel(",", this);
    QHBoxLayout *layoutWhiteName = new QHBoxLayout();
    whiteFirstname->setFixedWidth(len_firstname);
    whiteSurname->setFixedWidth(len_surname);
    layoutWhiteName->addWidget(whiteSurname);
    layoutWhiteName->addWidget(colonWhite);
    layoutWhiteName->addWidget(whiteFirstname);
    nameWhite->setBuddy(whiteSurname);
    colonWhite->setBuddy(whiteFirstname);

    QLabel* nameBlack = new QLabel(tr("Black:"), this);
    QLineEdit* blackSurname = new QLineEdit(this);
    QLineEdit* blackFirstname = new QLineEdit(this);
    QLabel* colonBlack = new QLabel(",", this);
    QHBoxLayout *layoutBlackName = new QHBoxLayout();
    blackFirstname->setFixedWidth(len_firstname);
    blackSurname->setFixedWidth(len_surname);
    layoutBlackName->addWidget(blackSurname);
    layoutBlackName->addWidget(colonBlack);
    layoutBlackName->addWidget(blackFirstname);
    nameBlack->setBuddy(blackSurname);
    colonBlack->setBuddy(blackFirstname);

    QCheckBox *cbIgnoreColors = new QCheckBox(tr("Ignore Colors"), this);

    QFormLayout *layoutNames = new QFormLayout();
    layoutNames->addRow(nameWhite, layoutWhiteName);
    layoutNames->addRow(nameBlack, layoutBlackName);
    layoutNames->addRow(new QLabel(""), cbIgnoreColors);

    QFormLayout *layoutNameSite = new QFormLayout();

    QLabel *lblEvent = new QLabel(tr("Event:"));
    QLineEdit *event = new QLineEdit(this);
    lblEvent->setBuddy(event);
    event->setFixedWidth(len_surname);

    QLabel *lblSite = new QLabel(tr("Site:"));
    QLineEdit *site = new QLineEdit(this);
    lblSite->setBuddy(site);
    site->setFixedWidth(len_surname);

    layoutNameSite->addRow(lblEvent, event);
    layoutNameSite->addRow(lblSite, site);

    QCheckBox *cbYear = new QCheckBox(tr("Year:"));
    QCheckBox *cbEco = new QCheckBox("ECO:");
    QCheckBox *cbMoves = new QCheckBox("Moves:");

    QSpinBox *minYear = new QSpinBox();
    QSpinBox *maxYear = new QSpinBox();
    QLineEdit *startEco = new QLineEdit();
    QLineEdit *stopEco = new QLineEdit();
    QSpinBox *minMove = new QSpinBox();
    QSpinBox *maxMove = new QSpinBox();

    minYear->setFixedWidth(len_year);
    maxYear->setFixedWidth(len_year);
    minYear->setRange(0,2200);
    maxYear->setRange(0,2200);
    startEco->setFixedWidth(len_eco);
    stopEco->setFixedWidth(len_eco);
    minMove->setFixedWidth(len_moves);
    maxMove->setFixedWidth(len_moves);
    minMove->setRange(1,500);
    maxMove->setRange(1,500);

    QGridLayout *layoutYearEcoMoves = new QGridLayout();
    layoutYearEcoMoves->addWidget(cbYear, 0, 0);
    layoutYearEcoMoves->addWidget(minYear, 0, 1);
    layoutYearEcoMoves->addWidget(new QLabel("-"), 0, 2);
    layoutYearEcoMoves->addWidget(maxYear, 0, 3);
    layoutYearEcoMoves->setColumnStretch(4, 3);

    minYear->setAlignment(Qt::AlignLeft);
    maxYear->setAlignment(Qt::AlignLeft);

    layoutYearEcoMoves->addWidget(cbEco, 1, 0);
    layoutYearEcoMoves->addWidget(startEco, 1, 1);
    layoutYearEcoMoves->addWidget(new QLabel("-"), 1, 2);
    layoutYearEcoMoves->addWidget(stopEco, 1, 3);

    layoutYearEcoMoves->addWidget(cbMoves, 2, 0);
    layoutYearEcoMoves->addWidget(minMove, 2, 1);
    layoutYearEcoMoves->addWidget(new QLabel("-"), 2, 2);
    layoutYearEcoMoves->addWidget(maxMove, 2, 3);

    QGroupBox *gbElo = new QGroupBox("Elo");
    QSpinBox *minElo = new QSpinBox();
    QSpinBox *maxElo = new QSpinBox();
    minElo->setFixedWidth(len_year);
    maxElo->setFixedWidth(len_year);
    minElo->setRange(1,3000);
    maxElo->setRange(1,3000);
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

    QGroupBox *gbResult = new QGroupBox(tr("Result"));
    QButtonGroup *resultButtons = new QButtonGroup();
    resultButtons->setExclusive(false);
    QCheckBox *btnWhiteWins = new QCheckBox("1-0");
    QCheckBox *btnBlackWins = new QCheckBox("0-1");
    QCheckBox *btnDraw = new QCheckBox("1/2-1/2");
    QCheckBox *btnUndecided = new QCheckBox("*");
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

    QPushButton *btnReset = new QPushButton(tr("Reset"));

/*



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


    gridLayout->addWidget(gbResult, 6, 5, 3, 1);

    gridLayout->addWidget(btnReset, 10, 5);
*/

    QVBoxLayout *layoutLeft = new QVBoxLayout();
    QVBoxLayout *layoutRight = new QVBoxLayout();
    QHBoxLayout *layoutLeftRight = new QHBoxLayout();

    layoutLeft->addLayout(layoutNames);
    layoutLeft->addSpacing(len_vertical_space);
    layoutLeft->addLayout(layoutNameSite);
    layoutLeft->addSpacing(len_vertical_space);
    layoutLeft->addLayout(layoutYearEcoMoves);

    layoutRight->addWidget(gbElo);
    layoutRight->addSpacing(len_vertical_space);
    layoutRight->addWidget(gbResult);
    layoutRight->addStretch(10);

    layoutLeftRight->addLayout(layoutLeft);
    layoutLeftRight->addSpacing(len_space);
    layoutLeftRight->addLayout(layoutRight);

    QHBoxLayout *layoutResetButton = new QHBoxLayout();
    layoutResetButton->addStretch(10);
    layoutResetButton->addWidget(btnReset);

    QVBoxLayout *mainLayout = new QVBoxLayout();
    mainLayout->addLayout(layoutLeftRight);
    mainLayout->addLayout(layoutResetButton);

    this->setLayout(mainLayout);
}
