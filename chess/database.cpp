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
#include <QProgressDialog>
#include <QFileDialog>
#include <QDir>
#include <QApplication>
#include "various/messagebox.h"

#include <ctime>

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
    //this->dcgencoder = new chess::DcgEncoder();
    //this->dcgdecoder = new chess::DcgDecoder();
    //this->pgnreader = new chess::PgnReader();

    this->lastOpenDir = QString("");
    this->currentOpenGameIdx = -1;

    this->loadUponOpen = 0;
        
    this->indices = new QList<chess::IndexEntry*>();
    this->currentSearchIndices = new QList<chess::IndexEntry*>();
}

chess::Database::~Database()
{
    this->offsetNames->clear();
    this->offsetSites->clear();
    this->offsetEvents->clear();
    delete this->offsetNames;
    delete this->offsetSites;
    delete this->offsetEvents;
    //delete this->dcgencoder;
    //delete this->dcgdecoder;
    //delete this->pgnreader;
    // todo: this indices must be cleared first! ?! who is
    // responsible for cleaning up?
    delete this->indices;
    delete this->currentSearchIndices;
}

void chess::Database::reset() {

    qDeleteAll(indices->begin(), indices->end());
    indices->clear();

    qDeleteAll(currentSearchIndices->begin(), currentSearchIndices->end());
    currentSearchIndices->clear();

    this->offsetNames->clear();
    this->offsetSites->clear();
    this->offsetEvents->clear();

    QString dummy = QString("Untitled");
    this->updateBaseName(dummy);

    this->lastOpenDir = QString("");

    this->loadUponOpen = 0;
}

void chess::Database::open(QWidget* parent = 0) {

    QString filename = QFileDialog::getOpenFileName(parent,
                          QApplication::tr("Open Database"), this->lastOpenDir, QApplication::tr("*.dci"));
    if(!filename.isEmpty() && filename.endsWith(".dci")) {

        QString base = QString(filename).left(filename.size()-4);
        this->updateBaseName(base);

        QDir dir = QDir::root();
        QString path = dir.absoluteFilePath(filename);

        this->lastOpenDir = QString(path);

        int err = 0;
        err = this->loadIndex(path, parent);
        if(err != 0) {
            this->reset();
            MessageBox *msg = new MessageBox(parent);
            msg->showMessage("Error opening .dci", QString("Code: ").append(QString::number(err)));
        }
        err = this->loadMetaData(this->filenameNames, this->offsetNames, this->magicNameString, parent);
        qDebug() << "error code of load data names" << err;
        if(err != 0) {
            this->reset();
            MessageBox *msg = new MessageBox(parent);
            msg->showMessage("Error opening .dcn", QString("Code: ").append(QString::number(err)));
        }
        err = this->loadMetaData(this->filenameEvents, this->offsetEvents, this->magicEventString, parent);
        if(err != 0) {
            this->reset();
            MessageBox *msg = new MessageBox(parent);
            msg->showMessage("Error opening .dce", QString("Code: ").append(QString::number(err)));
        }
        err = this->loadMetaData(this->filenameSites, this->offsetSites, this->magicSitesString, parent);
        if(err != 0) {
            this->reset();
            MessageBox *msg = new MessageBox(parent);
            msg->showMessage("Error opening .dcs", QString("Code: ").append(QString::number(err)));
        }
    }
}

void chess::Database::updateBaseName(QString &basename) {

    this->filenameBase = basename;
    this->filenameGames = QString(basename).append(".dcg");
    this->filenameIndex = QString(basename).append(".dci");
    this->filenameNames = QString(basename).append(".dcn");
    this->filenameSites = QString(basename).append(".dcs");
    this->filenameEvents = QString(basename).append(".dce");

}

