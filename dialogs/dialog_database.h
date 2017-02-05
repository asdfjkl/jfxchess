#ifndef DIALOGDATABASE_H
#define DIALOGDATABASE_H

#include <QDialog>

class DialogDatabase : public QDialog
{
    Q_OBJECT
public:
    explicit DialogDatabase(QWidget *parent = 0);

private:
    void resizeTo(float ratio);

signals:

public slots:

};


#endif // DIALOGDATABASE_H
