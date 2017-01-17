#include "database.h"
#include "chess/game.h"
#include "chess/pgn_reader.h"
#include "chess/dcgencoder.h"
#include "chess/dcgdecoder.h"
#include "chess/byteutil.h"
#include "assert.h"
#include <iostream>
#include <QFile>
#include <QDataStream>
#include <QDebug>

chess::Database::Database(QString &filename)
{
    this->filenameBase = filename;
    this->filenameGames = QString(filename).append(".dcg");
    this->filenameIndex = QString(filename).append(".dci");
    this->filenameNames = QString(filename).append(".dcn");
    this->filenameSites = QString(filename).append(".dcs");
    this->filenameEvents = QString(filename).append(".dce");
    this->magicNameString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x6e");   
    this->magicIndexString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x69");
    this->magicGamesString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x67");
    this->magicSitesString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x73");
    this->magicEventString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x65");
    this->version = QByteArrayLiteral("\x00");
    this->offsetNames = new QMap<quint32, QString>();
    this->offsetSites = new QMap<quint32, QString>();
    this->offsetEvents = new QMap<quint32, QString>();
    this->dcgencoder = new chess::DcgEncoder();
    this->dcgdecoder = new chess::DcgDecoder();
    this->pgnreader = new chess::PgnReader();

    this->loadUponOpen = 0;
        
    this->indices = new QList<chess::IndexEntry*>();
}

chess::Database::~Database()
{
    this->offsetNames->clear();
    this->offsetSites->clear();
    this->offsetEvents->clear();
    delete this->offsetNames;
    delete this->offsetSites;
    delete this->offsetEvents;
    delete this->dcgencoder;
    delete this->dcgdecoder;
    delete this->pgnreader;
    delete this->indices;
}


void chess::Database::loadIndex() {

    this->indices->clear();
    QFile dciFile;
    dciFile.setFileName(this->filenameIndex);
    if(!dciFile.exists()) {
        std::cout << "Error: can't open .dci file." << std::endl;
    } else {
        // read index file into QList of IndexEntries
        // (basically memory mapping file)
        qDebug() << "open file: ";
        dciFile.open(QFile::ReadOnly);
        QDataStream gi(&dciFile);
        bool error = false;
        QByteArray magic;
        magic.resize(10);
        magic.fill(char(0x20));
        gi.readRawData(magic.data(), 10);
        if(QString::fromUtf8(magic) != this->magicIndexString) {
            error = true;
        }
        qDebug() << magic.toHex();
        QByteArray version;
        version.resize(1);;
        version.fill(char(0x00));
        gi.readRawData(version.data(), 1);
        qDebug() << "VERSION: " << version.at(0);
        if(version.at(0) != 0x00) {
            error = true;
        }
        gi >> this->loadUponOpen;
        qDebug() << "LOAD UPON: " << this->loadUponOpen;
        while(!gi.atEnd() && !error) {
            QByteArray idx;
            idx.resize(39);
            idx.fill(char(0x00));
            if(gi.readRawData(idx.data(), 39) < 0) {
                error = true;
                continue;
            }
            QDataStream ds_entry_i(idx);
            chess::IndexEntry *entry_i = new chess::IndexEntry();

            quint8 status;
            ds_entry_i >> status;
            qDebug() << "status: " << status;
            if(status == GAME_DELETED) {
                entry_i->deleted = true;
            } else {
                entry_i->deleted = false;
            }
            ds_entry_i >> entry_i->gameOffset;
            qDebug() << "gameOffset: " << entry_i->gameOffset;
            ds_entry_i >> entry_i->whiteOffset;
            qDebug() << "whiteOffset: " << entry_i->whiteOffset;
            ds_entry_i >> entry_i->blackOffset;
            qDebug() << "whiteOffset: " << entry_i->blackOffset;
            ds_entry_i >> entry_i->round;
            qDebug() << "round: " << entry_i->round;
            ds_entry_i >> entry_i->siteRef;
            qDebug() << "site ref: " << entry_i->siteRef;
            ds_entry_i >> entry_i->eventRef;
            qDebug() << "event ref: " << entry_i->eventRef;
            ds_entry_i >> entry_i->eloWhite;
            qDebug() << "eloWhite: " << entry_i->eloWhite;
            ds_entry_i >> entry_i->eloBlack;
            qDebug() << "eloBlack: " << entry_i->eloBlack;
            ds_entry_i >> entry_i->result;
            qDebug() << "result: " << entry_i->result;
            char *eco = new char[sizeof "A00"];
            ds_entry_i.readRawData(eco, 3);
            entry_i->eco = eco;
            qDebug() << QString::fromLocal8Bit(eco);
            ds_entry_i >> entry_i->year;
            ds_entry_i >> entry_i->month;
            ds_entry_i >> entry_i->day;
            qDebug() << "yy.mm.dd " << entry_i->year << entry_i->month << entry_i->day;
            this->indices->append(entry_i);
        }
        dciFile.close();
        if(int(this->loadUponOpen) >= this->indices->size()) {
            this->loadUponOpen = 0;
        }
    }
}

