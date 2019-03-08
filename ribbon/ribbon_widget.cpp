#include "ribbon_widget.h"
#include "various/stylesheet.h"

RibbonWidget::RibbonWidget(QWidget *parent) :
    QToolBar (parent)
{
    //self.setStyleSheet(get_stylesheet("ribbon"))

    QString strRibbon("ribbon");
    QString style = StyleSheet::getInstance().getStylesheet(strRibbon);
    this->setStyleSheet(style);

    this->setObjectName("ribbonWidget");
    this->setWindowTitle("Ribbon");
    this->tabWidget = new QTabWidget(this);

    qreal ratio = this->devicePixelRatio();

    this->tabWidget->setMaximumHeight(120 * ratio);
    this->tabWidget->setMinimumHeight(110 * ratio);
    this->setMovable(false);
    this->addWidget(this->tabWidget);
}

RibbonTab* RibbonWidget::addRibbonTab(const QString &name) {

    RibbonTab *rt = new RibbonTab(name, this);
    rt->setObjectName(QString("tab_").append(name));
    this->tabWidget->addTab(rt, name);
    return rt;
}


void RibbonWidget::setActive(const QString &name) {

    QString childName = QString("tab_").append(name);
    this->tabWidget->setCurrentWidget(this->findChild<QWidget*>(childName));
}
