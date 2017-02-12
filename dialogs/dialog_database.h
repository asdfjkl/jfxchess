#ifndef DIALOGDATABASE_H
#define DIALOGDATABASE_H

#include <QDialog>
#include <QTableWidget>

class DialogDatabase : public QDialog
{
    Q_OBJECT
public:
    explicit DialogDatabase(QWidget *parent = 0);

private:
    void resizeTo(float ratio);
    void drawAllItems();

    QTableWidget *gameTable;
    QStringList *gameTableHeaders;
    QPushButton *btnOpenGame;
    QPushButton *btnCancel;

signals:

public slots:

};


#endif // DIALOGDATABASE_H