void chess::Database::loadSites() {
    this->offsetSites->clear();
    // for name file and site file build QMaps to quickly
    // access the data
    // read index file into QList of IndexEntries
    // (basically memory mapping file)
    QFile dcsFile;
    dcsFile.setFileName(this->filenameSites);
    dcsFile.open(QFile::ReadOnly);
    QDataStream ss(&dcsFile);
    bool error = false;
    QByteArray magic;
    magic.resize(10);
    magic.fill(char(0x20));
    ss.readRawData(magic.data(), 10);
    if(QString::fromUtf8(magic) != this->magicSitesString) {
        error = true;
    }
    while(!ss.atEnd() && !error) {
        quint32 pos = quint32(dcsFile.pos());
        QByteArray site_bytes;
        site_bytes.resize(36);
        site_bytes.fill(char(0x20));
        if(ss.readRawData(site_bytes.data(), 36) < 0) {
            error = true;
            break;
        }
        QString site = QString::fromUtf8(site_bytes).trimmed();
        this->offsetSites->insert(pos, site);
    }
    dcsFile.close();

}

void chess::Database::loadEvents() {
    this->offsetEvents->clear();
    // for events file and site file build QMaps to quickly
    // access the data
    // read index file into QList of IndexEntries
    // (basically memory mapping file)
    QFile dceFile;
    dceFile.setFileName(this->filenameEvents);
    dceFile.open(QFile::ReadOnly);
    QDataStream ss(&dceFile);
    bool error = false;
    QByteArray magic;
    magic.resize(10);
    magic.fill(char(0x20));
    ss.readRawData(magic.data(), 10);
    if(QString::fromUtf8(magic) != this->magicEventString) {
        error = true;
    }
    while(!ss.atEnd() && !error) {
        quint32 pos = quint32(dceFile.pos());
        QByteArray event_bytes;
        event_bytes.resize(36);
        event_bytes.fill(char(0x20));
        if(ss.readRawData(event_bytes.data(), 36) < 0) {
            error = true;
            break;
        }
        QString event = QString::fromUtf8(event_bytes).trimmed();
        qDebug() << "READ EVENT: " << pos << "at " << event;
        this->offsetEvents->insert(pos, event);
    }
    dceFile.close();

}



int chess::Database::countGames() {
    return this->indices->length();
}

