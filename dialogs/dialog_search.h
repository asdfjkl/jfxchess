#ifndef DIALOGSEARCH_H
#define DIALOGSEARCH_H

#include <QDialog>
#include "model/game_model.h"
#include "model/search_pattern.h"
#include "tab_comment_search.h"
#include "tab_header_search.h"
#include "tab_search_pos.h"

class DialogSearch : public QDialog
{
    Q_OBJECT

public:
    explicit DialogSearch(GameModel *gameModel, QWidget *parent = 0);
    SearchPattern* getPattern();

private:
    SearchPattern *pattern;
    TabHeaderSearch* ths;
    TabCommentSearch *tcs;
    TabSearchPos *tsp;

    QCheckBox *optGameData;
    QCheckBox *optComments;
    QCheckBox *optPosition;
    QCheckBox *optVariants;

protected:
    void resizeEvent(QResizeEvent *re);

};

#endif // DIALOGSEARCH_H
