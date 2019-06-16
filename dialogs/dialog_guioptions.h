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


#ifndef DIALOG_GUIOPTIONS_H
#define DIALOG_GUIOPTIONS_H

#include <QDialog>
//#include "viewController/simple_displayboard.h"
#include "viewController/pickcolorboard.h"
#include "model/font_style.h"
#include "dialogs/tab_board_style.h"
#include "dialogs/tab_font_style.h"

class DialogGuiOptions : public QDialog
{
    Q_OBJECT
public:
    explicit DialogGuiOptions(ColorStyle *currentStyle, FontStyle *fontStyle, QWidget *parent = 0);
    //SimpleDisplayBoard* displayBoard;
    //PickColorBoard* displayBoard;

private:
    TabBoardStyle *tbs;
    TabFontStyle *tfs;

signals:

public slots:

};

#endif // DIALOG_GUIOPTIONS_H
