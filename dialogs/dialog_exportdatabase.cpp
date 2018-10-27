#include "dialog_exportdatabase.h"
#include <QHBoxLayout>
#include <QDebug>
#include <QDialogButtonBox>

DialogExportDatabase::DialogExportDatabase(QWidget *parent) :
    QDialog(parent)
{

    this->groupBoxFormat = new QGroupBox(tr("Format"), parent);
    this->radioFormatPgn = new QRadioButton(tr("Text File (&PGN)"), parent);
    this->radioFormatDci = new QRadioButton(tr("Jerry Database (&DCI)"), parent);

    this->groupBoxSelection = new QGroupBox(tr("Game Selection"), parent);
    this->radioCurrentSelection = new QRadioButton(tr("&Current Selection"), parent);
    this->radioAllGameSelection = new QRadioButton(tr("&All Games"), parent);

    QVBoxLayout *vboxFormat = new QVBoxLayout();
    radioFormatDci->setChecked(true);
    vboxFormat->addWidget(radioFormatDci);
    vboxFormat->addWidget(radioFormatPgn);
    vboxFormat->addStretch(1);
    groupBoxFormat->setLayout(vboxFormat);

    QVBoxLayout *vboxSelection = new QVBoxLayout();
    radioCurrentSelection->setChecked(true);
    vboxSelection->addWidget(radioCurrentSelection);
    vboxSelection->addWidget(radioAllGameSelection);
    vboxSelection->addStretch(1);
    groupBoxSelection->setLayout(vboxSelection);

    QVBoxLayout *mainLayout = new QVBoxLayout();
    mainLayout->addWidget(groupBoxFormat);
    mainLayout->addWidget(groupBoxSelection);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);

    mainLayout->addWidget(buttonBox);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogExportDatabase::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogExportDatabase::reject);

    this->setLayout(mainLayout);

}

