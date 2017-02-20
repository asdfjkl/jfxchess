#include "dialog_search.h"
#include "tab_header_search.h"
#include <QTabWidget>
#include <QDialogButtonBox>
#include <QVBoxLayout>

DialogSearch::DialogSearch(QWidget *parent) :
    QDialog(parent)
{

    TabHeaderSearch* ths = new TabHeaderSearch(this);

    QTabWidget *tabWidget = new QTabWidget;
    tabWidget->addTab(ths, tr("Game Data"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogSearch::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogSearch::reject);

    QVBoxLayout *mainLayout = new QVBoxLayout;
    mainLayout->addWidget(tabWidget);
    mainLayout->addWidget(buttonBox);
    setLayout(mainLayout);

    setWindowTitle(tr("Search for Games"));

}