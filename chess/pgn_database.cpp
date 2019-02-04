#include "pgn_reader.h"
#include "pgn_database.h"
#include <QDebug>
#include <QFile>
#include <QProgressDialog>
#include <QTextCodec>
#include "chess/pgn_printer.h"
#include <iostream>

chess::PgnDatabase::PgnDatabase()
{
    this->parentWidget = nullptr;
    this->filename = "";
    //this->cacheSize = 50;
    this->isUtf8 = true;
    this->currentlyOpen = false;
    this->lastSelectedIndex = 0;
}

chess::PgnDatabase::~PgnDatabase() {

}

bool chess::PgnDatabase::isOpen() {
    return this->currentlyOpen;
}

int chess::PgnDatabase::createNew(QString &filename) {

    QFile file(filename);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        return -1;
    } else {
        QTextStream out(&file);
        out << "\n";
        file.close();

        this->filename = filename;
        this->allOffsets.clear();
        this->searchedOffsets.clear();
        this->currentlyOpen = true;
        return 0;
    }
}


void chess::PgnDatabase::setLastSelectedIndex(int idx) {
    if(idx > 0 && idx < this->searchedOffsets.size()) {
        this->lastSelectedIndex = idx;
    }
}

int chess::PgnDatabase::getLastSelectedIndex() {
    return this->lastSelectedIndex;
}

int chess::PgnDatabase::appendCurrentGame(chess::Game &game) {

    QFile file(this->filename);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Append)) {
        return -1;
    } else {
        QTextStream out(&file);
        out << "\n";
        qint64 gamePos = out.pos();
        chess::PgnPrinter printer;
        QStringList pgn = printer.printGame(game);
        for (int i = 0; i < pgn.size(); ++i) {
            out << pgn.at(i) << '\n';
        }
        file.close();
        this->allOffsets.append(gamePos);
        this->searchedOffsets.clear();
        this->searchedOffsets = this->allOffsets;
        this->lastSelectedIndex = this->countGames() - 1;
        return 0;
    }


}

void chess::PgnDatabase::setParentWidget(QWidget *parentWidget) {
    this->parentWidget = parentWidget;
}

QVector<qint64> chess::PgnDatabase::scanPgn(QString &filename, bool isLatin1) {

    QVector<qint64> offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return offsets;

    bool inComment = false;

    qint64 game_pos = -1;

    QByteArray byteLine;
    QString line("");
    qint64 last_pos = file.pos();

    int size = file.size();
    QProgressDialog progress(this->parentWidget->tr("scanning PGN file..."), this->parentWidget->tr("Cancel"), 0, size, this->parentWidget);
    progress.setMinimumDuration(400);
    progress.setWindowModality(Qt::WindowModal);
    //progress.setCancelButton(0);
    //progress.show();

    quint64 stepCounter = 0;

    int i= 0;
    while(!file.atEnd()) {

        if(progress.wasCanceled()) {
            break;
        }

        if(stepCounter %50 == 0) {
            progress.setValue(last_pos);
            stepCounter = 0;
        }
        stepCounter += 1;

        i++;
        byteLine = file.readLine();
        if(isLatin1) {
            line = QString::fromLatin1(byteLine);
        } else {
            line = QString::fromUtf8(byteLine);
        }

        // skip comments
        if(line.startsWith("%")) {
            byteLine = file.readLine();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            //QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            //if(match_t.hasMatch()) {

                if(game_pos == -1) {
                    game_pos = last_pos;
                }
                last_pos = file.pos();
                byteLine = file.readLine();
                continue;
            //}
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            offsets.append(game_pos);
            game_pos = -1;
        }

        last_pos = file.pos();
        byteLine = file.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        offsets.append(game_pos);
        game_pos = -1;
    }

    return offsets;
}


