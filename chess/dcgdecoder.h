#ifndef DCGDECODER_H
#define DCGDECODER_H

#include "chess/game.h"
#include <QByteArray>

namespace chess {

class DcgDecoder
{
public:
    DcgDecoder();
    ~DcgDecoder();
    Game* decodeGame(Game *g, QByteArray *ba);
    int decodeLength(QByteArray *ba, int *idx);

private:
    Game* game;
    void decodeAnnotations(QByteArray *ba, int *idx, int len, GameNode *current);
};

}

#endif // DCGDECODER_H
