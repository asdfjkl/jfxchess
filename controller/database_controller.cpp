#include "database_controller.h"
#include "dialogs/dialog_database.h"

DatabaseController::DatabaseController(QWidget *parent) :
    QObject(parent)
{

}

void DatabaseController::showDatabase() {

    DialogDatabase *ddb = new DialogDatabase();
    ddb->show();

}
