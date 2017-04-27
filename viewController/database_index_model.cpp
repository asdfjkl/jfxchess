#include "database_index_model.h"

DatabaseIndexModel::DatabaseIndexModel(QObject *parent)
    : QAbstractTableModel(parent)
{

}

void DatabaseIndexModel::setIndex(QList<chess::IndexEntry*> *indices)
{
    this->indices = indices;
}


int DatabaseIndexModel::rowCount(const QModelIndex & /* parent */) const
{
    return this->indices->count();
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
        int column = index.column();

        // todo: depending on column return correct result
        return this->indices->at(row)->eco;
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
