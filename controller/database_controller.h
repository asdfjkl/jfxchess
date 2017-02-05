#ifndef DATABASECONTROLLER_H
#define DATABASECONTROLLER_H

#include <QObject>

class DatabaseController : public QObject
{
    Q_OBJECT
public:
    explicit DatabaseController(QWidget *parent = 0);

signals:

public slots:

    void showDatabase();

};

#endif // DATABASECONTROLLER_H
