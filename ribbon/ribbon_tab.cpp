#include "ribbon_tab.h"

RibbonTab::RibbonTab(const QString &name, QWidget *parent) :
    QWidget(parent)
{
    this->layout = new QHBoxLayout();
    this->setLayout(layout);
    this->layout->setContentsMargins(0, 0, 0, 0);
    this->layout->setSpacing(0);
    this->layout->setAlignment(Qt::AlignLeft);
    this->name = name;
}


RibbonPane* RibbonTab::addRibbonPane(const QString &name) {

    RibbonPane *pane = new RibbonPane(name, this);
    this->layout->addWidget(pane);
    return pane;
}


void RibbonTab::addSpacer() {

    this->layout->addSpacerItem(new QSpacerItem(1, 1, QSizePolicy::MinimumExpanding));
    this->layout->setStretch(this->layout->count() - 1, 1);
}