chess::Game* chess::Database::getGameAt(int i) {

    if(i >= this->indices->size()) {
        return 0; // maybe throw out of range error or something instead of silently failing
    }
    chess::IndexEntry *ie = this->indices->at(i);
    if(ie->deleted) {
        // todo: jump to next valid entry
    }
    chess::Game* game = new chess::Game();
    QString whiteName = this->offsetNames->value(ie->whiteOffset);
    QString blackName = this->offsetNames->value(ie->blackOffset);
    QString site = this->offsetSites->value(ie->siteRef);
    qDebug() << "EVENT REF: " << ie->eventRef;
    QString event = this->offsetEvents->value(ie->eventRef);
    game->headers->insert("White",whiteName);
    game->headers->insert("Black", blackName);
    game->headers->insert("Site", site);
    game->headers->insert("Event", event);
    if(ie->eloWhite != 0) {
        game->headers->insert("WhiteElo", QString::number(ie->eloWhite));
    }
    if(ie->eloBlack != 0) {
        game->headers->insert("BlackElo", QString::number(ie->eloBlack));
    }
    qDebug() << "EVENT IS: " << event;
    QString date("");
    if(ie->year != 0) {
        date.append(QString::number(ie->year).rightJustified(4,'0'));
    } else {
        date.append("????");
    }
    date.append(".");
    if(ie->month != 0) {
        date.append(QString::number(ie->month).rightJustified(2,'0'));
    } else {
        date.append("??");
    }
    date.append(".");
    if(ie->day != 0) {
        date.append(QString::number(ie->day).rightJustified(2,'0'));
    } else {
        date.append("??");
    }
    game->headers->insert("Date", date);
    qDebug() << "RESULT: " << ie->result;
    if(ie->result == RES_WHITE_WINS) {
        game->headers->insert("Result", "1-0");
        game->setResult(RES_WHITE_WINS);
    } else if(ie->result == RES_BLACK_WINS) {
        game->headers->insert("Result", "0-1");
        game->setResult(RES_BLACK_WINS);
    } else if(ie->result == RES_DRAW) {
        game->headers->insert("Result", "1/2-1/2");
        game->setResult(RES_DRAW);
    } else {
        game->headers->insert("Result", "*");
        game->setResult(RES_UNDEF);
    }
    game->headers->insert("ECO", ie->eco);
    if(ie->round != 0) {
        game->headers->insert("Round", QString::number((ie->round)));
    } else {
        game->headers->insert("Round", "?");
    }
    QFile fnGames(this->filenameGames);
    if(fnGames.open(QFile::ReadOnly)) {
        fnGames.seek(ie->gameOffset);
        QDataStream gi(&fnGames);
        int length = this->decodeLength(&gi);
        QByteArray game_raw;
        game_raw.resize(length);
        game_raw.fill(char(0x20));
        gi.readRawData(game_raw.data(), length);
        qDebug() << "length" << length << " arr: " << game_raw.toHex();
        this->dcgdecoder->decodeGame(game, &game_raw);
    }
    return game;
}

int chess::Database::decodeLength(QDataStream *stream) {
    quint8 len1 = 0;
    *stream >> len1;
    qDebug() << "len1 is this: " << QString("%1").arg(len1 , 0, 16);
    if(len1 < 127) {
        return int(len1);
    }
    if(len1 == 0x81) {
        quint8 len2 = 0;
        *stream >> len2;
        return int(len2);
    }
    if(len1 == 0x82) {
        quint16 len2 = 0;
        *stream >> len2;
        return int(len2);
    }
    if(len1 == 0x83) {
        quint8 len2=0;
        quint16 len3=0;
        *stream >> len2;
        *stream >> len3;
        quint32 ret = 0;
        ret = len2 << 16;
        ret = ret + len3;
        return ret;
    }
    if(len1 == 0x84) {
        quint32 len = 0;
        *stream >> len;
        return int(len);
    }
    QByteArray buffer;
    quint8 byte;
    for (uint i=0; i<50; ++i) {
          *stream >> byte;
          buffer.append(byte);
    }
    qDebug() << "error here: " << buffer.toHex();
    throw std::invalid_argument("length decoding called with illegal byte value");
}

