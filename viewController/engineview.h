#ifndef ENGINEVIEW_H
#define ENGINEVIEW_H

#include <QTextEdit>
#include "model/game_model.h"
#include "uci/engine_info.h"

class EngineView : public QTextEdit
{
    Q_OBJECT
public:
    explicit EngineView(GameModel *gameModel, QWidget *parent = 0);

private:
    QString lastInfo;
    GameModel *gameModel;

signals:

public slots:
    void onNewInfo(QString info);
    void onStateChange();
    void flipShowEval();

};

#endif // ENGINEVIEW_H
