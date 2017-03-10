#ifndef DIALOGSEARCH_H
#define DIALOGSEARCH_H

#include <QDialog>
#include "model/game_model.h"
#include "model/search_pattern.h"

class DialogSearch : public QDialog
{
    Q_OBJECT

public:
    explicit DialogSearch(GameModel *gameModel, QWidget *parent = 0);
    SearchPattern* getPattern();

private:
    SearchPattern *pattern;

private slots:
    void updatePattern();

};

#endif // DIALOGSEARCH_H