void chess::Database::loadNames() {

    this->offsetNames->clear();
    QFile dcnFile;
    dcnFile.setFileName(this->filenameNames);
    dcnFile.open(QFile::ReadOnly);
    QDataStream sn(&dcnFile);
    bool error = false;
    QByteArray magic;
    magic.resize(10);
    magic.fill(char(0x20));
    sn.readRawData(magic.data(), 10);
    if(QString::fromUtf8(magic) != this->magicNameString) {
        error = true;
    }
    while(!sn.atEnd() && !error) {
        quint32 pos = quint32(dcnFile.pos());
        QByteArray name_bytes;
        name_bytes.resize(36);
        name_bytes.fill(char(0x20));
        if(sn.readRawData(name_bytes.data(), 36) < 0) {
            error = true;
            break;
        }
        QString name = QString::fromUtf8(name_bytes).trimmed();
        this->offsetNames->insert(pos, name);
    }
    dcnFile.close();
}

void chess::Database::importPgnAndSave(QString &pgnfile) {

    QMap<QString, quint32> *names = new QMap<QString, quint32>();
    QMap<QString, quint32> *sites = new QMap<QString, quint32>();
    QMap<QString, quint32> *events = new QMap<QString, quint32>();

    this->importPgnNamesSitesEvents(pgnfile, names, sites, events);
    this->importPgnAppendSites(sites);
    this->importPgnAppendNames(names);
    this->importPgnAppendEvents(events);
    this->importPgnAppendGamesIndices(pgnfile, names, sites, events);

    delete names;
    delete sites;
    delete events;
}

void chess::Database::importPgnNamesSitesEvents(QString &pgnfile,
                                          QMap<QString, quint32> *names,
                                          QMap<QString, quint32> *sites,
                                          QMap<QString, quint32> *events) {

    std::cout << "scanning names and sites from " << pgnfile.toStdString() << std::endl;
    const char* encoding = pgnreader->detect_encoding(pgnfile);

    chess::HeaderOffset* header = new chess::HeaderOffset();

    quint64 offset = 0;
    bool stop = false;

    std::cout << "scanning at 0";
    int i = 0;
    while(!stop) {
        if(i%100==0) {
            std::cout << "\rscanning at " << offset;
        }
        i++;
        int res = pgnreader->readNextHeader(pgnfile, encoding, &offset, header);
        if(res < 0) {
            stop = true;
            continue;
        }
        // below 4294967295 is the max range val of quint32
        // provided as default key during search. In case we get this
        // default key as return, the current db site and name maps do not contain
        // a value. Note that this limits the offset range of name and site
        // file to 4294967295-1!
        // otherwise we add the key index of the existing database map files
        // these must then be skipped when writing the newly read sites and names
        if(header->headers != 0) {
            if(header->headers->contains("Site")) {
                QString site = header->headers->value("Site");
                if(site.size() > 36) {
                    site = site.left(36);
                }
                quint32 key = this->offsetSites->key(site, 4294967295);
                if(key == 4294967295) {
                    sites->insert(header->headers->value("Site"), 0);
                } else {
                    sites->insert(header->headers->value("Site"), key);
                }
            }
            if(header->headers->contains("Event")) {
                QString event = header->headers->value("Event");
                if(event.size() > 36) {
                    event = event.left(36);
                }
                quint32 key = this->offsetSites->key(event, 4294967295);
                if(key == 4294967295) {
                    events->insert(header->headers->value("Event"), 0);
                } else {
                    events->insert(header->headers->value("Event"), key);
                }
            }
            if(header->headers->contains("White")) {
                QString white = header->headers->value("White");
                quint32 key = this->offsetNames->key(white, 4294967295);
                if(white.size() > 36) {
                    white = white.left(36);
                }
                if(key == 4294967295) {
                    names->insert(header->headers->value("White"), 0);
                } else {
                    names->insert(header->headers->value("White"), key);
                }
            }
            if(header->headers->contains("Black")) {
                QString black = header->headers->value("Black");
                if(black.size() > 36) {
                    black = black.left(36);
                }
                quint32 key = this->offsetNames->key(black, 4294967295);
                if(key == 4294967295) {
                    names->insert(header->headers->value("Black"), 0);
                } else {
                    names->insert(header->headers->value("Black"), key);
                }
            }
        }
        header->headers->clear();
        if(header->headers!=0) {
           delete header->headers;
        }
    }
    std::cout << std::endl << "scanning finished" << std::flush;
    delete header;
}

