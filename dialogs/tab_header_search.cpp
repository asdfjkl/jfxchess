#include "tab_header_search.h"
#include <QLabel>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QRadioButton>
#include <QButtonGroup>
#include <QDebug>

TabHeaderSearch::TabHeaderSearch(QWidget *parent) : QWidget(parent)
{
    QFontMetrics f = this->fontMetrics();
    int len_name = f.width(this->tr("abcdefghjlmnopqrstuvw"));
    int len_vertical_space = f.height();
    int len_year = f.width("2222")*2;
    int len_eco = f.width("EEEE")*2;
    int len_space = f.width("  ");

    QLabel *lblWhiteName = new QLabel(tr("White:"), this);
    this->whiteName = new QLineEdit(this);
    lblWhiteName->setBuddy(whiteName);
    whiteName->setFixedWidth(len_name);

    QLabel *lblBlackName = new QLabel(tr("Black:"), this);
    this->blackName = new QLineEdit(this);
    lblBlackName->setBuddy(blackName);
    blackName->setFixedWidth(len_name);

    this->cbIgnoreColors = new QCheckBox(tr("Ignore Colors"), this);

    QFormLayout *layoutNames = new QFormLayout();
    layoutNames->addRow(lblWhiteName, whiteName);
    layoutNames->addRow(lblBlackName, blackName);
    layoutNames->addRow(new QLabel(""), cbIgnoreColors);

    QFormLayout *layoutNameSite = new QFormLayout();

    QLabel *lblEvent = new QLabel(tr("Event:"), this);
    this->event = new QLineEdit(this);
    lblEvent->setBuddy(event);
    event->setFixedWidth(len_name);

    QLabel *lblSite = new QLabel(tr("Site:"));
    this->site = new QLineEdit(this);
    lblSite->setBuddy(site);
    site->setFixedWidth(len_name);

    layoutNameSite->addRow(lblEvent, event);
    layoutNameSite->addRow(lblSite, site);

    this->cbYear = new QCheckBox(tr("Year:"), this);
    this->cbEco = new QCheckBox("ECO:", this);

    this->minYear = new QSpinBox(this);
    this->maxYear = new QSpinBox(this);
    this->startEco = new QLineEdit(this);
    this->stopEco = new QLineEdit(this);

    minYear->setFixedWidth(len_year);
    maxYear->setFixedWidth(len_year);
    minYear->setRange(0,2200);
    maxYear->setRange(0,2200);
    startEco->setFixedWidth(len_eco);
    stopEco->setFixedWidth(len_eco);

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
    this->btnIgnoreElo = new QRadioButton(tr("Ignore"), this);
    this->btnBothElo = new QRadioButton(tr("Both"), this);
    this->btnOneElo = new QRadioButton(tr("One"), this);
    this->btnAverageElo = new QRadioButton("Average", this);

    this->btnIgnoreElo->setChecked(true);

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

    connect(this->btnReset, &QPushButton::pressed, this, &TabHeaderSearch::onReset);

    this->setLayout(mainLayout);
}

void TabHeaderSearch::onReset() {

    this->whiteName->setText("");
    this->blackName->setText("");
    this->cbIgnoreColors->setChecked(false);
    this->event->setText("");
    this->site->setText("");
    this->cbYear->setChecked(false);
    this->cbEco->setChecked(false);
    this->minYear->setValue(500);
    this->maxYear->setValue(2200);
    this->startEco->setText("A00");
    this->stopEco->setText("E99");
    this->minElo->setValue(1000);
    this->maxElo->setValue(3000);

    this->btnIgnoreElo->setChecked(true);
    this->btnAverageElo->setChecked(false);
    this->btnBothElo->setChecked(false);
    this->btnOneElo->setChecked(false);

    this->btnUndecided->setChecked(false);
    this->btnWhiteWins->setChecked(false);
    this->btnBlackWins->setChecked(false);
    this->btnDraw->setChecked(false);

}


