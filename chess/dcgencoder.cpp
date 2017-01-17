#include "dcgencoder.h"
#include "assert.h"
#include <QDebug>
#include "chess/byteutil.h"

namespace chess {

// TODO: rewrite by using QDataStream. Then appending
// is simply operator <<

DcgEncoder::DcgEncoder()
{
    this->gameBytes = new QByteArray();
}

DcgEncoder::~DcgEncoder()
{
    delete this->gameBytes;
}

void DcgEncoder::traverseNodes(GameNode *current) {
    int cntVar = current->getVariations()->count();

    // first handle mainline move, if there are variations
    if(cntVar > 0) {
        GameNode* main_variation = current->getVariation(0);
        this->appendMove(main_variation->getMove());
        // encode nags
        int cntNags = main_variation->getNags()->count();
        if(cntNags > 0) {
            this->appendNags(main_variation);
        }
        // encode comment, if any
        if(!main_variation->getComment().isEmpty()) {
            this->appendComment(main_variation);
        }
    }

    // now handle all variations (sidelines)
    for(int i=1;i<cntVar;i++) {
        // first create variation start marker
        GameNode *var_i = current->getVariation(i);
        this->appendStartTag();
        this->appendMove(var_i->getMove());
        // encode nags
        int cntNags = var_i->getNags()->count();
        if(cntNags > 0) {
            this->appendNags(var_i);
        }
        // encode comment, if any
        if(!var_i->getComment().isEmpty()) {
            this->appendComment(var_i);
        }
        // recursive call for all childs
        this->traverseNodes(var_i);

        // print variation end
        this->appendEndTag();
    }

    // finally do the mainline
    if(cntVar > 0) {
        GameNode* main_variation = current->getVariation(0);
        this->traverseNodes(main_variation);
    }
}

QByteArray* DcgEncoder::encodeGame(Game *game) {
    //qDebug() << "encoding";
    delete this->gameBytes;
    //qDebug() << "deleted gamebytes";
    this->gameBytes = new QByteArray();
    // add fen string tag if root is not initial position
    chess::Board* root = game->getRootNode()->getBoard();
    if(!root->is_initial_position()) {
        const QByteArray fen = root->fen().toUtf8();
        int l = fen.length();
        this->gameBytes->append(quint8(0x01));
        this->appendLength(l);
        this->gameBytes->append(fen);
    } else {
        this->gameBytes->append((char) (0x00));
    }
    //qDebug() << "before traversal";
    // if the root node has a comment, append first
    if(!(game->getRootNode()->getComment().isEmpty())) {
        this->appendComment(game->getRootNode());
    }
    this->traverseNodes(game->getRootNode());
    // prepend length
    int l = this->gameBytes->size();
    qDebug() << "len: " << l;
    this->prependLength(l);
    qDebug() << "game: " << this->gameBytes->toHex();
    return new QByteArray(*this->gameBytes);
}

void DcgEncoder::appendMove(Move *move) {
    if(move->is_null) {
        this->gameBytes->append(quint8(0x88));
    } else {
        QPoint fromPoint = move->fromAsXY();
        QPoint toPoint = move->toAsXY();
        quint8 from = fromPoint.y() * 8 + fromPoint.x();
        quint8 to = toPoint.y() * 8 + toPoint.x();
        quint16 move_binary = qint16(to) + (quint16(from) << 6);
        //qDebug() << "MOVE: "<< (qint16(to) + (quint16(from) << 6));
        if(move->promotion_piece != 0) {
            qDebug() << "FROM POINT " << fromPoint;
            qDebug() << "FROM  " << from;
            qDebug() << "PROMOTION PIECE: "<< quint16((move->promotion_piece) << 12);
            qDebug() << "promotion move bin: " << move_binary;
            move_binary += quint16((move->promotion_piece) << 12);
            qDebug() << "promotion move bin: " << move_binary;
        }
        ByteUtil::append_as_uint16(this->gameBytes, move_binary);
        qDebug() << this->gameBytes->mid(this->gameBytes->length()-2, 2).toHex();
    }
}

void DcgEncoder::appendLength(int len) {
    if(len >= 0 && len < 127) {
        ByteUtil::append_as_uint8(this->gameBytes, quint8(len));
    } else if(len >= 0 && len < 255) {
        ByteUtil::append_as_uint8(this->gameBytes, quint8(0x81));
        ByteUtil::append_as_uint8(this->gameBytes, quint8(len));
    } else if(len >= 0 && len < 65535) {
        ByteUtil::append_as_uint8(this->gameBytes, quint8(0x82));
        ByteUtil::append_as_uint16(this->gameBytes, quint16(len));
    } else if(len >= 0 && len < 16777215) {
        ByteUtil::append_as_uint8(this->gameBytes, quint8(0x83));
        ByteUtil::append_as_uint8(this->gameBytes, quint8(len >> 16));
        ByteUtil::append_as_uint16(this->gameBytes, quint16(len));
    } else if(len >= 0 && len < 4294967) {
        ByteUtil::append_as_uint8(this->gameBytes, quint8(0x84));
        ByteUtil::append_as_uint32(this->gameBytes, quint32(len));
    }
}

void DcgEncoder::prependLength(int len) {
    if(len >= 0 && len < 127) {
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(len));
    } else if(len >= 0 && len < 255) {
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(len));
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(0x81));        
    } else if(len >= 0 && len < 65535) {
        ByteUtil::prepend_as_uint16(this->gameBytes, quint16(len));
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(0x82));
    } else if(len >= 0 && len < 16777215) {
        ByteUtil::prepend_as_uint16(this->gameBytes, quint16(len));
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(len >> 16));
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(0x83));
    } else if(len >= 0 && len < 4294967) {
        ByteUtil::prepend_as_uint32(this->gameBytes, quint32(len));
        ByteUtil::prepend_as_uint8(this->gameBytes, quint8(0x84));
    }
}

void DcgEncoder::appendNags(GameNode* node) {
    QList<int>* nags = node->getNags();
    int l = nags->length();
    if(l>0) {
        this->gameBytes->append(quint8(0x87));
        this->appendLength(l);
        for(int i=0;i<nags->length();i++) {
            quint8 nag_i = quint8(nags->at(i));
            this->gameBytes->append(nag_i);
        }
    }
}

void DcgEncoder::appendComment(GameNode* node) {
    const QByteArray comment_utf8 = node->getComment().toUtf8();
    int l = comment_utf8.size();
    qDebug() << "COMMENT LENGTH: " << l;
    if(l>0) {
        this->gameBytes->append(quint8(0x86));
        this->appendLength(l);
        qDebug() << "COMMENT LEN: " << this->gameBytes->mid(this->gameBytes->size()-4, 4);
        this->gameBytes->append(comment_utf8);
    }
}

void DcgEncoder::appendStartTag() {
    this->gameBytes->append(quint8(0x84));
}

void DcgEncoder::appendEndTag() {
    this->gameBytes->append(quint8(0x85));
}

}
