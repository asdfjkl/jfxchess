#include "tab_header_search.h"
#include <QLabel>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QRadioButton>
#include <QButtonGroup>

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

    QLabel *nameWhite = new QLabel(tr("White:"), this);
    this->whiteSurname = new QLineEdit(this);
    this->whiteFirstname = new QLineEdit(this);
    QLabel* colonWhite = new QLabel(",", this);
    QHBoxLayout *layoutWhiteName = new QHBoxLayout();
    whiteFirstname->setFixedWidth(len_firstname);
    whiteSurname->setFixedWidth(len_surname);
    layoutWhiteName->addWidget(whiteSurname);
    layoutWhiteName->addWidget(colonWhite);
    layoutWhiteName->addWidget(whiteFirstname);
    layoutWhiteName->addStretch(1);
    nameWhite->setBuddy(whiteSurname);
    colonWhite->setBuddy(whiteFirstname);

    QLabel* nameBlack = new QLabel(tr("Black:"), this);
    this->blackSurname = new QLineEdit(this);
    this->blackFirstname = new QLineEdit(this);
    QLabel* colonBlack = new QLabel(",", this);
    QHBoxLayout *layoutBlackName = new QHBoxLayout();
    blackFirstname->setFixedWidth(len_firstname);
    blackSurname->setFixedWidth(len_surname);
    layoutBlackName->addWidget(blackSurname);
    layoutBlackName->addWidget(colonBlack);
    layoutBlackName->addWidget(blackFirstname);
    layoutBlackName->addStretch(1);
    nameBlack->setBuddy(blackSurname);
    colonBlack->setBuddy(blackFirstname);

    this->cbIgnoreColors = new QCheckBox(tr("Ignore Colors"), this);

    QFormLayout *layoutNames = new QFormLayout();
    layoutNames->addRow(nameWhite, layoutWhiteName);
    layoutNames->addRow(nameBlack, layoutBlackName);
    layoutNames->addRow(new QLabel(""), cbIgnoreColors);

    QFormLayout *layoutNameSite = new QFormLayout();

    QLabel *lblEvent = new QLabel(tr("Event:"), this);
    this->event = new QLineEdit(this);
    lblEvent->setBuddy(event);
    event->setFixedWidth(len_surname);

    QLabel *lblSite = new QLabel(tr("Site:"));
    this->site = new QLineEdit(this);
    lblSite->setBuddy(site);
    site->setFixedWidth(len_surname);

    layoutNameSite->addRow(lblEvent, event);
    layoutNameSite->addRow(lblSite, site);

    this->cbYear = new QCheckBox(tr("Year:"), this);
    this->cbEco = new QCheckBox("ECO:", this);
    this->cbMoves = new QCheckBox("Moves:", this);

    this->minYear = new QSpinBox(this);
    this->maxYear = new QSpinBox(this);
    this->startEco = new QLineEdit(this);
    this->stopEco = new QLineEdit(this);
    this->minMove = new QSpinBox(this);
    this->maxMove = new QSpinBox(this);

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
    layoutYearEcoMoves->addWidget(new QLabel("-", this), 0, 2);
    layoutYearEcoMoves->addWidget(maxYear, 0, 3);
    layoutYearEcoMoves->setColumnStretch(4, 3);

    minYear->setAlignment(Qt::AlignLeft);
    maxYear->setAlignment(Qt::AlignLeft);

    layoutYearEcoMoves->addWidget(cbEco, 1, 0);
    layoutYearEcoMoves->addWidget(startEco, 1, 1);
    layoutYearEcoMoves->addWidget(new QLabel("-", this), 1, 2);
    layoutYearEcoMoves->addWidget(stopEco, 1, 3);

    layoutYearEcoMoves->addWidget(cbMoves, 2, 0);
    layoutYearEcoMoves->addWidget(minMove, 2, 1);
    layoutYearEcoMoves->addWidget(new QLabel("-", this), 2, 2);
    layoutYearEcoMoves->addWidget(maxMove, 2, 3);

    QGroupBox *gbElo = new QGroupBox("Elo", this);
    this->minElo = new QSpinBox(this);
    this->maxElo = new QSpinBox(this);
    minElo->setFixedWidth(len_year);
    maxElo->setFixedWidth(len_year);
    minElo->setRange(1,3000);
    maxElo->setRange(1,3000);
    minElo->setAlignment(Qt::AlignLeft);
    maxElo->setAlignment(Qt::AlignLeft);
    QButtonGroup *eloButtons = new QButtonGroup(this);
    QRadioButton *btnIgnoreElo = new QRadioButton(tr("Ignore"), this);
    QRadioButton *btnBothElo = new QRadioButton(tr("Both"), this);
    QRadioButton *btnOneElo = new QRadioButton(tr("One"), this);
    QRadioButton *btnAverageElo = new QRadioButton("Average", this);
    eloButtons->addButton(btnIgnoreElo);
    eloButtons->addButton(btnBothElo);
    eloButtons->addButton(btnOneElo);
    eloButtons->addButton(btnAverageElo);
    QGridLayout *layoutElo = new QGridLayout();
    layoutElo->addWidget(minElo, 0, 0);
    layoutElo->addWidget(new QLabel("-", this), 0, 1);
    layoutElo->addWidget(maxElo, 0, 2);
    layoutElo->addWidget(btnIgnoreElo, 1, 0);
    layoutElo->addWidget(btnOneElo, 1, 2);
    layoutElo->addWidget(btnBothElo, 2, 0);
    layoutElo->addWidget(btnAverageElo, 2, 2);
    gbElo->setLayout(layoutElo);

    QGroupBox *gbResult = new QGroupBox(tr("Result"), this);
    QButtonGroup *resultButtons = new QButtonGroup(this);
    resultButtons->setExclusive(false);
    this->btnWhiteWins = new QCheckBox("1-0", this);
    this->btnBlackWins = new QCheckBox("0-1", this);
    this->btnUndecided = new QCheckBox("*", this);
    this->btnDraw = new QCheckBox("1/2-1/2", this);
    resultButtons->addButton(btnWhiteWins);
    resultButtons->addButton(btnBlackWins);
    resultButtons->addButton(btnDraw);
    resultButtons->addButton(btnUndecided);
    QGridLayout *layoutResult = new QGridLayout(this);
    layoutResult->addWidget(btnWhiteWins, 0, 0);
    layoutResult->addWidget(btnBlackWins, 0, 1);
    layoutResult->addWidget(btnDraw, 1, 1);
    layoutResult->addWidget(btnUndecided, 1, 0);
    gbResult->setLayout(layoutResult);

    this->btnReset = new QPushButton(tr("Reset"), this);

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
