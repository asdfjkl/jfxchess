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


#include <QDialogButtonBox>
#include <QGridLayout>
#include "dialog_plaintext.h"

DialogPlainText::DialogPlainText(QWidget *parent) :
    QDialog(parent)
{
    this->setWindowTitle(this->tr("Edit Comment"));
    this->plainTextEdit = new QPlainTextEdit();
    this->savedText = QString("");

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);
    QGridLayout *layout = new QGridLayout();
    layout->addWidget(this->plainTextEdit,0,1);
    layout->addWidget(buttonBox, 3, 0, 1, 3);
    this->setLayout(layout);
    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogPlainText::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogPlainText::reject);
    connect(this->plainTextEdit, &QPlainTextEdit::textChanged, this, &DialogPlainText::updateText);
}

void DialogPlainText::updateText() {
    QString temp = this->plainTextEdit->toPlainText();
    this->savedText = temp.replace("\n", " ").replace("{", " ").replace("} ", " ").replace("\r", " ");
}
