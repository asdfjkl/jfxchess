#include "dcgdecoder.h"
#include <QDebug>
#include <QStack>
#include <iostream>


chess::DcgDecoder::DcgDecoder()
{
    //this->game = new chess::Game();
}

int chess::DcgDecoder::decodeLength(QByteArray *ba, int *index) {
    int idx = *index;
    quint8 len1 = ba->at(idx);
    qDebug() << "len1 is: " << len1;
    if(len1 < 127) {
        (*index)++;
        return int(len1);
    }
    if(len1 == 0x81) {
        quint8 len2 = ba->at(idx+1);
        *index += 2;
        return int(len2);
    }
    if(len1 == 0x82) {
        quint16 len2 = ba->at(idx+1) * 256 + ba->at(idx+2);
        *index+=3;
        return int(len2);
    }
    if(len1 == 0x83) {
        quint32 ret = ba->at(idx+1) * 256 * 256 + ba->at(idx+2) * 256 + ba->at(idx+1);
        *index+=4;
        return ret;
    }
    if(len1 == 0x84) {
        quint32 ret = ba->at(idx+1) * 256*256*256 +  ba->at(idx+2) * 256 * 256 + ba->at(idx+3) * 256 + ba->at(idx+1);
        *index+=5;
        return int(ret);
    }
    qDebug() << "error here: " << ba->mid(*index, 30).toHex();
    throw std::invalid_argument("length decoding called with illegal byte value");
}

void chess::DcgDecoder::decodeAnnotations(QByteArray *ba, int *idx, int len, GameNode *current) {
    int start = *idx;
    int stop = (*idx) + len;
    for(int i=start;i<stop;i++) {
        quint8 ann_i = ba->at(i);
        qDebug() << "decoding annotation byte: " << ann_i;
        current->addNag(int(ann_i));
        (*idx)++;
    }
}

chess::Game* chess::DcgDecoder::decodeGame(Game *g, QByteArray *ba) {
    // to remember variations
    qDebug() << "called decode game with: " << ba->toHex();
    QStack<GameNode*> *game_stack = new QStack<GameNode*>();
    game_stack->push(g->getRootNode());
    GameNode* current = g->getRootNode();
    int idx = 0;
    bool error = false;
    // first check if we have a fen marker
    quint8 fenmarker = ba->at(idx);
    if(fenmarker == 0x01) {
        idx++;
        int len = this->decodeLength(ba, &idx);
        QByteArray fen = ba->mid(idx, len);
        QString fen_string = QString::fromUtf8(fen);
        chess::Board *b = new chess::Board(fen_string);
        g->getCurrentNode()->setBoard(b);
        idx += len;
    } else if(fenmarker == 0x00) {
        idx++;
    } else {
        error = true;
        idx++;
    }
    while(idx < ba->length() && !error) {
        quint8 byte = ba->at(idx);
        qDebug() << "next byte: " << byte;
        // >= 0x84: we have a marker, not a move
        if(byte >= 0x84) {
            if(byte == 0x84) {
                // start of variation
                // put current node on stack so that we
                // can go back when we reach end of variation
                qDebug() << "var start";
                game_stack->push(current);
                current = current->getParent();
                idx++;
            }
            else if(byte == 0x85) {
                // end of variation
                // pop from game stack. There _must_ be at least
                // one node, otherwise game is malformated (when closing
                // variation we must have started one before)
                // so pop from stack (but always leave root)
                qDebug() << "end of variation, popping from stack @";
                qDebug() << ba->mid(idx, 20).toHex();
                if(game_stack->size() > 1) {
                    current = game_stack->pop();
                }
                idx++;
            }
            else if(byte == 0x86) {
                idx++;
                // start of comment
                qDebug() << "comment start, trying to get len";
                int len = this->decodeLength(ba, &idx);
                qDebug() << ba->mid(idx, len+2).toHex();
                QString comment = QString::fromUtf8(QByteArray(ba->mid(idx,len)));
                current->setComment(comment);
                qDebug () << "got comment";
                idx+=len;
            }
            else if(byte == 0x87) {
                idx++;
                // annotations follow
                qDebug() << "ANNOTATIONS follow...";
                qDebug() << ba->mid(idx, 10).toHex();
                int len = this->decodeLength(ba, &idx);
                qDebug() << "len is: " << len;
                this->decodeAnnotations(ba, &idx, len, current);
                //idx+=len;
            } else if(byte == 0x88) {
                // null move
                Move *m = new Move();
                GameNode *next = new GameNode();
                Board *b_next = 0;
                try {
                    Board *b = current->getBoard();
                    b_next = b->copy_and_apply(*m);
                    next->setMove(m);
                    next->setBoard(b_next);
                    next->setParent(current);
                    current->addVariation(next);
                    current = next;
                } catch(std::invalid_argument a) {
                    std::cerr << a.what() << std::endl;
                    delete m;
                    delete next;
                    if(b_next != 0) {
                        delete b_next;
                    }
                    error = true;
                }
                idx++;
            } else {
                error = true;
            }
        } else {
            // we have a move, decode next two bytes
            // there should be at least one more move
            if(idx+1 >= ba->size()) {
                error = true;
            } else {
                qDebug() << "move dec";
                quint16 move = byte*256 + quint8((ba->at(idx+1)));
                quint8 from = quint8((move << 4) >> 10);
                quint8 to = quint8((quint8(move) << 2)) >> 2;
                // ((from % 8) + 1) is x column, (from/8) + 2 is row, cf.
                // Spracklen: "First steps in chess programming", BYTE 1978
                // for internal format
                quint8 from_internal = ((from % 8) + 1) + (((from / 8) + 2) * 10);
                quint8 to_internal = ((to % 8) + 1) + (((to / 8) + 2) * 10);
                quint8 promotion_piece = quint8((move << 2) >> 14);
                Move *m = new Move();
                GameNode *next = new GameNode();
                Board *b_next = 0;
                try {
                    Board *b = current->getBoard();
                    if(promotion_piece != 0) {
                        m = new chess::Move(from_internal, to_internal, promotion_piece);
                    } else {
                        m = new chess::Move(from_internal, to_internal);
                    }
                    if(b->is_legal_move(*m)) {
                        b_next = b->copy_and_apply(*m);
                        next->setMove(m);
                        next->setBoard(b_next);
                        next->setParent(current);
                        current->addVariation(next);
                        current = next;
                    } else {
                        qDebug() << "illegal move";
                        error = true;
                    }
                } catch(std::invalid_argument a) {
                    std::cerr << a.what() << std::endl;
                    delete m;
                    delete next;
                    if(b_next != 0) {
                        delete b_next;
                    }
                    error = true;
                }
                idx+=2;
            }
        }
    }
    return g;
}
