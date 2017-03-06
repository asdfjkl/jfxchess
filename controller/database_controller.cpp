#include "database_controller.h"
#include "dialogs/dialog_database.h"

DatabaseController::DatabaseController(GameModel *model, QWidget *parent) :
    QObject(parent)
{
    this->gameModel = model;
}

void DatabaseController::showDatabase() {

    DialogDatabase *ddb = new DialogDatabase(this->gameModel);
    ddb->show();

}
