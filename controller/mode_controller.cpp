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


#include "mode_controller.h"
#include "dialogs/dialog_guioptions.h"
#include "uci/uci_controller.h"
#include <QDebug>
#include "dialogs/dialog_engines.h"
#include "dialogs/dialog_gameanalysis.h"
#include "various/messagebox.h"
#include "chess/game_node.h"
#include "chess/game.h"
#include <stdlib.h>
#ifdef __APPLE__
#include <math.h>
#endif

ModeController::ModeController(GameModel *gameModel, UciController *controller, QWidget *parent) :
    QObject(parent)
{
    this->uci_controller = controller;
    this->gameModel = gameModel;
    this->parentWidget = parent;
}


void ModeController::onBestPv(QString pvs) {
    if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        this->gameModel->currentBestPv = pvs;
    }
}

void ModeController::onMateDetected(int mateIn) {
    if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        this->gameModel->currentMateInMoves = mateIn;
    }
}

void ModeController::onEval(float eval) {
    if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        this->gameModel->currentEval = eval;
    }
}

void ModeController::onBestMove(QString uci_move) {
    if(this->gameModel->getMode() == MODE_PLAY_BLACK ||
            this->gameModel->getMode() == MODE_PLAY_WHITE ||
            this->gameModel->getMode() == MODE_PLAYOUT_POS) {
        chess::Move m = chess::Move(uci_move);
        chess::Board b = this->gameModel->getGame()->getCurrentNode()->getBoard();
        if(b.is_legal_move(m)) {
            this->gameModel->getGame()->applyMove(m);
            this->gameModel->triggerStateChange();
        }
    }
    if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        // completely skip that for black or white, if
        // that was chosen in the analysis
        chess::GameNode *current = this->gameModel->getGame()->getCurrentNode();
        bool turn = current->getBoard().turn;
        int analyse = this->gameModel->gameAnalysisForPlayer;
        if( (analyse == ANALYSE_BOTH_PLAYERS) ||
            (analyse==ANALYSE_WHITE_ONLY && turn == chess::WHITE) ||
            (analyse==ANALYSE_BLACK_ONLY && turn == chess::BLACK)) {
        // check the result of the current analysis
        // first check if the first move in the bestpv
        // of this node is the same move as the first
        // child move
        //qDebug() << "at node: " << current->getMove()->uci();
        //qDebug() << "best pv: " << this->gameModel->currentBestPv;
        //qDebug() << "current eval: " << this->gameModel->currentEval;
        //qDebug() << "current mate: " << this->gameModel->currentMateInMoves;
        //qDebug() << "prev best pv: " << this->gameModel->prevBestPv;
        //qDebug() << "prev eval: " << this->gameModel->prevEval;
        //qDebug() << "prev mate: " << this->gameModel->prevMateInMoves;
        QString first_move_current_pv = this->gameModel->currentBestPv.split(" ")[0];
        if(!first_move_current_pv.isEmpty() && !current->isLeaf()
                && first_move_current_pv != current->getVariation(0)->getMove().uci()) {
            if( ((abs(this->gameModel->currentEval - this->gameModel->prevEval) > this->gameModel->analysisThreshold)
                    && this->gameModel->prevMateInMoves < 0 && this->gameModel->currentMateInMoves < 0)
                    || (this->gameModel->prevMateInMoves < 0 && this->gameModel->currentMateInMoves > 0) // player missed mate
                    || (this->gameModel->prevMateInMoves > 0 && this->gameModel->currentMateInMoves < 0)) { // player allowed mate
            // add best pv variation
            QStringList pv_list = this->gameModel->currentBestPv.split(" ");
            for(int i=0;i<pv_list.count();i++) {
                QString uci = pv_list.at(i);
                if(uci.size() == 4 || uci.size() == 5) {
                    chess::Move mi = chess::Move(uci);

                    chess::Board current_board = current->getBoard();
                     // chess::Board *new_board = current_board.copy_and_apply(mi);


                    //chess::Board new_board = chess::Board(current);
                    //new_board.apply(mi);
                    current_board.apply(mi);

                    chess::GameNode *gn = new chess::GameNode();
                    gn->setBoard(current_board);
                    gn->setMove(mi);
                    current->addVariation(gn);
                    //qDebug() << "current board nr: " << current_board.fullmove_number;
                    //qDebug() << "new board: " << new_board.fullmove_number;
                    //qDebug() << "gn board nr:" << new_board->fullmove_number;
                    //qDebug() << "node san: " << gn->getSan();
                    current = gn;
                    chess::Board current_board_temp = current->getBoard();
                    //qDebug() << "after setting: " << current_board_temp.fullmove_number;
                    //qDebug() << " ";

                }
            }
            current = this->gameModel->getGame()->getCurrentNode();
            // set the evals as a comment
            if(current->getVariations().count() >= 2) {
                if(this->gameModel->prevMateInMoves < 0 && this->gameModel->currentMateInMoves > 0) {
                    // player missed a mate
                    //qDebug() << "missed mate: " << this->gameModel->prevEval;
                    QString c0 = QString::number(this->gameModel->prevEval, 'f', 2);
                    current->getVariation(0)->setComment(c0);
                    // skip variation comment, if we have mate in 0
                    if(this->gameModel->currentMateInMoves-1 > 0) {
                        QString c1 = QString("#").append(QString::number(this->gameModel->currentMateInMoves-1));
                        current->getVariation(1)->setComment(c1);
                    }
                } else if(this->gameModel->prevMateInMoves >= 0 && this->gameModel->currentMateInMoves < 0) {
                    // player allowed a mate against him
                    QString c0 = QString::number(this->gameModel->currentEval, 'f', 2);
                    QString c1 = QString("#").append(QString::number(this->gameModel->prevMateInMoves));
                    current->getVariation(0)->setComment(c1);
                    current->getVariation(1)->setComment(c0);
                } else if(this->gameModel->prevMateInMoves < 0 && this->gameModel->currentMateInMoves < 0){
                QString c0 = QString::number(this->gameModel->prevEval, 'f', 2);
                QString c1 = QString::number(this->gameModel->currentEval, 'f', 2);
                current->getVariation(0)->setComment(c0);
                current->getVariation(1)->setComment(c1);
                }
            }
            this->gameModel->getGame()->setTreeWasChanged(true);
            }
        }
        }
        this->gameModel->prevBestPv = this->gameModel->currentBestPv;
        this->gameModel->prevEval = this->gameModel->currentEval;
        this->gameModel->prevMateInMoves = this->gameModel->currentMateInMoves;
        this->gameModel->currentBestPv = QString("");
        this->gameModel->currentEval = -10000;
        this->gameModel->currentMateInMoves = -1;
        this->gameModel->triggerStateChange();
    }
}


