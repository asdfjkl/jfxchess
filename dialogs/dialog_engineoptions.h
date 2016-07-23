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

#ifndef DIALOG_ENGINEOPTIONS_H
#define DIALOG_ENGINEOPTIONS_H

#include <QDialog>
#include <QList>
#include <QFrame>
#include <QRegularExpression>
#include <QSpinBox>
#include <QCheckBox>
#include <QComboBox>
#include <QLineEdit>
#include "model/engine_option.h"
#include "model/engine.h"

//const QRegularExpression REG_EXP_OPTION_NAME = QRegularExpression("option name (.*?) type");

class DialogEngineOptions : public QDialog
{
    Q_OBJECT
public:
    explicit DialogEngineOptions(Engine *e, QWidget *parent = 0);
    void updateEngineOptionsFromEntries();

private:
    QMap<QString,QSpinBox*> *spin_widgets;
    QMap<QString,QCheckBox*> *check_widgets;
    QMap<QString,QComboBox*> *combo_widgets;
    QMap<QString,QLineEdit*> *line_widgets;
    QFrame* hLine();
    Engine *engine;
    void getOptionsFromEngine();
    int existsEngineOption(QList<EngineOption*> *options, QString &name);
    void delay(int ms);

signals:

public slots:

};

#endif // DIALOG_ENGINEOPTIONS_H
