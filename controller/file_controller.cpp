/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#include "file_controller.h"
#include <QtPrintSupport/QPrinter>
#include <QtPrintSupport/QPrintDialog>
#include <QPlainTextEdit>
#include <QFileDialog>
#include <QPainter>
#include <QDebug>
#include <iostream>
#include "chess/pgn_printer.h"
#include "chess/pgn_reader.h"
#include "dialogs/dialog_newgame.h"
#include "dialogs/dialog_browseheaders.h"
#include "dialogs/dialog_database.h"
#include "various/messagebox.h"

FileController::FileController(GameModel *gameModel, QWidget *parent) :
    QObject(parent)
{
    this->gameModel = gameModel;
    this->parentWidget = parent;

}



void FileController::printGame() {
    QPrinter printer;
    QPrintDialog *dlg = new QPrintDialog(&printer, this->parentWidget);
    dlg->setWindowTitle(tr("Print Game"));
    if(dlg->exec() == QDialog::Accepted) {
        chess::PgnPrinter *pgnPrinter = new chess::PgnPrinter();
        QString pgn = pgnPrinter->printGame(*this->gameModel->getGame()).join("\n");
        QPlainTextEdit *textEdit = new QPlainTextEdit(pgn);
        textEdit->print(&printer);
        delete textEdit;
        delete pgnPrinter;
    }
    delete dlg;
}

void FileController::printPosition() {
    QPrinter printer;
    QPrintDialog *dlg = new QPrintDialog(&printer, this->parentWidget);
    dlg->setWindowTitle(tr("Print FEN"));
    if(dlg->exec() == QDialog::Accepted) {
        QString fen = this->gameModel->getGame()->getCurrentNode()->getBoard().fen();
        QPlainTextEdit *textEdit = new QPlainTextEdit(fen);
        textEdit->print(&printer);
        delete textEdit;
    }
    delete dlg;
}

/*
 *     def print_position(self):
        q_widget = self.mainAppWindow
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            p = QPixmap.grabWindow(q_widget.chessboard_view.winId())
            painter = QPainter(dialog.printer())
            dst = QRect(0,0,200,200)
            painter.drawPixmap(dst, p)
            del painter
 *
 */
void FileController::newGame() {

    // internal engine is always at 0, so otherwise we have a custom engine
    bool customEngine = this->gameModel->getActiveEngineIdx() != 0;
    DialogNewGame* dlg = new DialogNewGame(customEngine, this->gameModel->getEngineStrength(),
                                           this->gameModel->getEngineThinkTime(), this->parentWidget);
    if(dlg->exec() == QDialog::Accepted) {
        this->gameModel->wasSaved = false;
        this->gameModel->engineStrength = dlg->computerStrength;
        this->gameModel->engineThinkTimeMs = dlg->computerThinkTime*1000;
        //auto g = std::unique_ptr<chess::Game>(new chess::Game());
        chess::Game* g = new chess::Game();
        this->gameModel->setGame(g);
        this->gameModel->getGame()->setTreeWasChanged(true);
        if(dlg->playsWhite) {
            this->gameModel->flipBoard = false;
        } else {
            this->gameModel->flipBoard = true;
        }
        if(dlg->playsComputer) {
            if(dlg->playsWhite) {
                emit(newGamePlayWhite());
            } else {
                emit(newGamePlayBlack());
            }
        } else {
            emit(newGameEnterMoves());
        }
    }
    delete dlg;
}

void FileController::openGame() {

    QString filename = QFileDialog::getOpenFileName(this->parentWidget,
        tr("Open Game File"), this->gameModel->lastOpenDir, tr("PGN Files (*.pgn)"));
    if(!filename.isEmpty()) {
    QDir dir = QDir::root();
    QString path = dir.absoluteFilePath(filename);
        this->openGameFrom(path, filename, false);
    }
}

void FileController::openGameFrom(QString &path, QString &absoluteFilename, bool reOpen) {
    chess::PgnReader reader;
    //qDebug() << "open Game From";
    try {
        this->gameModel->database.open(absoluteFilename);
        if(this->gameModel->database.countGames() == 1) {
            //qDebug() << "count Games";
            chess::Game* onlyGame = this->gameModel->database.getGameAt(0);
            //qDebug() << "only game loaded";
            this->gameModel->lastOpenDir = path;
            this->gameModel->currentPgnFilename = absoluteFilename;
            this->gameModel->lastSaveFilename = absoluteFilename;
            // setup new game triggers statechange, so no need to call
            this->setupNewGame(onlyGame);
        } else if(this->gameModel->database.countGames() > 1) {
            //qDebug() << "count >= 1 for database load";
            DialogDatabase dlg(this->gameModel, this->parentWidget);
            if(dlg.exec() == QDialog::Accepted && dlg.selectedIndex >= 0) {
                    //qDebug() << "getting game at: " << dlg.selectedIndex;
                    chess::Game* selected_game = this->gameModel->database.getGameAt(dlg.selectedIndex);
                    this->gameModel->lastOpenDir = path;
                    this->gameModel->currentPgnFilename = absoluteFilename;
                    this->gameModel->lastSaveFilename = absoluteFilename;
                    // setup new game triggers statechange, so no need to call
                    this->setupNewGame(selected_game);
            } else {
                //qDebug() << "dlg selected index: " << dlg.selectedIndex;
            }
        }
    } catch(std::exception e) {
        std::cerr << e.what() << std::endl;
    }
}