int chess::Database::loadIndex(QString &filename, QWidget* parent) {

    this->indices->clear();
    QFile dciFile;
    dciFile.setFileName(filename);
    if(!dciFile.exists()) {
        std::cerr << "Error: can't open .dci file." << std::endl;
        return ERROR_OPENING_DCI;
    } else {
        int error_code = 0;

        // read index file into QList of IndexEntries

        dciFile.open(QFile::ReadOnly);
        quint64 size = dciFile.size();

        // set up the progress dialog
        QProgressDialog progress("reading index...", "Cancel", 0, size, parent);
        progress.setWindowModality(Qt::WindowModal);
        progress.setCancelButton(0);

        QDataStream gi(&dciFile);

        bool error = false;

        QByteArray magic;
        magic.resize(10);
        magic.fill(char(0x20));
        gi.readRawData(magic.data(), 10);
        if(QString::fromUtf8(magic) != this->magicIndexString) {
            error_code = ERROR_UNKNOWN_FILETYPE;
            error = true;
        }
        QByteArray version;
        version.resize(1);;
        version.fill(char(0x00));
        gi.readRawData(version.data(), 1);

        if(version.at(0) != 0x00) {
            error_code = ERROR_UNKNOWN_VERSION;
            error = true;
        }
        gi >> this->loadUponOpen;

        QByteArray idx;
        idx.resize(39);
        idx.fill(char(0x00));

        quint64 progressCounter = 0;
        quint64 one_percent = quint64(size / 100.);
        quint64 percent_counter = 0;

        while(!gi.atEnd() && !error) {

            if(percent_counter >= one_percent) {
                progress.setValue(progressCounter);
                percent_counter = 0;
            }

            if(gi.readRawData(idx.data(), 39) < 0) {
                error = true;
                error_code = ERROR_BROKEN_INDEX;
                continue;
            }

            QDataStream ds_entry_i(idx);
            chess::IndexEntry *entry_i = new chess::IndexEntry();

            ds_entry_i >> *entry_i;

            this->indices->append(entry_i);
            // also add to the currentSearchIndex
            this->currentSearchIndices->append(entry_i);

            progressCounter += 39;
            percent_counter += 39;
        }

        dciFile.close();
        if(int(this->loadUponOpen) >= this->indices->size()) {
            this->loadUponOpen = 0;
        }

        if(error == 0) {
            int len = filename.length();
            QString basename = QString(filename).left(len-4);
            this->updateBaseName(basename);
        }

        return error;
    }
}

int chess::Database::loadMetaData(QString &filename,
                 QMap<quint32, QString> *offsetTextdata,
                 QByteArray &magicIndexString, QWidget* parent) {

    offsetTextdata->clear();

    bool error_code = 0;

    QFile dcxFile;
    dcxFile.setFileName(filename);

    if(!dcxFile.exists()) {
        std::cerr << "Error: can't open data file: " << filename.toStdString() << std::endl;
        return ERROR_OPENING_FILE;
    } else {
        dcxFile.open(QFile::ReadOnly);
        QDataStream stream(&dcxFile);
        bool error = false;
        QByteArray magic;
        magic.resize(10);
        magic.fill(char(0x20));
        stream.readRawData(magic.data(), 10);
        if(QString::fromUtf8(magic) != magicIndexString) {
            return ERROR_UNKNOWN_FILETYPE;
        }

        qint64 size = dcxFile.size();
        quint64 progressCounter = 0;
        quint64 one_percent = quint64(size / 100.);
        quint64 percent_counter = 0;

        // set up the progress dialog
        QProgressDialog progress("reading metadata...", "Cancel", 0, size, parent);
        progress.setWindowModality(Qt::WindowModal);
        progress.setCancelButton(0);

        while(!stream.atEnd() && !error) {

            if(percent_counter >= one_percent) {
                progress.setValue(progressCounter);
                percent_counter = 0;
            }

            quint32 pos = quint32(dcxFile.pos());
            QByteArray data_bytes;
            data_bytes.resize(36);
            data_bytes.fill(char(0x20));
            if(stream.readRawData(data_bytes.data(), 36) < 0) {
                error_code = ERROR_BROKEN_FILE;
                error = true;
                break;
            }
            QString data = QString::fromUtf8(data_bytes).trimmed();
            offsetTextdata->insert(pos, data);

            progressCounter += 36;
            percent_counter += 36;
        }
        dcxFile.close();
    }
    return error_code;
}


