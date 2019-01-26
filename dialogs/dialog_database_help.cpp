#include "dialog_database_help.h"
#include <QVBoxLayout>
#include <QLabel>

DialogDatabaseHelp::DialogDatabaseHelp(QWidget *parent, QString version) :
    QDialog(parent)
{
    this->setWindowTitle(this->tr("About"));

    QVBoxLayout *vbox = new QVBoxLayout();
    QHBoxLayout *hbox = new QHBoxLayout();
    QString info = QString("<br><b>Jerry - PGN Database</b><br><br>" \
                              "Version ");
    info.append(version).append("<br>");
    info.append("Copyright © 2014 - 2019<br>" \
                              "Dominik Klein<br>" \
                              "licensed under GNU GPL 2<br><br>");
    QLabel *labelText = new QLabel(info);
    labelText->setAlignment(Qt::AlignCenter);

    QFontMetrics f = this->fontMetrics();
    int m = f.width("m");
    vbox->addWidget(labelText);
    hbox->addSpacing(m*5);
    hbox->addLayout(vbox);
    hbox->addSpacing(m*5);
    this->setLayout(hbox);

}
