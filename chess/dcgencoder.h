#ifndef DCGWRITER_H
#define DCGWRITER_H

#include <QByteArray>
#include <QQueue>
#include "game.h"

namespace chess {

class DcgEncoder
{
public:
    DcgEncoder();
    ~DcgEncoder();
    QByteArray* encodeGame(Game *game);
    QByteArray* encodeHeader();
    void traverseNodes(GameNode *current);
    void reset();

    void appendMove(Move *move);
    void appendLength(int len);
    void prependLength(int len);
    void appendNags(GameNode* node);
    void appendComment(GameNode* node);

    void appendStartTag();
    void appendEndTag();



    Game* decodeGame();

private:
    QByteArray* gameBytes;

};

}

#endif // DCGWRITER_H
