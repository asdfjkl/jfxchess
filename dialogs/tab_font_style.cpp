#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>
#include "tab_font_style.h"

TabFontStyle::TabFontStyle(QWidget *parent) : QWidget(parent)
{

    QGroupBox *groupBox = new QGroupBox(tr("Exclusive Radio Buttons"));

    QRadioButton *radio1 = new QRadioButton(tr("&Radio button 1"));
    QRadioButton *radio2 = new QRadioButton(tr("R&adio button 2"));
    QRadioButton *radio3 = new QRadioButton(tr("Ra&dio button 3"));

    radio1->setChecked(true);
    QVBoxLayout *vboxRadioButtons = new QVBoxLayout;
    vboxRadioButtons->addWidget(radio1);
    vboxRadioButtons->addWidget(radio2);
    vboxRadioButtons->addWidget(radio3);
    vboxRadioButtons->addStretch(1);
    groupBox->setLayout(vboxRadioButtons);

    QVBoxLayout *mainLayout = new QVBoxLayout();
    mainLayout->addWidget(groupBox);

    this->setLayout(mainLayout);
    //this->update();

}
