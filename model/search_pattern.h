#ifndef SEARCHPATTERN_H
#define SEARCHPATTERN_H

#include <QString>
#include "chess/board.h"

const int SEARCH_IGNORE_ELO = 0;
const int SEARCH_ONE_ELO = 1;
const int SEARCH_BOTH_ELO = 2;
const int SEARCH_AVERAGE_ELO = 3;

class SearchPattern
{
public:
    SearchPattern();

    bool searchGameData;
    bool searchComments;
    bool searchPosition;
    bool searchVariations;

    QString whiteName;
    QString blackName;
    bool ignoreNameColor;
    QString event;
    QString site;
    bool checkYear;
    int year_min;
    int year_max;
    bool checkEco;
    QString ecoStart;
    QString ecoStop;
    bool checkMoves;
    int move_min;
    int move_max;
    int checkElo;
    int elo_min;
    int elo_max;
    int result;

    QString comment_text1;
    QString comment_text2;
    bool wholeWord;
    bool mustNotStartInInitial;
    bool mustContainArrows;
    bool mustContainColoredFields;

    chess::Board search_board;

};

#endif // SEARCHPATTERN_H
