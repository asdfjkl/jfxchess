#include "ribbon_separator.h"
#include <QHBoxLayout>
#include <QPainter>
#include <QPaintEvent>

RibbonSeparator::RibbonSeparator(QWidget *parent) :
    QWidget(parent)
{

    qreal ratio = this->devicePixelRatio();

    this->setMinimumHeight(80 * ratio);
    this->setMaximumHeight(80 * ratio);
    this->setMinimumWidth(1);
    this->setMaximumWidth(1);
    this->setLayout(new QHBoxLayout());

}

void RibbonSeparator::paintEvent(QPaintEvent *e) {

    QPainter painter;
    painter.begin(this);
    painter.fillRect(e->rect(), Qt::lightGray);
    painter.end();

}