void FileController::setupNewGame(chess::Game* g) {
    //delete this->gameModel->getGame();
    this->gameModel->setGame(g);
    this->gameModel->getGame()->setTreeWasChanged(true);
    this->gameModel->triggerStateChange();
}

void FileController::saveGame() {
    this->saveGameTo(this->gameModel->lastSaveFilename);
}

void FileController::saveGameTo(QString &filename) {
    chess::PgnPrinter *pgn = new chess::PgnPrinter();
    try {
        pgn->writeGame(*this->gameModel->getGame(), filename);
    } catch (std::exception &e) {
        this->gameModel->wasSaved = false;
        MessageBox *msg = new MessageBox(this->parentWidget);
        msg->showMessage("Error", e.what());
        delete msg;
        this->gameModel->triggerStateChange();
    }
}

void FileController::toolbarSaveGame() {
    if(this->gameModel->wasSaved) {
        this->saveGame();
    } else {
        this->saveAsNewGame();
    }
}

void FileController::saveAsNewGame() {
    QString filename = QFileDialog::getSaveFileName(this->parentWidget,
        tr("Open Game File"), this->gameModel->lastSaveDir, tr("PGN Files (*.pgn)"));
    QDir dir = QDir::root();
    QString path = dir.absoluteFilePath(filename);
    if(!filename.isEmpty()) {
        try {
            this->saveGameTo(filename);
            this->gameModel->wasSaved = true;
            this->gameModel->lastSaveDir = path;
            this->gameModel->lastSaveFilename = filename;
            this->gameModel->triggerStateChange();
        } catch(std::exception e) {
            std::cerr << e.what() << std::endl;
        }
    }
}

void FileController::toolbarOpenCurrentPGN()  {
    this->openGameFrom(this->gameModel->lastOpenDir, this->gameModel->currentPgnFilename, true);
}

void FileController::openInCurrentPgnAt(int idx) {

    QString absoluteFilename = this->gameModel->currentPgnFilename;
    QString path = this->gameModel->lastOpenDir;

    chess::PgnReader reader;
    try {
        QFile file;
        file.setFileName(absoluteFilename);
        file.open(QIODevice::ReadOnly);
        quint64 size = file.size();
        file.close();
        if(size > 1048576) {
            MessageBox *msg = new MessageBox(this->parentWidget);
            msg->showMessage("Error Opening File", ("PGN files larger than 1 MB are not supported."));
            delete msg;
        } else {
            const char* encoding = reader.detect_encoding(absoluteFilename);
            QString complete_file = reader.readFileIntoString(absoluteFilename, encoding);
            QList<chess::HeaderOffset> header_offsets = reader.scan_headersFromString(complete_file);
            if(idx >= 0 && idx < header_offsets.size()) {
                qint64 gameOffset = header_offsets.at(idx).offset;
                chess::Game* g = reader.readGameFromString(complete_file, gameOffset);
                this->gameModel->wasSaved = false;
                this->gameModel->lastOpenDir = path;
                this->gameModel->database.setLastSelectedIndex(idx);
                this->gameModel->lastSaveFilename = QString("");
                this->setupNewGame(g);
                }
            }
        }
    catch(std::exception e) {
        std::cerr << e.what() << std::endl;
    }
}

void FileController::openDatabase() {

    DialogDatabase *dlg = new DialogDatabase(this->gameModel, this->parentWidget);    
    if(dlg->exec() == QDialog::Accepted && dlg->selectedIndex >= 0) {
            chess::Game* selected_game = this->gameModel->database.getGameAt(dlg->selectedIndex);
            this->gameModel->database.setLastSelectedIndex(dlg->selectedIndex);
            this->gameModel->setGame(selected_game);
            this->gameModel->getGame()->setTreeWasChanged(true);
        }
    delete dlg;
    this->gameModel->triggerStateChange();

}

void FileController::toolbarNextGameInPGN() {
    int idx = this->gameModel->database.getLastSelectedIndex() + 1;
    if(idx < this->gameModel->database.countGames()) {
        chess::Game* selected_game = this->gameModel->database.getGameAt(idx);
        //this->gameModel->database.setLastSelectedIndex(idx);
        this->gameModel->setGame(selected_game);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    }
}

void FileController::toolbarPrevGameInPGN() {
    int idx = this->gameModel->database.getLastSelectedIndex() - 1;
    if(idx >= 0) {
        chess::Game* selected_game = this->gameModel->database.getGameAt(idx);
        //this->gameModel->database.setLastSelectedIndex(idx);
        this->gameModel->setGame(selected_game);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    }
}
