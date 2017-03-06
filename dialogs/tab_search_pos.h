#ifndef TABSEARCHPOS_H
#define TABSEARCHPOS_H

#include <QWidget>
#include "model/game_model.h"

class TabSearchPos : public QWidget
{
    Q_OBJECT
public:
    explicit TabSearchPos(GameModel* model, QWidget *parent = 0);

signals:

public slots:
};

#endif // TABSEARCHPOS_H
