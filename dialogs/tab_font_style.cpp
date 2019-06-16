#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>
#include <QFontComboBox>
#include <QGridLayout>
#include <QSpacerItem>
#include <QDebug>
#include "tab_font_style.h"

TabFontStyle::TabFontStyle(FontStyle *fontStyle, QWidget *parent) : QWidget(parent)
{

    this->fontStyle = fontStyle;

    // Game Notation Settings
    QGroupBox *groupBoxGameNotationFontSize = new QGroupBox(tr("Game Notation Font Size"),this);

    this->radioGameNotationDefaultSize = new QRadioButton(tr("&System Default Font Size"));
    this->radioGameNotationcustomSize = new QRadioButton(tr("&Custom Size"));

    this->sizeBoxGameNotation = new QComboBox();

    QStringList fontSizeList = {"8", "10", "12", "14", "16", "20", "24", "36", "48"};
    sizeBoxGameNotation->addItems(fontSizeList);

    QGridLayout *gridGameNotationFontSize = new QGridLayout();

    gridGameNotationFontSize->addWidget(radioGameNotationDefaultSize, 0, 0);
    gridGameNotationFontSize->addWidget(radioGameNotationcustomSize, 1, 0);
    gridGameNotationFontSize->addWidget(sizeBoxGameNotation, 1, 1);

    groupBoxGameNotationFontSize->setLayout(gridGameNotationFontSize);
    groupBoxGameNotationFontSize->setTitle(("Game Notation Font Size"));

     // Engine Output Settings
    QGroupBox *groupBoxEngineOutFontSize = new QGroupBox(tr("Game Notation Font Size"),this);

    this->radioEngineOutDefaultSize = new QRadioButton(tr("&System Default Font Size"));
    this->radioEngineOutcustomSize = new QRadioButton(tr("&Custom Size"));

    this->sizeBoxEngineOut = new QComboBox(this);
    sizeBoxEngineOut->addItems(fontSizeList);

    QGridLayout *gridEngineOutFontSize = new QGridLayout();

    gridEngineOutFontSize->addWidget(radioEngineOutDefaultSize, 0, 0);
    gridEngineOutFontSize->addWidget(radioEngineOutcustomSize, 1, 0);
    gridEngineOutFontSize->addWidget(sizeBoxEngineOut, 1, 1);

    groupBoxEngineOutFontSize->setLayout(gridEngineOutFontSize);
    groupBoxEngineOutFontSize->setTitle(("Engine Output Font Size"));

    QVBoxLayout *mainLayout = new QVBoxLayout();
    mainLayout->addWidget(groupBoxGameNotationFontSize);
    mainLayout->addWidget(groupBoxEngineOutFontSize);
    mainLayout->addStretch(1);

    this->setLayout(mainLayout);

    if(fontStyle->moveWindowFontSize.isEmpty()) {
        radioGameNotationDefaultSize->setChecked(true);
    } else {
        radioGameNotationcustomSize->setChecked(true);
        sizeBoxGameNotation->setCurrentText(fontStyle->moveWindowFontSize);
    }

    if(fontStyle->engineOutFontSize.isEmpty()) {
        radioEngineOutDefaultSize->setChecked(true);
    } else {
        radioEngineOutcustomSize->setChecked(true);
        sizeBoxEngineOut->setCurrentText(fontStyle->engineOutFontSize);
    }


    connect(this->radioGameNotationcustomSize, &QRadioButton::toggled, this, &TabFontStyle::onSelectCustomGameNotationFontSize);
    connect(this->radioGameNotationDefaultSize, &QRadioButton::toggled, this, &TabFontStyle::onSelectDefaultGameNotationFontSize);
    connect(this->sizeBoxGameNotation, &QComboBox::currentTextChanged, this, &TabFontStyle::onSizeBoxGameNotationChange);

    connect(this->radioEngineOutcustomSize, &QRadioButton::toggled, this, &TabFontStyle::onSelectCustomEngineOutFontSize);
    connect(this->radioEngineOutDefaultSize, &QRadioButton::toggled, this, &TabFontStyle::onSelectDefaultEngineOutFontSize);
    connect(this->sizeBoxEngineOut, &QComboBox::currentTextChanged, this, &TabFontStyle::onSizeBoxEngineOutChange);

}

void TabFontStyle::onSelectDefaultGameNotationFontSize() {
    this->fontStyle->moveWindowFontSize = "";
}

void TabFontStyle::onSelectCustomGameNotationFontSize() {
    this->fontStyle->moveWindowFontSize = this->sizeBoxGameNotation->currentText();
}

void TabFontStyle::onSizeBoxGameNotationChange(const QString &text) {
    if(this->radioGameNotationcustomSize->isChecked()) {
        this->fontStyle->moveWindowFontSize = text;
    }
}

void TabFontStyle::onSelectDefaultEngineOutFontSize() {
    this->fontStyle->engineOutFontSize = "";
}

void TabFontStyle::onSelectCustomEngineOutFontSize() {
    this->fontStyle->engineOutFontSize = this->sizeBoxEngineOut->currentText();
}

void TabFontStyle::onSizeBoxEngineOutChange(const QString &text) {
    if(this->radioEngineOutcustomSize->isChecked()) {
        this->fontStyle->engineOutFontSize = text;
    }
}

