#ifndef ZOBRISTHASH_H
#define ZOBRISTHASH_H

#include "board.h"

namespace chess {

class ZobristHash
{
public:
    ZobristHash();
    quint64 compute(Board *b);

private:
    int kind_of_piece(uint8_t piece);

};

}

#endif // ZOBRISTHASH_H