int chess::Database::countGames() {
    return this->indices->length();
}

chess::Game* chess::Database::getGameFromEntry(chess::IndexEntry *ie) {
    chess::Game* game = new chess::Game();
    QString whiteName = this->offsetNames->value(ie->whiteOffset);
    QString blackName = this->offsetNames->value(ie->blackOffset);
    QString site = this->offsetSites->value(ie->siteRef);
    qDebug() << "White name: " << whiteName;
    QString event = this->offsetEvents->value(ie->eventRef);
    game->setHeader("White",whiteName);
    game->setHeader("Black", blackName);
    game->setHeader("Site", site);
    game->setHeader("Event", event);
    if(ie->eloWhite != 0) {
        game->setHeader("WhiteElo", QString::number(ie->eloWhite));
    }
    if(ie->eloBlack != 0) {
        game->setHeader("BlackElo", QString::number(ie->eloBlack));
    }
    //qDebug() << "EVENT IS: " << event;
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
    game->setHeader("Date", date);
    //qDebug() << "RESULT: " << ie->result;
    if(ie->result == RES_WHITE_WINS) {
        game->setHeader("Result", "1-0");
        game->setResult(RES_WHITE_WINS);
    } else if(ie->result == RES_BLACK_WINS) {
        game->setHeader("Result", "0-1");
        game->setResult(RES_BLACK_WINS);
    } else if(ie->result == RES_DRAW) {
        game->setHeader("Result", "1/2-1/2");
        game->setResult(RES_DRAW);
    } else {
        game->setHeader("Result", "*");
        game->setResult(RES_UNDEF);
    }
    game->setHeader("ECO", ie->eco);
    if(ie->round != 0) {
        game->setHeader("Round", QString::number((ie->round)));
    } else {
        game->setHeader("Round", "?");
    }
    qDebug() << "header load finished";
    QFile fnGames(this->filenameGames);
    if(fnGames.open(QFile::ReadOnly)) {
        bool ok = fnGames.seek(ie->gameOffset);
        if(!ok) {
            std::cerr << "seeking to game offset failed!" << std::endl;
            return game;
        }
        QDataStream gi(&fnGames);
        int length = this->decodeLength(&gi);
        if(length < 0) {
            std::cerr << "length decoding at game offset position failed!" << std::endl;
            return game;
        }
        QByteArray game_raw;
        game_raw.resize(length);
        game_raw.fill(char(0x20));
        gi.readRawData(game_raw.data(), length);
        if(gi.status() != QDataStream::Ok) {
            std::cerr << "reading game bytes at game offset failed!" << std::endl;
            return game;
        }
        this->dcgdecoder.decodeGame(*game, game_raw);
    }
    return game;
}

chess::Game* chess::Database::getGameAt(int i) {

    qDebug() << "get game at called with: " << i;
    if(i >= this->indices->size()) {
        qDebug() << "game not found, index mismatch";
        return nullptr; // maybe throw out of range error or something instead of silently failing
    }
    chess::IndexEntry *ie = this->indices->at(i);
    if(ie->deleted) {
        // todo: jump to next valid entry
    }
    chess::Game* gameAtI = this->getGameFromEntry(ie);
    this->currentOpenGameIdx = i;
    return gameAtI;
}