void chess::PgnDatabase::open(QString &filename) {
    qDebug() << "pgn database: open";

    this->isUtf8 = reader.detectUtf8(filename);
    /*
    const char* utf8 = "UTF-8";
    const char* encoding = reader.detect_encoding(filename);
    int cmp = strcmp(encoding, utf8);
    if(cmp != 0){
        isLatin1 = true;
    }*/
    this->allOffsets = this->scanPgn(filename, this->isUtf8);
    this->searchedOffsets = this->allOffsets;
    this->filename = filename;
    this->currentlyOpen = true;
    this->lastSelectedIndex = -1;
}

void chess::PgnDatabase::close() {
    this->allOffsets.clear();
    this->searchedOffsets.clear();
    this->filename = "";
    this->currentlyOpen = false;
    this->lastSelectedIndex = -1;
}

int chess::PgnDatabase::getRowCount() {
    //qDebug() << "pgn database: get row count" << this->offsets.size();

    return this->searchedOffsets.size();
}

chess::Game* chess::PgnDatabase::getGameAt(int idx) {
    const char* encoding = reader.detect_encoding(filename);
    chess::Game *g = this->reader.readGameFromFile(this->filename, encoding, this->searchedOffsets.at(idx));
    return g;
}

bool chess::PgnDatabase::pgnHeaderMatches(QFile &file, SearchPattern &pattern, qint64 offset) {


    bool foundHeader = false;
    bool continueSearch = true;

    QByteArray byteLine;
    QString line;
    file.seek(offset);

    QString pattern_year_min = QString::number(pattern.year_min);
    QString pattern_year_max = QString::number(pattern.year_max);

    QString elo_min = QString::number(pattern.elo_min);
    QString elo_max = QString::number(pattern.elo_max);

    int whiteElo = -1;
    int blackElo = -1;

    QString whiteName = "";
    QString blackName = "";

    while(!file.atEnd() && continueSearch) {
        byteLine = file.readLine();
        line = QString::fromUtf8(byteLine);
        if(line.startsWith("%") || line.isEmpty()) {
            byteLine = file.readLine();
            continue;
        }

        QRegularExpressionMatch match_t = TAG_REGEX.match(line);

        if(match_t.hasMatch()) {

            foundHeader = true;

            QString tag = match_t.captured(1);
            QString value = match_t.captured(2);

            if(!pattern.event.isEmpty() && tag == "Event") {
                if(!value.contains(pattern.event, Qt::CaseInsensitive)) {
                        return false;
                }
            }
            if(!pattern.site.isEmpty() && tag == "Site") {
                if(!value.contains(pattern.site, Qt::CaseInsensitive)) {
                    return false;
                }
            }
            if(pattern.checkYear && tag == "Date") {
                QString year_i = value.left(4);
                if(year_i < pattern_year_min) {
                    return false;
                }
                if(year_i > pattern_year_max) {
                    return false;
                }
            }
            if(pattern.checkEco && tag == "ECO") {
                if(value < pattern.ecoStart || value > pattern.ecoStop) {
                    return false;
                }
            }
            if(tag == "WhiteElo") {
                whiteElo = value.toInt();
            }
            if(tag == "blackElo") {
                blackElo = value.toInt();
            }
            if(tag == "White") {
                whiteName = value;
            }
            if(tag == "Black") {
                blackName = value;
            }
            if(pattern.result != RES_ANY) {
                if(pattern.result == RES_BLACK_WINS && !value.contains("0-1")) {
                    return false;
                } else if(pattern.result == RES_DRAW && !value.contains("1/2-1/2")) {
                    return false;
                } else if(pattern.result == RES_UNDEF && !value.contains("*")) {
                    return false;
                } else if(pattern.result == RES_WHITE_WINS && !value.contains("1-0")) {
                    return false;
                }
            }
        } else {
            if(foundHeader) {
                if(pattern.ignoreNameColor) {
                    if(!pattern.whiteName.isEmpty()) {
                        // must match either black or white
                        if(!whiteName.contains(pattern.whiteName, Qt::CaseInsensitive)
                           && !blackName.contains(pattern.whiteName, Qt::CaseInsensitive)) {
                            return false;
                        }
                    }
                    if(!pattern.blackName.isEmpty()) {
                        // must match either black or white
                        if(!whiteName.contains(pattern.blackName, Qt::CaseInsensitive)
                           && !blackName.contains(pattern.blackName, Qt::CaseInsensitive)) {
                            return false;
                        }
                    }
                } else {
                    // if whiteName is not empty, then it must match
                    if(!pattern.whiteName.isEmpty() &&
                            !whiteName.contains(pattern.whiteName, Qt::CaseInsensitive)) {
                        return false;
                    }
                    if(!pattern.blackName.isEmpty() &&
                            !blackName.contains(pattern.blackName, Qt::CaseInsensitive)) {
                        return false;
                    }
                }
                if(pattern.checkElo != SEARCH_IGNORE_ELO) {
                    // only if we could find elo information for both players
                    if(whiteElo > 0 && blackElo > 0) {
                        if(pattern.checkElo == SEARCH_AVERAGE_ELO) {
                            int avg = (whiteElo + blackElo) / 2;
                            if(pattern.elo_min > avg || pattern.elo_max < avg) {
                                return false;
                            }
                        } else if(pattern.checkElo == SEARCH_ONE_ELO) {
                            if((pattern.elo_min > whiteElo || pattern.elo_max < whiteElo) &&
                                    (pattern.elo_min > blackElo || pattern.elo_max < blackElo)) {
                                return false;
                            }
                        } else if(pattern.checkElo == SEARCH_BOTH_ELO) {
                            if(pattern.elo_min > whiteElo || pattern.elo_max < whiteElo ||
                                pattern.elo_min > blackElo || pattern.elo_max < blackElo) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        }
    }
    return false;
}



bool chess::PgnDatabase::pgnHeaderMatches1(QTextStream &openStream, SearchPattern &pattern, qint64 offset) {

    bool foundHeader = false;
    bool continueSearch = true;

    openStream.seek(offset);
    QString line = openStream.readLine();
    while(!openStream.atEnd() && continueSearch) {
        line = openStream.readLine();
        if(line.startsWith("%") || line.isEmpty()) {
            line = openStream.readLine();
            continue;
        }

        QRegularExpressionMatch match_t = TAG_REGEX.match(line);

        if(match_t.hasMatch()) {

            foundHeader = true;

            QString tag = match_t.captured(1);
            QString value = match_t.captured(2);

            /*
            if(tag == "Event") {
                if(!value.contains(pattern.event, Qt::CaseInsensitive)) {
                    return false;
                }
            }
            if(tag == "Site") {
                if(!value.contains(pattern.site, Qt::CaseInsensitive)) {
                    return false;
                }
            }*/
            if(tag == "Date") {
                // todo: compare date
            }
            if(tag == "Round") {
                // todo
            }
            if(tag == "White") {
                //qDebug() << "checking: " << pattern.whiteName << " " << value;
                if(!value.contains(pattern.whiteName, Qt::CaseInsensitive)) {
                    //qDebug() << "false";
                    return false;
                } else {
                    //qDebug() << "cont";
                    return true;
                }
            }
            /*
            if(tag == "Black") {
                if(!value.contains(pattern.blackName, Qt::CaseInsensitive)) {
                    return false;
                }
            }*/
            if(tag == "Result") {
                // todo
            }
            if(tag == "ECO") {
                // todo
            }
        } else {
            if(foundHeader) {

                return true;
                //continueSearch = false;
                //break;
            }
        }
    }
    return false;
}


void chess::PgnDatabase::search(SearchPattern &pattern) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }

    this->searchedOffsets.clear();

    QProgressDialog progress(this->parentWidget->tr("searching..."), this->parentWidget->tr("Cancel"), 0, this->countGames(), this->parentWidget);
    progress.setMinimumDuration(400);
    progress.setWindowModality(Qt::WindowModal);
    //progress.setCancelButton(0);
    //progress.show();

    quint64 stepCounter = 0;
    qDebug() << pattern.whiteName;

    for(int i=0;i<this->allOffsets.size();i++) {

        if(progress.wasCanceled()) {
            break;
        }

        if(stepCounter %50 == 0) {
            progress.setValue(i);
            stepCounter = 0;
        }
        stepCounter += 1;

        qint64 offset_i = this->allOffsets.at(i);
        if(this->pgnHeaderMatches(file, pattern, offset_i)) {
            this->searchedOffsets.append(offset_i);
        }
    }
    file.close();
}


/*
void chess::PgnDatabase::search(SearchPattern &pattern) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }
    QTextStream in(&file);
    QTextCodec *codec;
    if(isUtf8) {
        codec = QTextCodec::codecForName("UTF-8");
    } else {
        codec = QTextCodec::codecForName("ISO 8859-1");
    }
    in.setCodec(codec);

    this->searchedOffsets.clear();

    QProgressDialog progress(this->parentWidget->tr("searching..."), this->parentWidget->tr("Cancel"), 0, this->countGames(), this->parentWidget);
    progress.setMinimumDuration(400);
    progress.setWindowModality(Qt::WindowModal);
    progress.setCancelButton(0);
    progress.show();

    quint64 stepCounter = 0;
    qDebug() << pattern.whiteName;

    for(int i=0;i<this->allOffsets.size();i++) {


        if(stepCounter %50 == 0) {
            progress.setValue(i);
            stepCounter = 0;
        }
        stepCounter += 1;

        qint64 offset_i = this->allOffsets.at(i);
        if(this->pgnHeaderMatches(in, pattern, offset_i)) {
            this->searchedOffsets.append(offset_i);
        }
    }
    file.close();
}*/


/*
chess::PgnHeader chess::PgnDatabase::getRowInfo(int idx) {

    if(this->headerCache.contains(idx)) {
        qDebug() << "in cache";
        chess::PgnHeader header = headerCache.value(idx);
        return header;
    } else {
        int start = std::max(idx - 10, 0);
        int stop = std::min(this->offsets.size(), idx + 30);
        QVector<qint64> cacheOffsets;
        for(int i=start; i<=stop;i++) {
            qint64 oi = this->offsets.at(i);
            cacheOffsets.append(oi);
        }
        const char* utf8 = "UTF-8";
        chess::PgnHeader h;
        if(idx >= this->offsets.size()) {
            return h;
        } else {

            QVector<PgnHeaderOffset> cacheHeaderOffsets = this->reader.readMultipleHeadersFromPgnAround(this->filename, cacheOffsets, utf8);
            for(int i=0;i<cacheHeaderOffsets.size();i++) {
                chess::PgnHeader ci = cacheHeaderOffsets.at(i).header;
                qint64 ii = cacheHeaderOffsets.at(i).offset;
                this->headerCache.insert(ii, ci);
                if(ii == this->offsets.at(idx)) {
                    h = cacheHeaderOffsets.at(i).header;
                }
            }
            return h;

            //qint64 offset = this->offsets.at(idx);
            //chess::PgnHeader h_idx = this->reader.readSingleHeaderFromPgnAt(this->filename, offset, utf8);
            //return h_idx;
        }
    }
}
*/

chess::PgnHeader chess::PgnDatabase::getRowInfo(int idx) {

        chess::PgnHeader h;
        if(idx >= this->searchedOffsets.size()) {
            return h;
        } else {

            qint64 offset = this->searchedOffsets.at(idx);
            chess::PgnHeader h_idx = this->reader.readSingleHeaderFromPgnAt(this->filename, offset, this->isUtf8);
            return h_idx;
        }
}

void chess::PgnDatabase::resetSearch() {
    this->searchedOffsets.clear();
    this->searchedOffsets = this->allOffsets;
    this->lastSelectedIndex = -1;
}


int chess::PgnDatabase::countGames() {
    //qDebug() << "pgn database: count games";

    return this->allOffsets.size();
}
