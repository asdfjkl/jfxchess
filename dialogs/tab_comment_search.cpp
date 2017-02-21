#include "tab_comment_search.h"
#include <QLineEdit>
#include <QLabel>
#include <QCheckBox>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QSpinBox>
#include <QRadioButton>
#include <QButtonGroup>
#include <QPushButton>

TabCommentSearch::TabCommentSearch(QWidget *parent) : QWidget(parent)
{
    QFontMetrics f = this->fontMetrics();
    int len_surname = f.width(this->tr("abcdefghjlmnopqrstuvw"));
    int len_vertical_space = f.height();

    QHBoxLayout *layoutText1 = new QHBoxLayout();
    QHBoxLayout *layoutText2 = new QHBoxLayout();

    QVBoxLayout *mainLayout = new QVBoxLayout();

    QLabel *lblText1 = new QLabel(tr("Text1:"));
    QLineEdit* text1 = new QLineEdit(this);

    QLabel* lblText2 = new QLabel(tr("Text2:"));
    QLineEdit* text2 = new QLineEdit(this);

    text1->setFixedWidth(len_surname);
    text2->setFixedWidth(len_surname);

    lblText1->setBuddy(text1);
    lblText2->setBuddy(text2);

    QCheckBox *wholeWord = new QCheckBox(tr("Whole Word"));

    layoutText1->addWidget(lblText1);
    layoutText1->addWidget(text1);
    layoutText1->addSpacing(len_vertical_space);
    layoutText1->addWidget(wholeWord);
    layoutText1->addStretch(1);

    layoutText2->addWidget(lblText2);
    layoutText2->addWidget(text2);
    layoutText2->addStretch(1);

    QCheckBox *notInitialPos = new QCheckBox(tr("must NOT start in intial position"));
    QCheckBox *arrows = new QCheckBox(tr("must contain arrows"));
    QCheckBox *colorFields = new QCheckBox(tr("must contain colored fields"));

    mainLayout->addLayout(layoutText1);
    mainLayout->addLayout(layoutText2);
    mainLayout->addSpacing(len_vertical_space);
    mainLayout->addWidget(notInitialPos);
    mainLayout->addWidget(arrows);
    mainLayout->addWidget(colorFields);
    mainLayout->addStretch(1);

    this->setLayout(mainLayout);
}

