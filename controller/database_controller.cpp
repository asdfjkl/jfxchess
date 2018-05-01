#include "database_controller.h"
#include "dialogs/dialog_database.h"
#include <memory>

DatabaseController::DatabaseController(GameModel *model, QWidget *parent) :
    QObject(parent)
{
    this->gameModel = model;
    this->mainWindow = parent;
}

void DatabaseController::showDatabase() {

    DialogDatabase *ddb = new DialogDatabase(this->gameModel, this->mainWindow);
    if(ddb->exec() == QDialog::Accepted && ddb->selectedIndex >= 0) {
        std::unique_ptr <chess::Game> selected_game = this->gameModel->database->getGameAt(ddb->selectedIndex);
        this->gameModel->setGame(move(selected_game));
        this->gameModel->getGame()->setTreeWasChanged(true);
    }
    delete ddb;
    this->gameModel->triggerStateChange();
}
