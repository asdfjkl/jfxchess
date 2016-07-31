#include "polyglot.h"
#include <QFile>
#include <iostream>
#include <QDebug>
#include <QDataStream>

namespace chess {

char promote_pieces[6] = " nbrq";

Polyglot::Polyglot(QString &bookname)
{
    this->readFile = false;
    QFile file(bookname);
    quint64 size = file.size();
    if(size <= 15728640) {
        if(file.open(QIODevice::ReadOnly)) {
            //qDebug() << "reading test ";
            this->book = new QByteArray(file.readAll());
            /*
            QByteArray foo = file.read(16ULL);
            QDataStream foobar(foo);
            quint64 key;
            foobar >> key;
            qDebug() << "bar: " << foo.toHex();
            std::cout << "foobar key " << std::hex << key << std::endl;
            const Entry *entry = (const Entry*)foo.constData();
            std::cout << std::hex << entry->key << std::endl;
            */
            file.close();
            this->readFile = true;
        } else {
            std::cerr << "couldn't open polyglot book: " << bookname.toStdString() << std::endl;
        }
    } else {
        std::cerr << "couldn't open polyglot book: files > 15 MB are not supported" << std::endl;
    }
    //qDebug() << "finished reading";
}

Entry Polyglot::entryFromOffset(int offset) {
    if(this->book == 0 || offset > this->book->size()-16 || !this->readFile) {
        throw std::invalid_argument("called entryFromOffset with invalid offset");
    }
    Entry e = {0,0,0,0};
    QByteArray ba = this->book->mid(offset, 16);
    QDataStream da(ba);
    da >> e.key;
    da >> e.move;
    da >> e.weight;
    da >> e.learn;
    return e;
}

Move Polyglot::moveFromEntry(Entry e) {
    quint64 move = e.move;

    // literally taken from pgn_show.c
    // licensed under GPL2 - really
    // pure C code, so somehow little
    // bit odd here: todo: rewrite in nicer C++
    char move_s[6];
    int len = 4;
    int f,fr,ff,t,tr,tf,p;
    f=(move>>6)&077;
    fr=(f>>3)&0x7;
    ff=f&0x7;
    t=move&077;
    tr=(t>>3)&0x7;
    tf=t&0x7;
    p=(move>>12)&0x7;
    move_s[0]=ff+'a';
    move_s[1]=fr+'1';
    move_s[2]=tf+'a';
    move_s[3]=tr+'1';
    if(p){
        move_s[4]=promote_pieces[p];
        move_s[5]='\0';
        len = 5;
    }else{
        move_s[4]='\0';
    }
    if(!strcmp(move_s,"e1h1")){
        strcpy(move_s,"e1g1");
    }else  if(!strcmp(move_s,"e1a1")){
        strcpy(move_s,"e1c1");
    }else  if(!strcmp(move_s,"e8h8")){
        strcpy(move_s,"e8g8");
    }else  if(!strcmp(move_s,"e8a8")){
        strcpy(move_s,"e8c8");
    }

    QString uci = QString::fromLatin1(move_s, len);
    Move m = Move(uci);
    return m;
}

Moves* Polyglot::findMoves(Board *board) {
    Moves* bookMoves = new Moves();
    if(this->book != 0 && this->readFile) {
        quint64 zh_board = board->zobrist();
        quint64 low = 0;
        quint64 high = this->book->size() / 16;
        // find entry fast
        while(low < high) {
            quint64 middle = (low + high) / 2;
            Entry e = this->entryFromOffset(middle*16);
            quint64 middle_key = e.key;
            if(middle_key < zh_board) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        quint64 offset = low;
        quint64 size = this->book->size() / 16;
        // now we have the lowest key pos
        // where a possible entry is. collect all
        while(offset < size) {
            Entry e = this->entryFromOffset(offset*16);
            if(e.key != zh_board) {
                break;
            }
            Move m = this->moveFromEntry(e);
            bookMoves->append(m);
            offset += 1;
        }
    }
    return bookMoves;
}

bool Polyglot::inBook(Board *board) {
    int cntBookMoves = 0;

    if(this->book != 0 && this->readFile) {
        quint64 zh_board = board->zobrist();
        quint64 low = 0;
        quint64 high = this->book->size() / 16;
        // find entry fast
        while(low < high) {
            quint64 middle = (low + high) / 2;
            Entry e = this->entryFromOffset(middle*16);
            quint64 middle_key = e.key;
            if(middle_key < zh_board) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        quint64 offset = low;
        quint64 size = this->book->size() / 16;
        // now we have the lowest key pos
        // where a possible entry is. collect all
        while(offset < size) {
            Entry e = this->entryFromOffset(offset*16);
            if(e.key != zh_board) {
                break;
            }
            cntBookMoves++;
            offset += 1;
        }
    }
    if(cntBookMoves > 0) {
        return true;
    } else {
        return false;
    }
}

}
