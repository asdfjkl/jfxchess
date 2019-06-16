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


#include "dialog_guioptions.h"
#include "viewController/colorstyle.h"
#include <QDialogButtonBox>
#include <QGroupBox>
#include <QRadioButton>
#include <QVBoxLayout>
#include <QDebug>
#include <QTabWidget>
#include "various/resource_finder.h"

DialogGuiOptions::DialogGuiOptions(ColorStyle *currentStyle, FontStyle *fontStyle, QWidget *parent) :
    QDialog(parent) {

    setWindowTitle(tr("Set Options"));

    this->tbs = new TabBoardStyle(currentStyle, this);
    this->tfs = new TabFontStyle(fontStyle, this);
    //this->tcs = new TabCommentSearch(this);
    //this->tsp = new TabSearchPos(gameModel, this);

    QTabWidget *tabWidget = new QTabWidget;
    tabWidget->addTab(tbs, tr("Board Style"));
    tabWidget->addTab(tfs, tr("Font Size"));

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogGuiOptions::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogGuiOptions::reject);

    QVBoxLayout *mainLayout = new QVBoxLayout;
    mainLayout->addWidget(tabWidget);
    mainLayout->addWidget(buttonBox);
    setLayout(mainLayout);

    int h = parent->height();
    this->resize(h*0.7, 1);
}

