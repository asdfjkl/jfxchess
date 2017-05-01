#ifndef DATABASECONTROLLER_H
#define DATABASECONTROLLER_H

#include <QObject>
#include "model/game_model.h"

class DatabaseController : public QObject
{
    Q_OBJECT
public:
    explicit DatabaseController(GameModel *model, QWidget *parent);

private:
    GameModel *gameModel;
    QWidget *mainWindow;

signals:

public slots:

    void showDatabase();

};

#endif // DATABASECONTROLLER_H
