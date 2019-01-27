#ifndef DIALOG_SEARCH_H
#define DIALOG_SEARCH_H

#include <QDialog>
#include <QCheckBox>
#include "model/game_model.h"
#include "model/search_pattern.h"
#include "dialogs/tab_header_search.h"


class DialogSearch : public QDialog
{
    Q_OBJECT
public:
    explicit DialogSearch(GameModel *gameModel, QWidget *parent = nullptr);
    SearchPattern getPattern();

private:
    TabHeaderSearch* ths;

    QCheckBox *optGameData;
    //QCheckBox *optComments;
    //QCheckBox *optPosition;
    //QCheckBox *optVariants;

protected:
    void resizeEvent(QResizeEvent *re);

signals:

public slots:
};

#endif // DIALOG_SEARCH_H
