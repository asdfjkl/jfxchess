#ifndef DATABASE_INDEX_MODEL_H
#define DATABASE_INDEX_MODEL_H

#include <QAbstractTableModel>
#include "chess/indexentry.h"

const int COLUMN_COUNT = 7;

class DatabaseIndexModel : public QAbstractTableModel
{
public:
    DatabaseIndexModel(QObject *parent = 0);

    void setIndex(QList<chess::IndexEntry*> *indices);
    int rowCount(const QModelIndex &parent) const;
    int columnCount(const QModelIndex &parent) const;
    QVariant data(const QModelIndex &index, int role) const;
    //QVariant headerData(int section, Qt::Orientation orientation,
    //                    int role) const;

private:
    QList<chess::IndexEntry*> *indices;


};

#endif // DATABASE_INDEX_MODEL_H
