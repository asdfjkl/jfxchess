#ifndef DIALOGDATABASE_H
#define DIALOGDATABASE_H

#include <QDialog>
#include <QTableWidget>
#include "chess/dci_database.h"
#include "model/game_model.h"
#include "viewController/database_index_model.h"
#include "dialogs/dialog_exportdatabase.h"

class DialogDatabase : public QDialog
{
    Q_OBJECT
public:
    explicit DialogDatabase(GameModel *gameModel, QWidget *parent);
    int selectedIndex;

    const int DATABASE_TYPE_DCI = 0;
    const int DATABASE_TYPE_PNG = 1;

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

    void onClickSearch();
    void onClickExport();
    void onClickOpen();
    void onRowChanged();

};


#endif // DIALOGDATABASE_H
