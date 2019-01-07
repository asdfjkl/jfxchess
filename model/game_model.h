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


#ifndef GAME_MODEL_H
#define GAME_MODEL_H

#include "chess/game.h"
#include "chess/board.h"
#include "chess/move.h"
#include "viewController/colorstyle.h"
#include <QPixmap>
#include <QList>
#include "chess/polyglot.h"
#include "engine.h"

const int MODE_ANALYSIS = 0;
const int MODE_PLAY_WHITE = 1;
const int MODE_PLAY_BLACK = 2;
const int MODE_ENTER_MOVES = 3;
const int MODE_GAME_ANALYSIS = 4;
const int MODE_PLAYOUT_POS = 5;

const int ANALYSE_BOTH_PLAYERS = 0;
const int ANALYSE_WHITE_ONLY = 1;
const int ANALYSE_BLACK_ONLY = 2;

const QString JERRY_VERSION = "3.1.0";

class GameModel : public QObject
{
    Q_OBJECT

public:
    int modelVersion;
    bool wasSaved;
    QString lastSaveFilename;
    QString lastOpenDir;
    QString currentPgnFilename;
    int currentPgnIndex;
    QString lastSaveDir;
    GameModel(QObject *parent = 0);
    ~GameModel();
    chess::Game* getGame();
    void setGame(chess::Game *g);
    void triggerStateChange();
    int getMode();
    void setMode(int mode);

    ColorStyle *colorStyle;
    QVector<Engine> getEngines();
    Engine getActiveEngine();
    int getActiveEngineIdx();
    void setEngines(QVector<Engine> engines);
    void setActiveEngine(int engine_idx);
    void setLastAddedEnginePath(QString &path);
    QString getLastAddedEnginePath();
    void setInternalEngine(Engine e);
    Engine getInternalEngine();

    bool humanPlayerColor;
    int engineStrength;
    int engineThinkTimeMs;
    bool flipBoard;

    // helpers for game analysis
    QString currentBestPv;
    int currentMateInMoves;
    float currentEval;

    QString prevBestPv;
    int prevMateInMoves;
    float prevEval;
    float analysisThreshold;

    bool showEval;

    int gameAnalysisForPlayer;

    int getEngineThinkTime();
    int getEngineStrength();

    void saveGameState();
    void restoreGameState();

    QString ressourcePath;

    bool canAndMayUseBook(chess::GameNode *node);
    QVector<chess::Move> getBookMoves(chess::GameNode *node);
    bool isInBook(chess::GameNode *node);    

    bool gameAnalysisStarted;

private:
    void loadOpeningBook();

    chess::Polyglot* book;
    chess::Game *game;
    int mode;
    QVector<Engine> engines;
    //Engine active_engine;
    int activeEngineIdx;
    QString lastAddedEnginePath;

    QString company;
    QString appId;

signals:
    void stateChange();

public slots:

};

#endif // GAME_MODEL_H