// save the map at the _end_ of file with filename (i.e. apend new names or sites)
// update the offset while saving
void chess::Database::importPgnAppendNames(QMap<QString, quint32> *names) {
    QFile fnNames(this->filenameNames);
    if(fnNames.open(QFile::Append)) {
        if(fnNames.pos() == 0) {
            fnNames.write(this->magicNameString, this->magicNameString.length());
        }
        QList<QString> keys = names->keys();
        for (int i = 0; i < keys.length(); i++) {
            // value != 0 means the value exists
            // already in the existing database maps
            quint32 ex_offset = names->value(keys.at(i));
            if(ex_offset != 0) {
                continue;
            }
            QByteArray name_i = keys.at(i).toUtf8();
            // truncate if too long
            if(name_i.size() > 36) {
                name_i = name_i.left(36);
            }
            // pad to 36 byte if necc.
            int pad_n = 36 - name_i.length();
            if(pad_n > 0) {
                for(int j=0;j<pad_n;j++) {
                    name_i.append(0x20);
                }
            }
            quint32 offset = fnNames.pos();
            qDebug() << "writing pos: " << offset << " for " << name_i;
            fnNames.write(name_i,36);
            names->insert(name_i.trimmed(), offset);
        }
    }
    fnNames.close();
}

// save the map at the _end_ of file with filename (i.e. apend new names or sites)
// update the offset while saving
void chess::Database::importPgnAppendSites(QMap<QString, quint32> *sites) {
    QFile fnSites(this->filenameSites);
    if(fnSites.open(QFile::Append)) {
        if(fnSites.pos() == 0) {
            fnSites.write(this->magicSitesString, this->magicSitesString.length());
        }
        QList<QString> keys = sites->keys();
        for (int i = 0; i < keys.length(); i++) {
            // value != 0 means the value exists
            // already in the existing database maps
            quint32 ex_offset = sites->value(keys.at(i));
            if(ex_offset != 0) {
                continue;
            }
            QByteArray site_i = keys.at(i).toUtf8();
            // truncate if too long
            if(site_i.size() > 36) {
                site_i = site_i.left(36);
            }
            // pad to 36 byte if necc.
            int pad_n = 36 - site_i.length();
            if(pad_n > 0) {
                for(int j=0;j<pad_n;j++) {
                    site_i.append(0x20);
                }
            }
        quint32 offset = fnSites.pos();
        fnSites.write(site_i,36);
        sites->insert(site_i.trimmed(), offset);
        }
    }
    fnSites.close();
}

// save the map at the _end_ of file with filename (i.e. apend new names or sites)
// update the offset while saving
void chess::Database::importPgnAppendEvents(QMap<QString, quint32> *events) {
    QFile fnEvents(this->filenameEvents);
    if(fnEvents.open(QFile::Append)) {
        if(fnEvents.pos() == 0) {
            fnEvents.write(this->magicEventString, this->magicEventString.length());
        }
        QList<QString> keys = events->keys();
        for (int i = 0; i < keys.length(); i++) {
            // value != 0 means the value exists
            // already in the existing database maps
            quint32 ex_offset = events->value(keys.at(i));
            if(ex_offset != 0) {
                continue;
            }
            QByteArray event_i = keys.at(i).toUtf8();
            // truncate if too long
            if(event_i.size() > 36) {
                event_i = event_i.left(36);
            }
            // pad to 36 byte if necc.
            int pad_n = 36 - event_i.length();
            if(pad_n > 0) {
                for(int j=0;j<pad_n;j++) {
                    event_i.append(0x20);
                }
            }
        quint32 offset = fnEvents.pos();
        qDebug() << "ADDING OFFSET: " << offset;
        fnEvents.write(event_i,36);
        events->insert(event_i.trimmed(), offset);
        }
    }
    fnEvents.close();
}


