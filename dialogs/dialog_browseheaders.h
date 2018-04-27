#ifndef DIALOG_BROWSERHEADERS_H
#define DIALOG_BROWSERHEADERS_H

#include <QDialog>
#include <QTableWidget>
#include <QLineEdit>
#include <QPushButton>
#include "chess/pgn_reader.h"

class DialogBrowseHeaders : public QDialog
{
    Q_OBJECT
public:
    explicit DialogBrowseHeaders(QList<chess::HeaderOffset> &header_offsets,
                                 QString &filename, QWidget *parent = 0);
    int gameOffset;

private:
    QList<chess::HeaderOffset> &header_offsets;
    QStringList *tableHeaders;
    QTableWidget *table;
    void drawAllItems();
    QLineEdit *searchField;
    QPushButton *btnSearch;
    QPushButton *btnReset;

signals:

public slots:
    void onItemSelectionChanged();
    void onSearch();
    void onResetSearch();
};

#endif // DIALOG_BROWSERHEADERS_H