int chess::Database::decodeLength(QDataStream *stream) {
    quint8 len1 = 0;
    *stream >> len1;
    if(stream->status() != QDataStream::Ok) {
        return -1;
    }
    //qDebug() << "len1 is this: " << QString("%1").arg(len1 , 0, 16);
    if(len1 < 127) {
        return int(len1);
    }
    if(len1 == 0x81) {
        quint8 len2 = 0;
        *stream >> len2;
        if(stream->status() != QDataStream::Ok) {
            return -1;
        } else {
        return int(len2);
        }
    }
    if(len1 == 0x82) {
        quint16 len2 = 0;
        *stream >> len2;
        if(stream->status() != QDataStream::Ok) {
            return -1;
        } else {
            return int(len2);
        }
    }
    if(len1 == 0x83) {
        quint8 len2=0;
        quint16 len3=0;
        *stream >> len2;
        *stream >> len3;
        quint32 ret = 0;
        ret = len2 << 16;
        ret = ret + len3;
        if(stream->status() != QDataStream::Ok) {
            return -1;
        } else {
            return ret;
        }
    }
    if(len1 == 0x84) {
        quint32 len = 0;
        *stream >> len;
        if(stream->status() != QDataStream::Ok) {
            return -1;
        } else {
            return int(len);
        }
    }
    return -1;
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
    const char* encoding = pgnreader.detect_encoding(pgnfile);

    chess::HeaderOffset header;

    quint64 offset = 0;
    bool stop = false;

    std::cout << "scanning at 0";
    int i = 0;
    while(!stop) {
        if(i%100==0) {
            std::cout << "\rscanning at " << offset;
        }
        i++;
        int res = pgnreader.readNextHeader(pgnfile, encoding, offset, header);
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
        //if(header.headers != 0) {
            if(header.headers.contains("Site")) {
                QString site = header.headers.value("Site");
                if(site.size() > 36) {
                    site = site.left(36);
                }
                quint32 key = this->offsetSites->key(site, 4294967295);
                if(key == 4294967295) {
                    sites->insert(header.headers.value("Site"), 0);
                } else {
                    sites->insert(header.headers.value("Site"), key);
                }
            }
            if(header.headers.contains("Event")) {
                QString event = header.headers.value("Event");
                if(event.size() > 36) {
                    event = event.left(36);
                }
                quint32 key = this->offsetSites->key(event, 4294967295);
                if(key == 4294967295) {
                    events->insert(header.headers.value("Event"), 0);
                } else {
                    events->insert(header.headers.value("Event"), key);
                }
            }
            if(header.headers.contains("White")) {
                QString white = header.headers.value("White");
                quint32 key = this->offsetNames->key(white, 4294967295);
                if(white.size() > 36) {
                    white = white.left(36);
                }
                if(key == 4294967295) {
                    names->insert(header.headers.value("White"), 0);
                } else {
                    names->insert(header.headers.value("White"), key);
                }
            }
            if(header.headers.contains("Black")) {
                QString black = header.headers.value("Black");
                if(black.size() > 36) {
                    black = black.left(36);
                }
                quint32 key = this->offsetNames->key(black, 4294967295);
                if(key == 4294967295) {
                    names->insert(header.headers.value("Black"), 0);
                } else {
                    names->insert(header.headers.value("Black"), key);
                }
            }
        //}
    }
    std::cout << std::endl << "scanning finished" << std::flush;
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
            qDebug() << keys.at(i);
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
            //qDebug() << "writing pos: " << offset << " for " << name_i;
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
        //qDebug() << "ADDING OFFSET: " << offset;
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
    chess::HeaderOffset header;
    quint64 offset = 0;
    QFile pgnFile(pgnfile);
    quint64 size = pgnFile.size();
    bool stop = false;

    const char* encoding = this->pgnreader.detect_encoding(pgnfile);

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
                ByteUtil::append_as_uint64(openDefault, this->loadUponOpen);
                //qDebug() << "OFFSET0: " << openDefault.toHex();
                fnIndex.write(openDefault, 8);
            }
            if(fnGames.pos() == 0) {
                fnGames.write(magicGamesString, magicGamesString.length());
            }
            std::cout << "\nsaving games: 0.00 % finished";
            int i = 0;
            while(!stop) {
                if(i%100==0) {
                    if(i%100==0) {
                        QString finished = QString::number( float(offset) * 100. / float(size), 'f', 2 );
                        std::cout << "\rsaving games: "<< finished.toStdString() << " %" << std::flush;
                    }
                }
                i++;
                int res = pgnreader.readNextHeader(pgnfile, encoding, offset, header);
                if(res < 0) {
                    stop = true;
                    continue;
                }
                // the current index entry
                QByteArray iEntry;
                // first write index entry
                // status
                ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                // game offset
                ByteUtil::append_as_uint64(iEntry, fnGames.pos());
                // white offset
                QString white = header.headers.value("White");
                //qDebug() << "WHITE: " << white;
                quint32 whiteOffset = names->value(white);
                ByteUtil::append_as_uint32(iEntry, whiteOffset);
                // black offset
                QString black = header.headers.value("Black");
                quint32 blackOffset = names->value(black);
                //qDebug() << "BLACK: " << black;
                ByteUtil::append_as_uint32(iEntry, blackOffset);
                // round
                quint16 round = header.headers.value("Round").toUInt();
                ByteUtil::append_as_uint16(iEntry, round);
                // site offset
                quint32 site_offset = sites->value(header.headers.value("Site"));
                ByteUtil::append_as_uint32(iEntry, site_offset);
                // event offset
                quint32 event_offset = events->value(header.headers.value("Event"));
                ByteUtil::append_as_uint32(iEntry, event_offset);
                //qDebug() << "EVENT OFFSET: " << event_offset;
                // elo white
                quint16 elo_white = header.headers.value("WhiteElo").toUInt();
                //qDebug() << "elo white: " << elo_white;
                ByteUtil::append_as_uint16(iEntry, elo_white);
                quint16 elo_black = header.headers.value("BlackElo").toUInt();
                ByteUtil::append_as_uint16(iEntry, elo_black);
                // result
                if(header.headers.contains("Result")) {
                    QString res = header.headers.value("Result");
                    if(res == "1-0") {
                        ByteUtil::append_as_uint8(iEntry, quint8(0x01));
                    } else if(res == "0-1") {
                        ByteUtil::append_as_uint8(iEntry, quint8(0x02));
                    } else if(res == "1/2-1/2") {
                        ByteUtil::append_as_uint8(iEntry, quint8(0x03));
                    } else {
                        ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                    }
                } else  {
                    ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                }
                //qDebug() << iEntry.size();
                // ECO
                if(header.headers.contains("ECO")) {
                    QByteArray eco = header.headers.value("ECO").toLatin1().left(3);
                    //qDebug() << eco;
                    iEntry.append(eco);
                } else {
                    QByteArray eco = QByteArrayLiteral("\x00\x00\x00");
                    iEntry.append(eco);
                }
                //qDebug() << iEntry.size();
                // parse date
                if(header.headers.contains("Date")) {
                    QString date = header.headers.value("Date");
                    // try to parse the date
                    quint16 year = 0;
                    quint8 month = 0;
                    quint8 day = 0;
                    QStringList dd_mm_yy = date.split(".");
                    if(dd_mm_yy.size() > 0 && dd_mm_yy.at(0).length() == 4) {
                        quint16 prob_year = dd_mm_yy.at(0).toInt();
                        //qDebug() << "PROb YEAR:" << prob_year;
                        if(prob_year > 0 && prob_year < 2100) {
                            year = prob_year;
                        }
                        if(dd_mm_yy.size() > 1 && dd_mm_yy.at(1).length() == 2) {
                            quint8 prob_month = dd_mm_yy.at(1).toInt();
                            //qDebug() << "prob monath: " << prob_month;
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
                    //qDebug() << "YEAR: " << year;
                    ByteUtil::append_as_uint16(iEntry, year);
                    ByteUtil::append_as_uint8(iEntry, month);
                    ByteUtil::append_as_uint8(iEntry, day);
                } else {
                    ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                    ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                    ByteUtil::append_as_uint8(iEntry, quint8(0x00));
                }
                //qDebug() << iEntry.size();
                assert(iEntry.size() == 39);
                fnIndex.write(iEntry, iEntry.length());
                //qDebug() << "just before reading back file";
                chess::Game* g = pgnreader.readGameFromFile(pgnfile, encoding, header.offset);
                //qDebug() << "READ file ok";
                QByteArray g_enc = dcgencoder.encodeGame(*g); //"<<<<<<<<<<<<<<<<<<<<<< this is the cause of mem acc fault"
                //qDebug() << "enc ok";
                //qDebug() << "writing game: " << g_enc->toHex();
                fnGames.write(g_enc, g_enc.length());
            }
            std::cout << "\rsaving games: "<<size<< "/"<<size << std::endl;
        }
        fnGames.close();
    }
    fnIndex.close();
}

