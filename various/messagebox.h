#ifndef MESSAGEBOX_H
#define MESSAGEBOX_H

#include <QString>

class MessageBox
{
public:
    MessageBox();
    static void showMessage(QString title, QString content);
};

#endif // MESSAGEBOX_H