void ModeController::onSetEnginesClicked() {
    // temp. stop current mode, reset after
    // user finished selecting engines
    this->gameModel->setMode(MODE_ENTER_MOVES);
    this->onActivateEnterMovesMode();
    this->gameModel->triggerStateChange();
    DialogEngines *dlg = new DialogEngines(this->gameModel);
    int result = dlg->exec();
    if(result == QDialog::Accepted) {
        // replace engine list with dialog list
        this->gameModel->setEngines(dlg->engines);
        this->gameModel->setActiveEngine(dlg->activeEngineIdx);
        // this->gameModel->triggerStateChange();
    }
    this->gameModel->setLastAddedEnginePath(dlg->lastAddedEnginePath);
    delete dlg;
    // reset to prev. mode user was in before tampering
    // with engines
    /*
    if(currentMode == MODE_PLAY_WHITE) {
        this->onActivatePlayWhiteMode();
    } else if(currentMode == MODE_PLAY_BLACK){
        this->onActivatePlayBlackMode();
    } else if(currentMode == MODE_ANALYSIS) {
        this->onActivateAnalysisMode();
    } else if(currentMode == MODE_ANALYSIS) {
        this->onActivateGameAnalysisMode();
    }*/

}

void ModeController::onOptionsClicked() {
    DialogGuiOptions *dlg = new DialogGuiOptions(this->gameModel->colorStyle, this->parentWidget);
    int result = dlg->exec();
    if(result == QDialog::Accepted) {
        //this->gameModel->colorStyle = dlg->displayBoard->style;
        this->gameModel->colorStyle = dlg->displayBoard->getColorStyle();
        this->gameModel->triggerStateChange();
    }
}

void ModeController::onStateChangeEnterMoves() {
    // not much to do here...
}

void ModeController::onStateChangeAnalysis() {

    QString fen = this->gameModel->getGame()->getCurrentNode()->getBoard().fen();
    this->uci_controller->uciSendCommand("stop");
    this->uci_controller->uciSendFen(fen);
    QString position = QString("position fen ").append(fen);
    this->uci_controller->uciSendPosition(position);
    this->uci_controller->uciGoInfinite();

}

