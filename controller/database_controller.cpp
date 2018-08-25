#include "database_controller.h"
#include "dialogs/dialog_database.h"
#include <memory>
#include <QDebug>

DatabaseController::DatabaseController(GameModel *model, QWidget *parent) :
    QObject(parent)
{
    this->gameModel = model;
    this->mainWindow = parent;
}

void DatabaseController::showDatabase() {

    DialogDatabase *ddb = new DialogDatabase(this->gameModel, this->mainWindow);
    qDebug() << "show database";
    if(ddb->exec() == QDialog::Accepted && ddb->selectedIndex >= 0) {
        qDebug() << "insided dialog acc";
        std::unique_ptr <chess::Game> selected_game = this->gameModel->database->getGameAt(ddb->selectedIndex);
        this->gameModel->setGame(move(selected_game));
        this->gameModel->getGame()->setTreeWasChanged(true);
    } else {
        qDebug() << "exec not ok, because:";
        //qDebug() << "dbb exec: " << +(ddb->exec() == QDialog::Accepted);
        qDebug() << "selected index: " << ddb->selectedIndex;
        if(ddb->selectedIndex >= 0) {
            qDebug() << "qdialog not accepted";
        } else {
            qDebug() << "dialog accpeted";
        }
    }
    delete ddb;
    this->gameModel->triggerStateChange();
}
