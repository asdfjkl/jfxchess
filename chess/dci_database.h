#ifndef DCI_DATABASE_H
#define DCI_DATABASE_H


#include <QString>
#include "chess/pgn_reader.h"
#include "chess/dcgencoder.h"
#include "chess/dcgdecoder.h"
#include "chess/indexentry.h"
#include "chess/game.h"
#include "chess/constants.h"
#include "chess/database.h"
#include "model/search_pattern.h"

#include <QElapsedTimer>

namespace chess {

class DCIDatabase : public Database
{
public:
    DCIDatabase(QWidget* parent);
    ~DCIDatabase();

    void open(QString &filename);
    void search(SearchPattern &sp);
    int countGames();

    void setParentWidget(QWidget *parentWidget) = 0;
    void close() = 0;
    void exportDB(QString &outFilename, QVector<int> &indices, int outType) = 0;
    QString getFilename();
    // next functions are w.r.t. the current active index
    int getRowCount() = 0;
    DatabaseRowInfo getRowInfo(int idx) = 0;
    Game* getGameAtAbsoluteIndex(int idx) = 0;

    void reset();

    void importPgnAndSave(QString &pgnfile);
    void saveToFile();

    int loadIndex(QString &filename, QWidget* parent = 0);
    int loadMetaData(QString &filename, QMap<quint32, QString> *offsetTextdata,
                     QByteArray &magicIndexString, QWidget* parent=0);


    chess::Game* getGameAt(int i);
    chess::Game* getGameFromEntry(chess::IndexEntry *ie);

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
    // to properly display progress dialogs
    QWidget *parentWidget;

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

    QByteArray cacheData;
    bool cacheValid;

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

    QElapsedTimer global_timer;
    qint64 ns_seek;
    qint64 ns_decode_header;
    qint64 ns_decode_length;
    qint64 ns_decode_game;

};

}

#endif // DCI_DATABASE_H
