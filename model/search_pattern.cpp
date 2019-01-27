#include "search_pattern.h"
#include "chess/game.h"

SearchPattern::SearchPattern()
{

    this->searchGameData = false;

    this->whiteName = "";
    this->blackName = "";
    this->ignoreNameColor = false;
    this->event = "";
    this->site = "";
    this->checkYear = false;
    this->year_min = 500;
    this->year_max = 2200;
    this->checkEco = false;
    this->ecoStart = "A00";
    this->ecoStop = "E99";
    this->checkMoves = false;
    this->move_min = 1;
    this->move_max = 99;
    this->checkElo = SEARCH_IGNORE_ELO;
    this->elo_min = 1000;
    this->elo_max = 3000;
    this->result = chess::RES_ANY;

}