void chess::Database::search(SearchPattern &sp, QWidget *parent) {

    qDebug() << "about to clear search index";

    this->currentSearchIndices->clear();
    qDebug() << "size after clear" << this->currentSearchIndices->size();

    qDebug() << "cleared search index";

    // set up a progress dialog
    int size = this->indices->size();
    QProgressDialog progress("reading index...", "Cancel", 0, size, parent);
<<<<<<< HEAD
    progress.setMinimumDuration(100);
=======
>>>>>>> c3f979a4f133596d772483c0fbd1cac4a0bdc496
    progress.setWindowModality(Qt::WindowModal);
    progress.setCancelButton(0);

    for(int i=0;i<size;i++) {
        progress.setValue(i);
        //do search here
        if(sp.searchGameData) {
            IndexEntry *ei = this->indices->at(i);
            if(!sp.ignoreNameColor) { // look for player names
                if(!sp.whiteName.isEmpty()) {
                    QString eiWhiteName = this->offsetNames->value(ei->whiteOffset);
                    if(!eiWhiteName.contains(sp.whiteName, Qt::CaseInsensitive)) {
                        continue;
                    }
                }
                if(!sp.blackName.isEmpty()) {
                    QString eiBlackName = this->offsetNames->value(ei->blackOffset);
                    if(!eiBlackName.contains(sp.blackName, Qt::CaseInsensitive)) {
                        continue;
                    }
                }
            } else {  // look for player names, but ignore color
                if(!sp.whiteName.isEmpty()) {
                    QString eiWhiteName = this->offsetNames->value(ei->whiteOffset);
                    QString eiBlackName = this->offsetNames->value(ei->blackOffset);
                    if(!eiWhiteName.contains(sp.whiteName, Qt::CaseInsensitive) &&
                       !eiBlackName.contains(sp.whiteName, Qt::CaseInsensitive)) {
                        continue;
                    }
                }
                if(!sp.blackName.isEmpty()) {
                    QString eiWhiteName = this->offsetNames->value(ei->whiteOffset);
                    QString eiBlackName = this->offsetNames->value(ei->blackOffset);
                    if(!eiWhiteName.contains(sp.blackName, Qt::CaseInsensitive) &&
                       !eiBlackName.contains(sp.blackName, Qt::CaseInsensitive)) {
                       continue;
                    }
                }
            }
            // check event
            if(!sp.event.isEmpty()) {
                QString eiEvent = this->offsetEvents->value(ei->eventRef);
                if(!eiEvent.contains(sp.event, Qt::CaseInsensitive)) {
                    continue;
                }
            }
            // site
            if(!sp.site.isEmpty()) {
                QString eiSite = this->offsetSites->value(ei->siteRef);
                if(!eiSite.contains(sp.site, Qt::CaseInsensitive)) {
                    continue;
                }
            }
            if(sp.checkYear) {
                quint16 spYearMin = sp.year_min;
                quint16 spYearMax = sp.year_max;
                if((ei->year != 0)
                        && (spYearMin < ei->year || spYearMax > ei->year)) {
                    continue;
                }
            }
            if(sp.checkEco) {
                if(!(ei->eco >= sp.ecoStart && ei->eco <= sp.ecoStop)) {
                    continue;
                }
            }
            if(sp.checkElo != SEARCH_IGNORE_ELO) {
                if(sp.checkElo == SEARCH_ONE_ELO) {
                    // black and white are outside range
                    if( (ei->eloBlack < sp.elo_min || ei->eloBlack > sp.elo_max)
                            && (ei->eloWhite < sp.elo_min || ei->eloWhite > sp.elo_max) ) {
                        continue;
                    }
                } else {
                    if(sp.checkElo == SEARCH_BOTH_ELO) {
                        // either black or white is out of range
                        if((ei->eloBlack < sp.elo_min || ei->eloBlack > sp.elo_max)
                                || (ei->eloWhite < sp.elo_min || ei->eloWhite > sp.elo_max) ) {
                            continue;
                        }
                    } else {
                        // sp.checkElo must be SEARCH_AVERAGE_ELO
                        quint16 average = quint16 ((float(ei->eloBlack) + float(ei->eloWhite)) / 2.0);
                        // average must be in range, otherwise ignore
                        if(average < sp.elo_min || average > sp.elo_max) {
                            continue;
                        }
                    }
                }
            }
            if(sp.result != chess::RES_ANY) {
                if(sp.result != ei->result) {
                    continue;
                }
            }
            if(sp.checkMoves) {
                chess::Game *gi = this->getGameFromEntry(ei);
                int halfmoves = gi->countHalfmoves();
                int moveMin = std::max( (sp.move_min - 1) * 2, 0);
                int moveMax = sp.move_max * 2;
                delete gi;
                if(halfmoves < moveMin || halfmoves > moveMax) {
                    continue;
                }
            }
<<<<<<< HEAD
=======

>>>>>>> c3f979a4f133596d772483c0fbd1cac4a0bdc496
        }
        if(sp.searchComments) {
            IndexEntry *ei = this->indices->at(i);
            chess::Game *gi = this->getGameFromEntry(ei);
<<<<<<< HEAD
            if(sp.mustNotStartInInitial) {
                if(gi->getRootNode()->getBoard().is_initial_position()) {
                    continue;
                }
            }
            QString text1 = sp.comment_text1;
            QString text2 = sp.comment_text2;
            if(sp.wholeWord && (!sp.comment_text1.isEmpty())) {
                text1 = sp.comment_text1.simplified().prepend((" ")).append(" ");
            }
            if(sp.wholeWord && (!sp.comment_text2.isEmpty())) {
                text2 = sp.comment_text2.simplified().prepend((" ")).append(" ");
            }
            if(!sp.comment_text1.isEmpty()) {
                if(!(gi->hasCommentSubstring(sp.comment_text1, !sp.caseSensitive))) {
                    continue;
                };
            }
            if(!sp.comment_text2.isEmpty()) {
                if(!(gi->hasCommentSubstring(sp.comment_text2, !sp.caseSensitive))) {
                    continue;
                };
            }
            delete gi;
=======

            delete gi;

>>>>>>> c3f979a4f133596d772483c0fbd1cac4a0bdc496
        }

        this->currentSearchIndices->append(this->indices->at(i));
    }

<<<<<<< HEAD

    // just as a test
    //this->currentSearchIndices[0] = this->indices[0];
    //this->currentSearchIndices->append(this->indices->at(0));
=======
    // just as a test
    //this->currentSearchIndices[0] = this->indices[0];
    this->currentSearchIndices->append(this->indices->at(0));
>>>>>>> c3f979a4f133596d772483c0fbd1cac4a0bdc496
    qDebug() << "set first element of search index";

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
