/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#include "dialog_about.h"
#include <QVBoxLayout>
#include <QLabel>

DialogAbout::DialogAbout(QWidget *parent, QString version) :
    QDialog(parent)
{

    this->setWindowTitle(this->tr("About"));
    QVBoxLayout *vbox = new QVBoxLayout();
    QHBoxLayout *hbox = new QHBoxLayout();
    QString info = QString("<br><b>Jerry</b><br><br>" \
                              "Version ");
    info.append(version).append("<br>");
    info.append("Copyright © 2014 - 2019<br>" \
                              "Dominik Klein<br>" \
                              "licensed under GNU GPL 2<br><br>" \
                              "<b>Credits</b><br><br>" \
                              "Stockfish Chess Engine<br>" \
                              "by the Stockfish-Team<br><br>" \
                              "'VARIED.BIN' opening book<br>" \
                              "by Marc Lacrosse/Jose-Chess Tool<br><br>" \
                              "Piece Images<br>" \
                              "from Raptor Chess Interface<br><br>" \
                              "all licensed under GNU GPL 2<br>");
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
