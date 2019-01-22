#include "database_index_model.h"
#include "chess/game.h"
#include <QFont>
#include <QDebug>

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

    //qDebug() << "ROW COUNT: " << this->database->currentSearchIndices->count();
    //return this->database->currentSearchIndices->count();
    return this->database->getRowCount();
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
        //int column = index.column();
        /*
        if(column == 0 || column == 1 || column == 2) {
            return int(Qt::AlignLeft | Qt::AlignVCenter);
        }
        if(column == 3 || column == 4 || column == 5) {
            return int(Qt::AlignCenter | Qt::AlignVCenter);
        }*/
        // just always align left (same as header)
        return int(Qt::AlignLeft | Qt::AlignVCenter);
    } else if (role == Qt::DisplayRole) {
        int row = index.row();
        chess::PgnHeader rowInfo = this->database->getRowInfo(row);
        //chess::IndexEntry *entry_row = this->database->currentSearchIndices->at(row);

        int column = index.column();

        // todo: depending on column return correct result
        // White (ELO) | Black (Elo) | Event (Round) | Eco | Year | Result
        if(column == 0) {
            //QString whiteName = this->database->offsetNames->value(entry_row->whiteOffset);
            QString tableEntry = QString(rowInfo.white);
            return tableEntry;
        }
        if(column == 1) {
            QString tableEntry = QString(rowInfo.black);
            return tableEntry;
        }
        if(column == 2) {
            QString tableEntry = QString(rowInfo.event);
            if(rowInfo.round != "0") {
                tableEntry.append(" (Round ").append(rowInfo.round).append(")");
            }
            return tableEntry;
        }
        /*
        if(column == 3) {
            return rowInfo.eco;
        }*/
        if(column == 4) {
            QString tableEntry = QString(rowInfo.date);
            return tableEntry;
        }
        if(column == 5) {
            return rowInfo.result;
                    /*
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
            return result;*/
        }
    } else if(role == Qt::FontRole) {
        int row = index.row();
        chess::PgnHeader rowInfo = this->database->getRowInfo(row);
        /*
        if(rowInfo.isDeleted) {
            QFont defaultFont;
            defaultFont.setStrikeOut(true);
            defaultFont.setItalic(true);
            return defaultFont;
        }*/
        return QVariant();
    }
    return QVariant();
}

QVariant DatabaseIndexModel::headerData(int section,
                                   Qt::Orientation orientation,
                                   int role) const
{
    if (role != Qt::DisplayRole)
        return QVariant();
    if(orientation == Qt::Horizontal) {
        if(section == 0) {
            return QString("White");
        }
        if(section == 1) {
            return QString("Black");
        }
        if(section == 2) {
            return QString("Event");
        }
        if(section == 3) {
            return QString("ECO");
        }
        if(section == 4) {
            return QString("Date");
        }
        if(section == 5) {
            return QString("Result");
        }

    }
    if(orientation == Qt::Vertical) {
        return QString::number(section);
    }

    return QVariant();
}
