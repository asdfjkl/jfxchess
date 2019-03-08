#include "ribbon_pane.h"
#include "ribbon_separator.h"
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QLabel>
#include "various/stylesheet.h"

RibbonPane::RibbonPane(const QString &name, QWidget *parent) :
    QWidget(parent)
{
    // self.setStyleSheet(get_stylesheet("ribbonPane"))

    QString strRibbonPane("ribbonPane");
    QString style = StyleSheet::getInstance().getStylesheet(strRibbonPane);
    this->setStyleSheet(style);

    QHBoxLayout *horizontalLayout = new QHBoxLayout();
    horizontalLayout->setSpacing(0);
    horizontalLayout->setContentsMargins(0, 0, 0, 0);

    this->setLayout(horizontalLayout);

    QWidget *verticalWidget = new QWidget(this);

    horizontalLayout->addWidget(verticalWidget);
    horizontalLayout->addWidget(new RibbonSeparator(this));

    QVBoxLayout *verticalLayout = new QVBoxLayout();
    verticalLayout->setSpacing(0);
    verticalLayout->setContentsMargins(0, 0, 0, 0);

    verticalWidget->setLayout(verticalLayout);

    QLabel *label = new QLabel(this);
    label->setText(name);
    label->setAlignment(Qt::AlignCenter);
    label->setStyleSheet("color:#666;");

    QWidget *contentWidget = new QWidget(this);

    verticalLayout->addWidget(contentWidget);
    verticalLayout->addWidget(label);

    QHBoxLayout *contentLayout = new QHBoxLayout();
    contentLayout->setAlignment(Qt::AlignLeft);
    contentLayout->setSpacing(0);
    contentLayout->setContentsMargins(0, 0, 0, 0);

    this->contentLayout = contentLayout;
    contentWidget->setLayout(contentLayout);

}


void RibbonPane::addRibbonWidget(QWidget *widget) {
    this->contentLayout->addWidget(widget, 0, Qt::AlignTop);
}


QGridLayout* RibbonPane::addGridWidget(int width) {

    QWidget *widget = new QWidget(this);
    widget->setMaximumWidth(width);

    QGridLayout *gridLayout = new QGridLayout();
    widget->setLayout(gridLayout);
    gridLayout->setSpacing(4);
    gridLayout->setContentsMargins(4, 4, 4, 4);

    this->contentLayout->addWidget(widget);
    gridLayout->setAlignment(Qt::AlignTop | Qt::AlignLeft);

    return gridLayout;
}
