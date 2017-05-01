#include "database_controller.h"
#include "dialogs/dialog_database.h"

DatabaseController::DatabaseController(GameModel *model, QWidget *parent) :
    QObject(parent)
{
    this->gameModel = model;
    this->mainWindow = parent;
}

void DatabaseController::showDatabase() {

    DialogDatabase *ddb = new DialogDatabase(this->gameModel, this->mainWindow);
    if(ddb->exec() == QDialog::Accepted && ddb->selectedIndex >= 0) {
        chess::Game *selected_game = this->gameModel->database->getGameAt(ddb->selectedIndex);
        this->gameModel->setGame(selected_game);
        this->gameModel->getGame()->treeWasChanged = true;
    }
    delete ddb;
    this->gameModel->triggerStateChange();
}
