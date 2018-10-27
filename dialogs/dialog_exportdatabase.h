#ifndef DIALOG_EXPORTDATABASE_H
#define DIALOG_EXPORTDATABASE_H

#include <QDialog>
#include <QGroupBox>
#include <QRadioButton>

class DialogExportDatabase : public QDialog
{
    Q_OBJECT

public:
    DialogExportDatabase(QWidget *parent = 0);
    QRadioButton *radioFormatDci;
    QRadioButton *radioFormatPgn;
    QRadioButton *radioCurrentSelection;
    QRadioButton *radioAllGameSelection;

private:
    QGroupBox *groupBoxFormat;
    QGroupBox *groupBoxSelection;

};

#endif // DIALOG_EXPORTDATABASE_H
