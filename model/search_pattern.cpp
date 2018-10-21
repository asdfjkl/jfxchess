#include "search_pattern.h"
#include "chess/game.h"
#include "chess/game_node.h"
#include <iostream>

SearchPattern::SearchPattern()
{

    this->searchGameData = false;
    this->searchComments = false;
    this->searchPosition = false;
    this->searchVariations = false;

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

    this->comment_text1 = "";
    this->comment_text2 = "";
    this->wholeWord = false;
    this->mustNotStartInInitial = false;
    this->caseSensitive = false;

}


bool SearchPattern::isInGame(chess::Game *g) {

    if(!this->searchVariations) {
        chess::GameNode *end = g->getEndNode();
        chess::GameNode *temp = g->getRootNode();
        while(temp != end) {
            if(this->compareToPattern(temp->getBoard())) {
                return true;
            }
            temp = temp->getVariation(0);
        }
        return false;
    } else {
        chess::GameNode* temp = g->getRootNode();
        return this->isInGameRec(temp);
    }

}

bool SearchPattern::isInGameRec(chess::GameNode *node) {

    if(this->compareToPattern(node->getBoard())) {
        return true;
    } else {
        for(int i=0;i<node->getVariations().size();i++) {
            chess::GameNode *temp_i = node->getVariation(i);
            if(this->isInGameRec(temp_i)) {
                return true;
            }
        }
        return false;
    }
}

bool SearchPattern::compareToPattern(const chess::Board &board) {

    for(uint8_t i=21;i<=98;i++) {
        uint8_t sp_i = search_board.piece_at(i);
        uint8_t bp_i = board.piece_at(i);
        if(sp_i == bp_i) {
            continue;
        } else {
            if(sp_i == chess::ANY_PIECE) {
                if(bp_i == chess::EMPTY) {
                    return false;
                } else {
                    continue;
                }
            }
            if(sp_i == chess::WHITE_ANY_PIECE) {
                if(bp_i >= chess::WHITE_PAWN && bp_i <= chess::WHITE_KING) {
                    continue;
                } else {
                    return false;
                }
            }
            if(sp_i == chess::BLACK_ANY_PIECE) {
                if(bp_i >= chess::BLACK_PAWN && bp_i <= chess::BLACK_KING) {
                    continue;
                } else {
                    return false;
                }
            }
            return false;
        }
    }
    std::cout << "found hit: " << std::endl;
    std::cout << board << std::endl;
    return true;
}
