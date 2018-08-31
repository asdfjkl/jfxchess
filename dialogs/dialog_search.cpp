#include "dialog_search.h"
#include "tab_header_search.h"
#include "tab_comment_search.h"
#include "tab_search_pos.h"
#include <QTabWidget>
#include <QDialogButtonBox>
#include <QVBoxLayout>
#include <QDebug>
#include <QCheckBox>

DialogSearch::DialogSearch(GameModel *gameModel, QWidget *parent) :
    QDialog(parent)
{
    this->ths = new TabHeaderSearch(this);
    this->tcs = new TabCommentSearch(this);
    this->tsp = new TabSearchPos(gameModel, this);

    QTabWidget *tabWidget = new QTabWidget;
    tabWidget->addTab(ths, tr("Game Data"));
    tabWidget->addTab(tcs, tr("Comments"));
    tabWidget->addTab(tsp, tr("Position"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogSearch::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogSearch::reject);

    this->optGameData = new QCheckBox(tr("Game Data"));
    this->optComments = new QCheckBox(tr("Comments"));
    this->optPosition = new QCheckBox(tr("Position"));
    this->optVariants = new QCheckBox(tr("Search in Variations"));

    QHBoxLayout *layoutOptions = new QHBoxLayout();
    layoutOptions->addWidget(optGameData);
    layoutOptions->addWidget(optComments);
    layoutOptions->addWidget(optPosition);
    layoutOptions->addWidget(optVariants);
    layoutOptions->addStretch(1);

    QVBoxLayout *mainLayout = new QVBoxLayout;
    mainLayout->addWidget(tabWidget);
    mainLayout->addLayout(layoutOptions);
    mainLayout->addWidget(buttonBox);
    setLayout(mainLayout);

    this->setMinimumWidth(this->height()*1.65);
    qDebug() << "this height: " << this->height() << " and min width: " << (this->height()*1.65);

    setWindowTitle(tr("Search for Games"));

}

void DialogSearch::resizeEvent(QResizeEvent *) {
    this->setMinimumWidth(this->height()*1.35);
}

SearchPattern DialogSearch::getPattern() {
    qDebug() << "this height: " << this->height() << " and min width: " << (this->height()*1.65);

    SearchPattern sp;

    // game data search
    sp.whiteName = this->ths->whiteSurname->text().append(", ").append(this->ths->whiteFirstname->text());
    sp.blackName = this->ths->blackSurname->text().append(", ").append(this->ths->blackFirstname->text());

    sp.ignoreNameColor = this->ths->cbIgnoreColors->isChecked();

    sp.event = this->ths->event->text();

    sp.site = this->ths->site->text();

    sp.checkYear = this->ths->cbYear->isChecked();
    sp.checkEco = this->ths->cbEco->isChecked();
    sp.checkMoves = this->ths->cbEco->isChecked();

    sp.year_min = this->ths->minYear->value();
    sp.year_max = this->ths->maxYear->value();

    sp.ecoStart = this->ths->startEco->text();
    sp.ecoStop = this->ths->stopEco->text();

    sp.move_min = this->ths->minMove->value();
    sp.move_max = this->ths->maxMove->value();

    sp.elo_min = this->ths->minElo->value();
    sp.elo_max = this->ths->maxElo->value();

    sp.result = chess::RES_ANY;
    if(this->ths->btnUndecided->isChecked()) {
        sp.result = chess::RES_UNDEF;
    }
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
    sp.comment_text1 = this->tcs->text1->text();
    sp.comment_text2 = this->tcs->text2->text();

    sp.wholeWord = this->tcs->wholeWord->isChecked();
    sp.mustNotStartInInitial = this->tcs->notInitialPos->isChecked();
    sp.mustContainColoredFields = this->tcs->colorFields->isChecked();
    sp.mustContainArrows = this->tcs->arrows->isChecked();

    // search options
    sp.searchGameData = this->optGameData->isChecked();
    sp.searchComments = this->optComments->isChecked();
    sp.searchPosition = this->optPosition->isChecked();
    sp.searchVariations = this->optVariants->isChecked();

    return sp;
}
