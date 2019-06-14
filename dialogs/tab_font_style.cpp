#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>
#include <QFontComboBox>
#include <QGridLayout>
#include "tab_font_style.h"

TabFontStyle::TabFontStyle(QWidget *parent) : QWidget(parent)
{

    // Game Notation Settings
    QGroupBox *groupBoxGameNotationFontSize = new QGroupBox(tr("Game Notation Font Size"),this);
    QGroupBox *groupBoxGameNotationFontType = new QGroupBox(tr("Game Notation Font Type"),this);

    QRadioButton *radioGameNotationDefaultFont = new QRadioButton(tr("&System Default Font Family"));
    QRadioButton *radioGameNotationCustomFont = new QRadioButton(tr("&Custom Font"));

    QRadioButton *radioGameNotationDefaultSize = new QRadioButton(tr("&System Default Font Size"));
    QRadioButton *radioGameNotationCustomtSize = new QRadioButton(tr("&Custom Size"));

    QFontComboBox *fontBoxGameNotation = new QFontComboBox(this);
    fontBoxGameNotation->setFontFilters(QFontComboBox::ScalableFonts);
    QComboBox *sizeBoxGameNotation = new QComboBox(this);

    QGridLayout *gridGameNotationFontSize = new QGridLayout();
    QGridLayout *gridGameNotationFontType = new QGridLayout();

    gridGameNotationFontSize->addWidget(radioGameNotationDefaultSize, 0, 0);
    gridGameNotationFontSize->addWidget(radioGameNotationCustomtSize, 1, 0);
    gridGameNotationFontSize->addWidget(sizeBoxGameNotation, 1, 1);

    gridGameNotationFontType->addWidget(radioGameNotationDefaultFont, 2, 0);
    gridGameNotationFontType->addWidget(radioGameNotationCustomFont, 3, 0);
    gridGameNotationFontType->addWidget(fontBoxGameNotation, 3, 1);

    groupBoxGameNotationFontSize->setLayout(gridGameNotationFontSize);
    groupBoxGameNotationFontSize->setTitle(("Game Notation Font Size"));

    groupBoxGameNotationFontType->setLayout(gridGameNotationFontType);
    groupBoxGameNotationFontType->setTitle(("Game Notation Font Type"));

    // Engine Output Settings
    QGroupBox *groupBoxEngineOutFontSize = new QGroupBox(tr("Game Notation Font Size"),this);
    QGroupBox *groupBoxEngineOutFontType = new QGroupBox(tr("Game Notation Font Type"),this);

    QRadioButton *radioEngineOutDefaultFont = new QRadioButton(tr("&System Default Font Family"));
    QRadioButton *radioEngineOutCustomFont = new QRadioButton(tr("&Custom Font"));

    QRadioButton *radioEngineOutDefaultSize = new QRadioButton(tr("&System Default Font Size"));
    QRadioButton *radioEngineOutCustomtSize = new QRadioButton(tr("&Custom Size"));

    QFontComboBox *fontBoxEngineOut = new QFontComboBox(this);
    fontBoxEngineOut->setFontFilters(QFontComboBox::ScalableFonts);
    QComboBox *sizeBoxEngineOut = new QComboBox(this);

    QGridLayout *gridEngineOutFontSize = new QGridLayout();
    QGridLayout *gridEngineOutFontType = new QGridLayout();

    gridEngineOutFontSize->addWidget(radioEngineOutDefaultSize, 0, 0);
    gridEngineOutFontSize->addWidget(radioEngineOutCustomtSize, 1, 0);
    gridEngineOutFontSize->addWidget(sizeBoxEngineOut, 1, 1);

    gridEngineOutFontType->addWidget(radioEngineOutDefaultFont, 2, 0);
    gridEngineOutFontType->addWidget(radioEngineOutCustomFont, 3, 0);
    gridEngineOutFontType->addWidget(fontBoxEngineOut, 3, 1);

    groupBoxEngineOutFontSize->setLayout(gridEngineOutFontSize);
    groupBoxEngineOutFontSize->setTitle(("Engine Output Font Size"));

    groupBoxEngineOutFontType->setLayout(gridEngineOutFontType);
    groupBoxEngineOutFontType->setTitle(("Engine Output Font Type"));

    QVBoxLayout *mainLayout = new QVBoxLayout();
    mainLayout->addWidget(groupBoxGameNotationFontSize);
    mainLayout->addWidget(groupBoxGameNotationFontType);

    mainLayout->addWidget(groupBoxEngineOutFontSize);
    mainLayout->addWidget(groupBoxEngineOutFontType);

    this->setLayout(mainLayout);
    //this->update();

}