void chess::Database::importPgnAppendGamesIndices(QString &pgnfile,
                                                  QMap<QString, quint32> *names,
                                                  QMap<QString, quint32> *sites,
                                                  QMap<QString, quint32> *events) {

    // now save everything
    chess::HeaderOffset *header = new chess::HeaderOffset();
    quint64 offset = 0;
    QFile pgnFile(pgnfile);
    quint64 size = pgnFile.size();
    bool stop = false;

    const char* encoding = this->pgnreader->detect_encoding(pgnfile);

    QFile fnIndex(this->filenameIndex);
    QFile fnGames(this->filenameGames);
    if(fnIndex.open(QFile::Append)) {
        if(fnGames.open(QFile::Append)) {
            if(fnIndex.pos() == 0) {
                fnIndex.write(this->magicIndexString, this->magicIndexString.length());
                // version
                fnIndex.write(this->version,1);
                QByteArray openDefault;
                //openDefault.resize(8);
                //openDefault.fill(0x00);
                ByteUtil::append_as_uint64(&openDefault, this->loadUponOpen);
                qDebug() << "OFFSET0: " << openDefault.toHex();
                fnIndex.write(openDefault, 8);
            }
            if(fnGames.pos() == 0) {
                fnGames.write(magicGamesString, magicGamesString.length());
            }
            std::cout << "\nsaving games: 0/"<< size;
            int i = 0;
            while(!stop) {
                if(i%100==0) {
                    std::cout << "\rsaving games: "<<offset<< "/"<<size << std::flush;
                }
                i++;
                int res = pgnreader->readNextHeader(pgnfile, encoding, &offset, header);
                if(res < 0) {
                    stop = true;
                    continue;
                }
                // the current index entry
                QByteArray iEntry;
                // first write index entry
                // status
                ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                // game offset
                ByteUtil::append_as_uint64(&iEntry, fnGames.pos());
                // white offset
                QString white = header->headers->value("White");
                quint32 whiteOffset = names->value(white);
                ByteUtil::append_as_uint32(&iEntry, whiteOffset);
                // black offset
                QString black = header->headers->value("Black");
                quint32 blackOffset = names->value(black);
                ByteUtil::append_as_uint32(&iEntry, blackOffset);
                // round
                quint16 round = header->headers->value("Round").toUInt();
                ByteUtil::append_as_uint16(&iEntry, round);
                // site offset
                quint32 site_offset = sites->value(header->headers->value("Site"));
                ByteUtil::append_as_uint32(&iEntry, site_offset);
                // event offset
                quint32 event_offset = events->value(header->headers->value("Event"));
                ByteUtil::append_as_uint32(&iEntry, event_offset);
                qDebug() << "EVENT OFFSET: " << event_offset;
                // elo white
                quint16 elo_white = header->headers->value("WhiteElo").toUInt();
                qDebug() << "elo white: " << elo_white;
                ByteUtil::append_as_uint16(&iEntry, elo_white);
                quint16 elo_black = header->headers->value("BlackElo").toUInt();
                ByteUtil::append_as_uint16(&iEntry, elo_black);
                // result
                if(header->headers->contains("Result")) {
                    QString res = header->headers->value("Result");
                    if(res == "1-0") {
                        ByteUtil::append_as_uint8(&iEntry, quint8(0x01));
                    } else if(res == "0-1") {
                        ByteUtil::append_as_uint8(&iEntry, quint8(0x02));
                    } else if(res == "1/2-1/2") {
                        ByteUtil::append_as_uint8(&iEntry, quint8(0x03));
                    } else {
                        ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                    }
                } else  {
                    ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                }
                qDebug() << iEntry.size();
                // ECO
                if(header->headers->contains("ECO")) {
                    QByteArray eco = header->headers->value("ECO").toUtf8().left(3);
                    qDebug() << eco;
                    iEntry.append(eco);
                } else {
                    QByteArray eco = QByteArrayLiteral("\x00\x00\x00");
                    iEntry.append(eco);
                }
                qDebug() << iEntry.size();
                // parse date
                if(header->headers->contains("Date")) {
                    QString date = header->headers->value("Date");
                    // try to parse the date
                    quint16 year = 0;
                    quint8 month = 0;
                    quint8 day = 0;
                    QStringList dd_mm_yy = date.split(".");
                    if(dd_mm_yy.size() > 0 && dd_mm_yy.at(0).length() == 4) {
                        quint16 prob_year = dd_mm_yy.at(0).toInt();
                        qDebug() << "PROb YEAR:" << prob_year;
                        if(prob_year > 0 && prob_year < 2100) {
                            year = prob_year;
                        }
                        if(dd_mm_yy.size() > 1 && dd_mm_yy.at(1).length() == 2) {
                            quint8 prob_month = dd_mm_yy.at(1).toInt();
                            qDebug() << "prob monath: " << prob_month;
                            if(prob_month > 0 && prob_month <= 12) {
                                month = prob_month;
                            }
                            if(dd_mm_yy.size() > 2 && dd_mm_yy.at(2).length() == 2) {
                            quint8 prob_day = dd_mm_yy.at(2).toInt();
                            if(prob_day > 0 && prob_day < 32) {
                                day = prob_day;
                                }
                            }
                        }
                    }
                    qDebug() << "YEAR: " << year;
                    ByteUtil::append_as_uint16(&iEntry, year);
                    ByteUtil::append_as_uint8(&iEntry, month);
                    ByteUtil::append_as_uint8(&iEntry, day);
                } else {
                    ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                    ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                    ByteUtil::append_as_uint8(&iEntry, quint8(0x00));
                }
                qDebug() << iEntry.size();
                assert(iEntry.size() == 39);
                fnIndex.write(iEntry, iEntry.length());
                //qDebug() << "just before reading back file";
                chess::Game *g = pgnreader->readGameFromFile(pgnfile, encoding, header->offset);
                //qDebug() << "READ file ok";
                QByteArray *g_enc = dcgencoder->encodeGame(g); //"<<<<<<<<<<<<<<<<<<<<<< this is the cause of mem acc fault"
                //qDebug() << "enc ok";
                qDebug() << "writing game: " << g_enc->toHex();
                fnGames.write(*g_enc, g_enc->length());
                delete g_enc;
                header->headers->clear();
                if(header->headers!=0) {
                    delete header->headers;
                }
                delete g;
            }
            std::cout << "\rsaving games: "<<size<< "/"<<size << std::endl;
        }
        fnGames.close();
    }
    fnIndex.close();
    delete header;
}



