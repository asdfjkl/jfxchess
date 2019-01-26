#ifndef DIALOG_DATABASE_HELP_H
#define DIALOG_DATABASE_HELP_H

#include <QDialog>

class DialogDatabaseHelp : public QDialog
{
    Q_OBJECT

public:
    explicit DialogDatabaseHelp(QWidget *parent, QString version);
};

#endif // DIALOG_DATABASE_HELP_H
