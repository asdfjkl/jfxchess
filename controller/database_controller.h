#ifndef DATABASECONTROLLER_H
#define DATABASECONTROLLER_H

#include <QObject>
#include "model/game_model.h"

class DatabaseController : public QObject
{
    Q_OBJECT
public:
    explicit DatabaseController(GameModel *model, QWidget *parent = 0);

private:
    GameModel *gameModel;

signals:

public slots:

    void showDatabase();

};

#endif // DATABASECONTROLLER_H
