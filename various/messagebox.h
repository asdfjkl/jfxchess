#ifndef MESSAGEBOX_H
#define MESSAGEBOX_H

#include <QString>
#include <QWidget>

class MessageBox
{
public:
    MessageBox(QWidget *parent);
    void showMessage(QString title, QString content);

private:
    QWidget *parent;

};

#endif // MESSAGEBOX_H