void ModeController::onStateChangeGameAnalysis() {

    MessageBox *msg = new MessageBox(this->parentWidget);
    // go to the parent node, if there is any. if root, then abort
    chess::GameNode *parent = this->gameModel->getGame()->getCurrentNode()->getParent();
    if(parent == 0) {
        this->gameModel->setMode(MODE_ENTER_MOVES);
        msg->showMessage(tr("Game Analysis"), tr("The analysis is finished."));
    } else if(this->gameModel->isInBook(parent)) {
        QString cmt = QString("last book move");
        parent->setComment(cmt);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->setMode(MODE_ENTER_MOVES);
        this->gameModel->triggerStateChange();
        msg->showMessage(tr("Game Analysis"), tr("The analysis is finished."));
    } else {
        // if we just started game analysis, first get the
        // evaluation for the leaf node. Only afterwards go
        // to the parent node.
        if(!this->gameModel->gameAnalysisStarted) {
            this->gameModel->getGame()->setCurrent(parent);
        } else {
            this->gameModel->gameAnalysisStarted = false;
        }
        QString fen = parent->getBoard().fen();
        this->uci_controller->uciSendCommand("stop");
        this->uci_controller->uciSendFen(fen);
        QString position = QString("position fen ").append(fen);
        this->uci_controller->uciSendPosition(position);
        this->uci_controller->uciGoMovetime(this->gameModel->engineThinkTimeMs);
    }
    delete msg;
}

void ModeController::onMultiPVChanged(int nrLines) {
    this->gameModel->nrPvLines = nrLines;
    if(this->gameModel->getMode() == MODE_ANALYSIS) {
        this->onActivateEnterMovesMode();
        this->onActivateAnalysisMode();
    }
}

void ModeController::onActivateAnalysisMode() {

    // first change gamestate and reset engine
    this->uci_controller->uciSendCommand("quit");
    QString engine_path = this->gameModel->getActiveEngine().getPath();
    this->uci_controller->startEngine(engine_path);
    this->uci_controller->uciNewgame();
    this->uci_controller->uciSendCommand("uci");
    // set engine strength to MAX
    // since we use stockfish, this is 20
    // will be just ignored by other engines
    // internal engine is always at idx 0. todo: Add global const
    // for 0, i.e. INTERNAL_ENGINE_IDX
    if(this->gameModel->getActiveEngineIdx() == 0) {
        this->uci_controller->uciStrength(20);
    }
    QVector<EngineOption> en_opts = this->gameModel->getActiveEngine().getUciOptions();
    this->uci_controller->sendEngineOptions(en_opts);
    // also send multi pv command according to current selection
    this->uci_controller->uciMultiPV(this->gameModel->nrPvLines);
    // then trigger state change
    this->gameModel->setMode(MODE_ANALYSIS);
    this->gameModel->triggerStateChange();
}

void ModeController::onActivateEnterMovesMode() {
    // stop any running engine
    this->uci_controller->uciSendCommand("stop");
    this->uci_controller->uciSendCommand("quit");
    // trigger statechange
    this->gameModel->setMode(MODE_ENTER_MOVES);
    this->gameModel->triggerStateChange();
}

void ModeController::onActivatePlayWhiteMode() {
    // first change gamestate and reset engine
    this->uci_controller->uciSendCommand("quit");
    QString engine_path = this->gameModel->getActiveEngine().getPath();
    this->uci_controller->startEngine(engine_path);
    this->uci_controller->uciNewgame();
    this->uci_controller->uciSendCommand("uci");
    if(this->gameModel->getActiveEngineIdx() == 0) {
        this->uci_controller->uciStrength(this->gameModel->getEngineStrength());
    }
    QVector<EngineOption> en_opts = this->gameModel->getActiveEngine().getUciOptions();
    this->uci_controller->sendEngineOptions(en_opts);
    // trigger statechange
    this->gameModel->setMode(MODE_PLAY_WHITE);
    this->gameModel->flipBoard = false;
    this->gameModel->humanPlayerColor = chess::WHITE;
    this->gameModel->triggerStateChange();
}

