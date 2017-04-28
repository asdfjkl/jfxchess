#include "database_index_model.h"
#include "chess/game.h"

DatabaseIndexModel::DatabaseIndexModel(QObject *parent)
    : QAbstractTableModel(parent)
{

}

void DatabaseIndexModel::setDatabase(chess::Database *database)
{
    this->database = database;
}

int DatabaseIndexModel::rowCount(const QModelIndex & /* parent */) const
{
    return this->database->indices->count();
}


int DatabaseIndexModel::columnCount(const QModelIndex & /* parent */) const
{
    return COLUMN_COUNT;
}

QVariant DatabaseIndexModel::data(const QModelIndex &index, int role) const
{
    if (!index.isValid())
        return QVariant();

    if (role == Qt::TextAlignmentRole) {
        return int(Qt::AlignRight | Qt::AlignVCenter);
    } else if (role == Qt::DisplayRole) {

        int row = index.row();
        chess::IndexEntry *entry_row = this->database->indices->at(row);

        int column = index.column();

        // todo: depending on column return correct result
        // White (ELO) | Black (Elo) | Site | Event (Round) | YYYYMMDD | Eco | Result
        if(column == 0) {
            QString whiteName = this->database->offsetNames->value(entry_row->whiteOffset);
            QString whiteElo = QString::number(entry_row->eloWhite);
            QString tableEntry = QString(whiteName).append(" (").append(whiteElo).append(")");
            return tableEntry;
        }
        if(column == 1) {
            QString blackName = this->database->offsetNames->value(entry_row->blackOffset);
            QString blackElo = QString::number(entry_row->eloBlack);
            QString tableEntry = QString(blackName).append(" (").append(blackElo).append(")");
            return tableEntry;
        }
        if(column == 2) {
            QString site = this->database->offsetSites->value(entry_row->siteRef);
            return site;
        }
        if(column == 3) {
            QString event = this->database->offsetEvents->value(entry_row->eventRef);
            QString round = QString::number(entry_row->round);
            QString tableEntry = QString(event).append(" (").append(round).append(")");
            return tableEntry;
        }
        if(column == 4) {
            QString date("");
             if(entry_row->year != 0) {
                date.append(QString::number(entry_row->year).rightJustified(4,'0'));
             } else {
                date.append("????");
            }
            date.append(".");
            if(entry_row->month != 0) {
                date.append(QString::number(entry_row->month).rightJustified(2,'0'));
            } else {
                date.append("??");
            }
            date.append(".");
            if(entry_row->day != 0) {
                date.append(QString::number(entry_row->day).rightJustified(2,'0'));
            } else {
                date.append("??");
            }
            return date;
        }
        if(column == 5) {
            return entry_row->eco;
        }
        if(column == 6) {
            QString result("");
            if(entry_row->result == chess::RES_WHITE_WINS) {
                result.append("1-0");
            } else if(entry_row->result == chess::RES_BLACK_WINS) {
                result.append("0-1");
            } else if(entry_row->result == chess::RES_DRAW) {
                result.append("1/2-1/2");
            } else {
                result.append("*");
            }
            return result;
        }
    }
    return QVariant();
}

/*
QVariant DatabaseIndexModel::headerData(int section,
                                   Qt::Orientation, // orientation
                                   int role) const
{
    if (role != Qt::DisplayRole)
        return QVariant();
    return
}

*/
