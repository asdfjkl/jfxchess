#include "dialog_search.h"
#include "tab_header_search.h"
#include <QTabWidget>
#include <QDialogButtonBox>
#include <QVBoxLayout>
#include <QDebug>
#include <QCheckBox>

DialogSearch::DialogSearch(GameModel *gameModel, QWidget *parent) :
    QDialog(parent)
{
    qDebug() << "dialog search constructor";
    this->ths = new TabHeaderSearch(this);
    //this->tcs = new TabCommentSearch(this);
    //this->tsp = new TabSearchPos(gameModel, this);

    QTabWidget *tabWidget = new QTabWidget;
    tabWidget->addTab(ths, tr("Game Data"));
    //tabWidget->addTab(tcs, tr("Comments"));
    //tabWidget->addTab(tsp, tr("Position"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogSearch::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogSearch::reject);

    //this->optGameData = new QCheckBox(tr("Game Data"));
    //this->optComments = new QCheckBox(tr("Comments"));
    //this->optPosition = new QCheckBox(tr("Position"));
    //this->optVariants = new QCheckBox(tr("Search in Variations"));

    //this->optGameData->setChecked(true);

    //QHBoxLayout *layoutOptions = new QHBoxLayout();
    //layoutOptions->addWidget(optGameData);
    //layoutOptions->addWidget(optComments);
    //layoutOptions->addWidget(optPosition);
    //layoutOptions->addWidget(optVariants);
    //layoutOptions->addStretch(1);

    QVBoxLayout *mainLayout = new QVBoxLayout;
    mainLayout->addWidget(tabWidget);
    //mainLayout->addLayout(layoutOptions);
    mainLayout->addWidget(buttonBox);
    setLayout(mainLayout);

    this->setMinimumWidth(this->height()*1.65);
    qDebug() << "this height: " << this->height() << " and min width: " << (this->height()*1.65);

    setWindowTitle(tr("Search for Games"));

    qDebug() << "dialog search constructor finished";
}

void DialogSearch::resizeEvent(QResizeEvent *) {
    this->setMinimumWidth(this->height()*1.35);
}

SearchPattern DialogSearch::getPattern() {
    qDebug() << "this height: " << this->height() << " and min width: " << (this->height()*1.65);

    SearchPattern sp;

    qDebug() << "names";

    // game data search
    sp.whiteName = this->ths->whiteName->text();
    sp.blackName = this->ths->blackName->text();

    qDebug() << "ignore colors: " << this->ths->cbIgnoreColors->isChecked();
    sp.ignoreNameColor = this->ths->cbIgnoreColors->isChecked();
    qDebug() << "OK";

    sp.event = this->ths->event->text();

    sp.site = this->ths->site->text();

    sp.checkYear = this->ths->cbYear->isChecked();
    sp.checkEco = this->ths->cbEco->isChecked();
    sp.checkMoves = this->ths->cbEco->isChecked();
qDebug() << "OK";
    sp.year_min = this->ths->minYear->value();
    sp.year_max = this->ths->maxYear->value();

    sp.ecoStart = this->ths->startEco->text();
    sp.ecoStop = this->ths->stopEco->text();

    sp.elo_min = this->ths->minElo->value();
    sp.elo_max = this->ths->maxElo->value();
    qDebug() << "OK1";
    if(this->ths->btnAverageElo->isChecked()) {
        sp.checkElo = SEARCH_AVERAGE_ELO;
    }
    if(this->ths->btnIgnoreElo->isChecked()) {
        sp.checkElo = SEARCH_IGNORE_ELO;
    }
    if(this->ths->btnBothElo->isChecked()) {
        sp.checkElo = SEARCH_BOTH_ELO;
    }
    if(this->ths->btnOneElo->isChecked()) {
        sp.checkElo = SEARCH_ONE_ELO;
    }

    sp.result = chess::RES_ANY;
    if(this->ths->btnUndecided->isChecked()) {
        sp.result = chess::RES_UNDEF;
    }
    qDebug() << "OK2";

    if(this->ths->btnWhiteWins->isChecked()) {
        sp.result = chess::RES_WHITE_WINS;
    }
    if(this->ths->btnBlackWins->isChecked()) {
        sp.result = chess::RES_BLACK_WINS;
    }
    if(this->ths->btnUndecided->isChecked()) {
        sp.result = chess::RES_UNDEF;
    }
    if(this->ths->btnDraw->isChecked()) {
        sp.result = chess::RES_DRAW;
    }

    // comment search
    //sp.comment_text1 = this->tcs->text1->text();
    //sp.comment_text2 = this->tcs->text2->text();

    //sp.wholeWord = this->tcs->wholeWord->isChecked();
    //sp.mustNotStartInInitial = this->tcs->notInitialPos->isChecked();
    //sp.caseSensitive = this->tcs->caseSensitive->isChecked();

    qDebug() << "options";

    // search options
    //sp.searchGameData = this->optGameData->isChecked();
    sp.searchGameData = true;
    //sp.searchComments = this->optComments->isChecked();
    //sp.searchPosition = this->optPosition->isChecked();
    //sp.searchVariations = this->optVariants->isChecked();

    //sp.search_board = this->tsp->getBoard();

    return sp;
}

void DialogSearch::setPattern(SearchPattern &sp) {

    // game data search
    this->ths->whiteName->setText(sp.whiteName);
    this->ths->blackName->setText(sp.blackName);
    this->ths->cbIgnoreColors->setChecked(sp.ignoreNameColor);
    this->ths->event->setText(sp.event);
    this->ths->site->setText(sp.site);
    this->ths->cbYear->setChecked(sp.checkYear);
    this->ths->cbEco->setChecked(sp.checkEco);
    this->ths->cbEco->setChecked(sp.checkMoves);
    this->ths->minYear->setValue(sp.year_min);
    this->ths->maxYear->setValue(sp.year_max);
    this->ths->startEco->setText(sp.ecoStart);
    this->ths->stopEco->setText(sp.ecoStop);
    this->ths->minElo->setValue(sp.elo_min);
    this->ths->maxElo->setValue(sp.elo_max);

    switch(sp.checkElo) {
        case SEARCH_AVERAGE_ELO:
            this->ths->btnAverageElo->setChecked(true);
            break;
        case SEARCH_BOTH_ELO:
            this->ths->btnBothElo->setChecked(true);
            break;
        case SEARCH_IGNORE_ELO:
            this->ths->btnIgnoreElo->setChecked(true);
            break;
        case SEARCH_ONE_ELO:
            this->ths->btnOneElo->setChecked(true);
            break;
    }

    switch(sp.result) {
        case chess::RES_UNDEF:
            this->ths->btnUndecided->setChecked(true);
            break;
        case chess::RES_WHITE_WINS:
            this->ths->btnWhiteWins->setChecked(true);
            break;
        case chess::RES_BLACK_WINS:
            this->ths->btnBlackWins->setChecked(true);
            break;
        case chess::RES_DRAW:
            this->ths->btnDraw->setChecked(true);
            break;
    }

}
