#ifndef DIALOGDATABASE_H
#define DIALOGDATABASE_H

#include <QDialog>
#include <QTableWidget>
#include "model/game_model.h"
#include "viewController/database_index_model.h"

class DialogDatabase : public QDialog
{
    Q_OBJECT
public:
    explicit DialogDatabase(GameModel *gameModel, QWidget *parent = 0);

private:
    void resizeTo(float ratio);
    void drawAllItems();

    QTableWidget *gameTable;
    QStringList *gameTableHeaders;
    QPushButton *btnOpenGame;
    QPushButton *btnCancel;

    GameModel *gameModel;

    QTableView *tableView;
    DatabaseIndexModel *indexModel;

signals:

public slots:

    void onClickSearch();
    void onClickOpen();

};


#endif // DIALOGDATABASE_H
