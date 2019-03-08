#ifndef RIBBON_TAB_H
#define RIBBON_TAB_H

#include <QWidget>
#include <QHBoxLayout>
#include "ribbon/ribbon_pane.h"

class RibbonTab : public QWidget
{
    Q_OBJECT

public:
    explicit RibbonTab(const QString &name, QWidget *parent = nullptr);
    RibbonPane* addRibbonPane(const QString &name);
    void addSpacer();

private:
    QHBoxLayout *layout;
    QString name;

signals:

public slots:
};

#endif // RIBBON_TAB_H
