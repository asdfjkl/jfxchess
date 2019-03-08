#ifndef RIBBON_WIDGET_H
#define RIBBON_WIDGET_H

#include <QToolBar>
#include <QTableWidget>
#include "ribbon/ribbon_tab.h"

class RibbonWidget : public QToolBar
{
    Q_OBJECT

public:
    explicit RibbonWidget(QWidget *parent = nullptr);
    RibbonTab* addRibbonTab(const QString &name);
    void setActive(const QString &name);

private:
    QTabWidget *tabWidget;

};

#endif // RIBBON_WIDGET_H
