#ifndef RIBBON_BUTTON_H
#define RIBBON_BUTTON_H

#include <QToolButton>
#include <QAction>


const int ICON_SIZE_LARGE = 32;
const int ICON_SIZE_SMALL = 16;

class RibbonButton : public QToolButton
{
    Q_OBJECT

public:
    explicit RibbonButton(QAction *action, bool isLargeIcon, QWidget *parent = nullptr);

private:
    QAction *actionOwner;

private slots:
    void onActionChanged();

};

#endif // RIBBON_BUTTON_H