void ModeController::onActivatePlayBlackMode() {
    // first change gamestate and reset engine
    this->uci_controller->uciSendCommand("quit");
    QString engine_path = this->gameModel->getActiveEngine().getPath();
    this->uci_controller->startEngine(engine_path);
    this->uci_controller->uciNewgame();
    this->uci_controller->uciSendCommand("uci");
    if(this->gameModel->getActiveEngineIdx() == 0) {
        this->uci_controller->uciStrength(this->gameModel->getEngineStrength());
    }
    QVector<EngineOption> en_opts = this->gameModel->getActiveEngine().getUciOptions();
    this->uci_controller->sendEngineOptions(en_opts);
    // trigger statechange
    this->gameModel->setMode(MODE_PLAY_BLACK);
    this->gameModel->humanPlayerColor = chess::BLACK;
    this->gameModel->flipBoard = true;
    this->gameModel->triggerStateChange();

}

void ModeController::onActivateGameAnalysisMode() {

    bool run = false;
    DialogGameanalysis *dlg = new DialogGameanalysis(this->gameModel->engineThinkTimeMs,
                                                     this->gameModel->analysisThreshold);
    if(dlg->exec() == QDialog::Accepted) {
        run = true;
        this->gameModel->engineThinkTimeMs = dlg->secsPerMove;
        this->gameModel->analysisThreshold = dlg->threshold;
        if(dlg->rbAnalyseBoth->isChecked()) {
            this->gameModel->gameAnalysisForPlayer = ANALYSE_BOTH_PLAYERS;
        } else if(dlg->rbAnalyseWhite->isChecked()) {
            this->gameModel->gameAnalysisForPlayer = ANALYSE_WHITE_ONLY;
        } else {
            this->gameModel->gameAnalysisForPlayer = ANALYSE_BLACK_ONLY;
        }
    }
    delete dlg;
    if(run) {
        // delete all comments and variants
        this->gameModel->getGame()->removeAllComments();
        this->gameModel->getGame()->removeAllVariants();
        this->gameModel->getGame()->setTreeWasChanged(true);
        // first change gamestate and reset engine
        this->uci_controller->uciSendCommand("quit");
        QString engine_path = this->gameModel->getActiveEngine().getPath();
        this->uci_controller->startEngine(engine_path);
        this->uci_controller->uciNewgame();
        this->uci_controller->uciSendCommand("uci");
        QVector<EngineOption> en_opts = this->gameModel->getActiveEngine().getUciOptions();
        this->uci_controller->sendEngineOptions(en_opts);
        // set engine strength to MAX
        // since we use stockfish, this is 20
        if(this->gameModel->getActiveEngineIdx() == 0) {
            this->uci_controller->uciStrength(20);
        }
        // trigger statechange
        this->gameModel->setMode(MODE_GAME_ANALYSIS);
        this->gameModel->flipBoard = false;
        this->gameModel->getGame()->goToLeaf();
        this->gameModel->gameAnalysisStarted = true;
        this->gameModel->triggerStateChange();
    }
}

void ModeController::onStateChangePlayWhiteOrBlack() {
    // first check if we can apply a bookmove
    chess::GameNode *current = this->gameModel->getGame()->getCurrentNode();
    bool usedBook = false;
    QString uci = QString("");
    if(this->gameModel->canAndMayUseBook(current)) {
        QVector<chess::Move> mvs = this->gameModel->getBookMoves(current);
        if(mvs.size() > 0) {
            int sel = (rand() % (int)(mvs.size()));
            chess::Move mi = mvs.at(sel);
            uci = mi.uci();
            usedBook = true;
        }
    }
    if(!usedBook) {
        QString fen = this->gameModel->getGame()->getCurrentNode()->getBoard().fen();
        this->uci_controller->uciSendFen(fen);
        QString position = QString("position fen ").append(fen);
        this->uci_controller->uciSendPosition(position);
        this->uci_controller->uciGoMovetime(this->gameModel->engineThinkTimeMs);
    } else {
        this->onBestMove(uci);
    }
}

void ModeController::onActivatePlayoutPositionMode() {
    // first change gamestate and reset engine
    this->uci_controller->uciSendCommand("quit");
    QString engine_path = this->gameModel->getActiveEngine().getPath();
    this->uci_controller->startEngine(engine_path);
    this->uci_controller->uciNewgame();
    this->uci_controller->uciSendCommand("uci");
    QVector<EngineOption> en_opts = this->gameModel->getActiveEngine().getUciOptions();
    this->uci_controller->sendEngineOptions(en_opts);
    // trigger statechange
    this->gameModel->setMode(MODE_PLAYOUT_POS);
    this->gameModel->flipBoard = false;
    this->gameModel->triggerStateChange();
}

