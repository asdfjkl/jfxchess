#ifndef RIBBON_SEPARATOR_H
#define RIBBON_SEPARATOR_H

#include <QWidget>

class RibbonSeparator : public QWidget
{
    Q_OBJECT

public:
    explicit RibbonSeparator(QWidget *parent = nullptr);

protected:
    void paintEvent(QPaintEvent *e);

};

#endif // RIBBON_SEPARATOR_H
