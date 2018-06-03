#include "dialog_editheaders.h"
#include <QFormLayout>
#include <QDialogButtonBox>
#include <QGroupBox>
#include <QDebug>


DialogEditHeaders::DialogEditHeaders(chess::Game &g, QWidget *parent) :
    QDialog(parent)
{

    leEvent = new QLineEdit(this);
    leSite = new QLineEdit(this);
    leDate = new QLineEdit(this);
    leRound = new QLineEdit(this);
    leWhite = new QLineEdit(this);
    leBlack = new QLineEdit(this);
    leECO = new QLineEdit(this);
    rbWhiteWins = new QRadioButton("1-0", this);
    rbBlackWins = new QRadioButton("0-1", this);
    rbDraw = new QRadioButton("1/2 - 1/2", this);
    rbUndefined = new QRadioButton("*", this);

    lblSite = new QLabel("Site", this);
    lblDate = new QLabel("Date", this);
    lblRound = new QLabel("Round", this);
    lblEvent = new QLabel("Event", this);
    lblWhite = new QLabel("White", this);
    lblBlack = new QLabel("Black", this);
    lblResult = new QLabel("Result", this);
    lblEco = new QLabel("Eco", this);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel, this);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogEditHeaders::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogEditHeaders::reject);

    QFormLayout *formLayout = new QFormLayout;
    formLayout->addRow(lblSite, leSite);
    formLayout->addRow(lblDate, leDate);
    formLayout->addRow(lblRound, leRound);
    formLayout->addRow(lblEvent, leEvent);
    formLayout->addRow(lblWhite, leWhite);
    formLayout->addRow(lblBlack, leBlack);
    formLayout->addRow(lblEco, leECO);

    QGroupBox *grbData = new QGroupBox(tr("Game Data"), this);
    QGroupBox *grbRes = new QGroupBox(tr("Result"), this);

    grbData->setLayout(formLayout);
    QHBoxLayout *hbox = new QHBoxLayout();
    hbox->addWidget(rbWhiteWins);
    hbox->addWidget(rbBlackWins);
    hbox->addWidget(rbDraw);
    hbox->addWidget(rbUndefined);
    grbRes->setLayout(hbox);

    QVBoxLayout *layout = new QVBoxLayout();
    layout->addWidget(grbData);
    layout->addWidget(grbRes);
    layout->addWidget(buttonBox);

    leDate->setText(g.getHeader("Date"));
    leSite->setText(g.getHeader("Site"));
    leRound->setText(g.getHeader("Round"));
    leEvent->setText(g.getHeader("Event"));
    leWhite->setText(g.getHeader("White"));
    leBlack->setText(g.getHeader("Black"));
    leECO->setText(g.getHeader("ECO"));

    rbWhiteWins->setChecked(true);
    QString res = g.getHeader("Result");

    if(!res.isEmpty()) {
        if(res == QString("0-1")) {
            rbBlackWins->setChecked(true);
        }
        if(res == QString("1/2-1/2")) {
            rbDraw->setChecked(true);
        }
        if(res == QString("*")) {
            rbUndefined->setChecked(true);
        }
    }
    setLayout(layout);

}
