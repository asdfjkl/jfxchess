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

    TabHeaderSearch* ths = new TabHeaderSearch(this);
    TabCommentSearch *tcs = new TabCommentSearch(this);
    TabSearchPos *tsp = new TabSearchPos(gameModel, this);

    QTabWidget *tabWidget = new QTabWidget;
    tabWidget->addTab(ths, tr("Game Data"));
    tabWidget->addTab(tcs, tr("Comments"));
    tabWidget->addTab(tsp, tr("Position"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogSearch::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogSearch::reject);

    QCheckBox *optGameData = new QCheckBox(tr("Game Data"));
    QCheckBox *optComments = new QCheckBox(tr("Comments"));
    QCheckBox *optPosition = new QCheckBox(tr("Position"));
    QCheckBox *optVariants = new QCheckBox(tr("Search in Variations"));

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

void DialogSearch::updatePattern() {

}