/*
 write sites into file
QString fnSitesString = pgnFileName.left(pgnFileName.size()-3).append("dcs");
QFile fnSites(fnSitesString);
QByteArray magicSiteString = QByteArrayLiteral("\x53\x69\x6d\x70\x6c\x65\x43\x44\x62\x73");
success = false;
if(fnSites.open(QFile::WriteOnly)) {
  QDataStream s(&fnSites);
  s.writeRawData(magicSiteString, magicSiteString.length());
  QList<QString> keys = sites->keys();
  for (int i = 0; i < keys.length(); i++) {
      QByteArray site_i = keys.at(i).toUtf8();
      // truncate if too long
      if(site_i.size() > 36) {
          site_i = site_i.left(36);
      }
      int pad_n = 36 - site_i.length();
      if(pad_n > 0) {
          for(int j=0;j<pad_n;j++) {
              site_i.append(0x20);
          }
      }
      quint32 offset = fnSites.pos();
      s.writeRawData(site_i,36);
      sites->insert(site_i, offset);
  }
  success = true;
} else {
  std::cerr << "error opening output file\n";
}
fnNames.close();
if(!success) {
    throw std::invalid_argument("Error writing file");
}
pgnFile.close();
*/

    /*
void chess::Database::saveToFile() {

}

void chess::Database::writeSites() {

}

void chess::Database::writeNames() {

}

void chess::Database::writeIndex() {

}

void chess::Database::writeGames() {

}

*/
