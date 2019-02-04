#ifndef DIALOG_DATABASE_H
#define DIALOG_DATABASE_H

#include <QDialog>
#include <QTableView>
#include <QTableWidget>
#include "model/game_model.h"
#include "viewController/database_index_model.h"

class DialogDatabase : public QDialog
{
    Q_OBJECT
public:
    explicit DialogDatabase(GameModel *gameModel, QWidget *parent);
    int selectedIndex;

private:
    void resizeTo(float ratio);
    void drawAllItems();

    QTableView *tableView;

    QTableWidget *gameTable;
    QStringList *gameTableHeaders;
    QPushButton *btnOpenGame;
    QPushButton *btnCancel;

    GameModel *gameModel;

    QAction *tbActionDeleteGame;

    DatabaseIndexModel *indexModel;

    int currentOpenDBType;

signals:

public slots:

    void onClickNew();
    void onClickAppend();
    void onClickSearch();
    void onClickExport();
    void onClickOpen();
    void onClickReset();
    void onRowChanged();
    void showHelp();


};

#endif // DIALOG_DATABASE_H