void ModeController::onStateChangePlayoutPosition() {
    QString fen = this->gameModel->getGame()->getCurrentNode()->getBoard().fen();
    this->uci_controller->uciSendFen(fen);
    QString position = QString("position fen ").append(fen);
    this->uci_controller->uciSendPosition(position);
    this->uci_controller->uciGoMovetime(this->gameModel->engineThinkTimeMs);
}

void ModeController::onStateChange() {
    int mode = this->gameModel->getMode();
    int turn = this->gameModel->getGame()->getCurrentNode()->getBoard().turn;

    MessageBox *msg = new MessageBox(this->parentWidget);
    chess::GameNode *current = this->gameModel->getGame()->getCurrentNode();
    // check if the game has ended by checkmate or stalemate
    // only show message if
    // human plays: show info, change mode to enter moves
    // enter moves mode & analysis mode: show info but only
    // if the node was just created
    if(current->getBoard().is_checkmate()) {
        if(mode == MODE_PLAY_WHITE || mode == MODE_PLAY_BLACK) {
            current->userWasInformedAboutResult = true;
            msg->showMessage(tr("Checkmate"), tr("The game is over!"));
            if(mode == MODE_PLAY_WHITE) {
                this->gameModel->getGame()->setResult(chess::RES_WHITE_WINS);
                this->gameModel->getGame()->setTreeWasChanged(true);
            } else {
                this->gameModel->getGame()->setResult(chess::RES_BLACK_WINS);
                this->gameModel->getGame()->setTreeWasChanged(true);
            }
            this->onActivateEnterMovesMode();
        } else if((mode == MODE_ANALYSIS || mode == MODE_ENTER_MOVES)
                  && !current->userWasInformedAboutResult) {
            current->userWasInformedAboutResult = true;
            msg->showMessage(tr("Checkmate"), tr("The game is over!"));
        }
    }
    // same for stalemate
    if(current->getBoard().is_stalemate()) {
        if(mode == MODE_PLAY_WHITE || mode == MODE_PLAY_BLACK) {
            msg->showMessage(tr("Stalemate"), tr("The game is drawn!"));
            this->onActivateEnterMovesMode();
        } else if((mode == MODE_ANALYSIS || mode == MODE_ENTER_MOVES)
                  && !current->userWasInformedAboutResult) {
            msg->showMessage(tr("Stalemate"), tr("The game is drawn!"));
        }
        current->userWasInformedAboutResult = true;
    }
    // 50 moves rule
    if(current->getBoard().can_claim_fifty_moves()) {
        if(mode == MODE_PLAY_WHITE || mode == MODE_PLAY_BLACK) {
            this->gameModel->getGame()->setTreeWasChanged(true);
            msg->showMessage(tr("Draw"), tr("50 moves rule"));
            this->onActivateEnterMovesMode();
        } else if((mode == MODE_ANALYSIS || mode == MODE_ENTER_MOVES)
                  && !current->userWasInformedAboutResult) {
            msg->showMessage(tr("Draw"), tr("50 moves rule"));
        }
        current->userWasInformedAboutResult = true;
    }
    if(current->getBoard().is_threefold_repetition()) {
        if(mode == MODE_PLAY_WHITE || mode == MODE_PLAY_BLACK) {
            msg->showMessage(tr("Draw"), tr("Threefold repetition"));
            this->onActivateEnterMovesMode();
        } else if((mode == MODE_ANALYSIS || mode == MODE_ENTER_MOVES)
                  && !current->userWasInformedAboutResult) {
            msg->showMessage(tr("Draw"), tr("Threefold repetition"));
        }
        current->userWasInformedAboutResult = true;
    }
    if(mode == MODE_ANALYSIS) {
        this->onStateChangeAnalysis();
    } else if(mode == MODE_ENTER_MOVES) {
        this->onStateChangeEnterMoves();
    } else if((mode == MODE_PLAY_WHITE || mode == MODE_PLAY_BLACK)
            && turn != this->gameModel->humanPlayerColor)
    {
        this->onStateChangePlayWhiteOrBlack();
    } else if(this->gameModel->getMode() == MODE_PLAYOUT_POS) {
        this->onStateChangePlayoutPosition();
    } else if(mode == MODE_GAME_ANALYSIS) {
        this->onStateChangeGameAnalysis();
    }
    delete msg;
}
