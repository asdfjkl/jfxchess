#ifndef SEARCH_PATTERN_H
#define SEARCH_PATTERN_H

#include <QString>

const int SEARCH_IGNORE_ELO = 0;
const int SEARCH_ONE_ELO = 1;
const int SEARCH_BOTH_ELO = 2;
const int SEARCH_AVERAGE_ELO = 3;

class SearchPattern
{
public:
    SearchPattern();

    bool searchGameData;

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

};

#endif // SEARCH_PATTERN_H
