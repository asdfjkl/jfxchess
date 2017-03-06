#ifndef DIALOGSEARCH_H
#define DIALOGSEARCH_H

#include <QDialog>
#include "model/game_model.h"

class DialogSearch : public QDialog
{
    Q_OBJECT

public:
    explicit DialogSearch(GameModel *gameModel, QWidget *parent = 0);

};

#endif // DIALOGSEARCH_H
