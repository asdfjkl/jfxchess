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

<<<<<<< HEAD
    QHBoxLayout *layoutText1 = new QHBoxLayout();
    QHBoxLayout *layoutText2 = new QHBoxLayout();
=======
    QHBoxLayout *layoutText1 = new QHBoxLayout(this);
    QHBoxLayout *layoutText2 = new QHBoxLayout(this);
>>>>>>> c3f979a4f133596d772483c0fbd1cac4a0bdc496

    QVBoxLayout *mainLayout = new QVBoxLayout(this);

    QLabel *lblText1 = new QLabel(tr("Text1:"), this);
    this->text1 = new QLineEdit(this);

    QLabel* lblText2 = new QLabel(tr("Text2:"), this);
    this->text2 = new QLineEdit(this);

    text1->setFixedWidth(len_surname);
    text2->setFixedWidth(len_surname);

    lblText1->setBuddy(text1);
    lblText2->setBuddy(text2);

    this->wholeWord = new QCheckBox(tr("Whole Word"), this);
    this->caseSensitive = new QCheckBox(tr("Case Sensitive"), this);
    this->notInitialPos = new QCheckBox(tr("must NOT start in intial position"), this);

    layoutText1->addWidget(lblText1);
    layoutText1->addWidget(text1);
    layoutText1->addStretch(1);

    layoutText2->addWidget(lblText2);
    layoutText2->addWidget(text2);
    layoutText2->addStretch(1);

    mainLayout->addLayout(layoutText1);
    mainLayout->addLayout(layoutText2);
    mainLayout->addSpacing(len_vertical_space);
    mainLayout->addWidget(notInitialPos);
    mainLayout->addWidget(caseSensitive);
    mainLayout->addWidget(wholeWord);
    mainLayout->addStretch(1);

    this->setLayout(mainLayout);
}

