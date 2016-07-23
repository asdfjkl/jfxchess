#ifndef ECOCODE_H
#define ECOCODE_H

#include <QString>
#include "board.h"

namespace chess {

struct EcoInfo {
    QString code;
    QString info;
};

class EcoCode
{
public:
    EcoCode();
    EcoInfo* classify(Board *b);

};

}

#endif // ECOCODE_H
