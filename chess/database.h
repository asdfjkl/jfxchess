#ifndef DATABASE_H
#define DATABASE_H

#include <QString>
#include "chess/pgn_reader.h"
#include "chess/dcgencoder.h"
#include "chess/dcgdecoder.h"
#include "chess/indexentry.h"
#include "chess/game.h"
#include "chess/constants.h"
#include "model/search_pattern.h"

namespace chess {

class Database
{
public:
    Database(QString &filename);
    ~Database();

    void open(QWidget* parent);

    void reset();

    void importPgnAndSave(QString &pgnfile);
    void saveToFile();

    int loadIndex(QString &filename, QWidget* parent = 0);
    int loadMetaData(QString &filename, QMap<quint32, QString> *offsetTextdata,
                     QByteArray &magicIndexString, QWidget* parent=0);

    void search(SearchPattern &sp, QWidget* parent=nullptr);

    chess::Game* getGameAt(int i);
    chess::Game* getGameFromEntry(chess::IndexEntry *ie);
    int countGames();
    QList<chess::IndexEntry*> *indices;
    QList<chess::IndexEntry*> *currentSearchIndices;

    void updateBaseName(QString &basename);

    QMap<quint32, QString> *offsetNames;
    QMap<quint32, QString> *offsetSites;
    QMap<quint32, QString> *offsetEvents;

    QString lastOpenDir;
    int currentOpenGameIdx;
    QString filenameIndex;

private:
    // filename is only the base, always append *.dcs, *.dcn, *.dcg, *.dci
    QString filenameBase;
    QString filenameNames;
    QString filenameSites;
    QString filenameEvents;
    QString filenameGames;
    QByteArray magicNameString;
    QByteArray magicIndexString;
    QByteArray magicGamesString;
    QByteArray magicSitesString;
    QByteArray magicEventString;
    QByteArray version;

    void writeSites();
    void writeNames();
    void writeIndex();
    void writeGames();
    // scans all headers in pgn file and reads names and sites into passed maps
    void importPgnNamesSitesEvents(QString &pgnfile,
                                   QMap<QString, quint32> *names,
                                   QMap<QString, quint32> *sites,
                                   QMap<QString, quint32> *events);
    void importPgnAppendNames(QMap<QString, quint32> *names);
    void importPgnAppendSites(QMap<QString, quint32> *sites);
    void importPgnAppendEvents(QMap<QString, quint32> *events);
    void importPgnAppendGamesIndices(QString &pgnfile,
                                     QMap<QString, quint32> *names,
                                     QMap<QString, quint32> *sites,
                                     QMap<QString, quint32> *events);

    int decodeLength(QDataStream *stream);
    chess::DcgEncoder dcgencoder;
    chess::DcgDecoder dcgdecoder;
    chess::PgnReader pgnreader;

    quint64 loadUponOpen;
};

}

#endif // DATABASE_H
