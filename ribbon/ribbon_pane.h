#ifndef RIBBON_PANE_H
#define RIBBON_PANE_H

#include <QWidget>
#include <QHBoxLayout>

class RibbonPane : public QWidget
{
    Q_OBJECT

public:
    explicit RibbonPane(const QString &name, QWidget *parent = nullptr);
    void addRibbonWidget(QWidget *widget);
    QGridLayout* addGridWidget(int width);

private:
    QHBoxLayout *contentLayout;

};

#endif // RIBBON_PANE_H
