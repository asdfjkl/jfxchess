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
    this->pattern = new SearchPattern();

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

    this->setMinimumWidth(this->height()*1.35);

    setWindowTitle(tr("Search for Games"));

}

SearchPattern* DialogSearch::getPattern() {

    // game data search
    this->pattern->whiteName = this->ths->whiteSurname->text().append(", ").append(this->ths->whiteFirstname->text());
    this->pattern->blackName = this->ths->blackSurname->text().append(", ").append(this->ths->blackFirstname->text());

    this->pattern->ignoreNameColor = this->ths->cbIgnoreColors->isChecked();

    this->pattern->event = this->ths->event->text();

    this->pattern->site = this->ths->site->text();

    this->pattern->checkYear = this->ths->cbYear->isChecked();
    this->pattern->checkEco = this->ths->cbEco->isChecked();
    this->pattern->checkMoves = this->ths->cbEco->isChecked();

    this->pattern->year_min = this->ths->minYear->value();
    this->pattern->year_max = this->ths->maxYear->value();

    this->pattern->ecoStart = this->ths->startEco->text();
    this->pattern->ecoStop = this->ths->stopEco->text();

    this->pattern->move_min = this->ths->minMove->value();
    this->pattern->move_max = this->ths->maxMove->value();

    this->pattern->elo_min = this->ths->minElo->value();
    this->pattern->elo_max = this->ths->maxElo->value();

    this->pattern->result = chess::RES_ANY;
    if(this->ths->btnUndecided->isChecked()) {
        this->pattern->result = chess::RES_UNDEF;
    }
    if(this->ths->btnWhiteWins->isChecked()) {
        this->pattern->result = chess::RES_WHITE_WINS;
    }
    if(this->ths->btnBlackWins->isChecked()) {
        this->pattern->result = chess::RES_BLACK_WINS;
    }
    if(this->ths->btnUndecided->isChecked()) {
        this->pattern->result = chess::RES_UNDEF;
    }
    if(this->ths->btnDraw->isChecked()) {
        this->pattern->result = chess::RES_DRAW;
    }

    // comment search
    this->pattern->comment_text1 = this->tcs->text1->text();
    this->pattern->comment_text2 = this->tcs->text2->text();

    this->pattern->wholeWord = this->tcs->wholeWord->isChecked();
    this->pattern->mustNotStartInInitial = this->tcs->notInitialPos->isChecked();
    this->pattern->mustContainColoredFields = this->tcs->colorFields->isChecked();
    this->pattern->mustContainArrows = this->tcs->arrows->isChecked();

    // search options
    this->pattern->searchGameData = this->optGameData->isChecked();
    this->pattern->searchComments = this->optComments->isChecked();
    this->pattern->searchPosition = this->optPosition->isChecked();
    this->pattern->searchVariations = this->optVariants->isChecked();

    return this->pattern;
}
