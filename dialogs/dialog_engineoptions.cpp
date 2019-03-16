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

#include <QProcess>
#include <QTime>
#include <QCoreApplication>
#include <QRegularExpression>
#include <QDebug>
#include <QFrame>
#include <QGridLayout>
#include <QLabel>
#include <QDialogButtonBox>
#include "dialog_engineoptions.h"

DialogEngineOptions::DialogEngineOptions(Engine &e, QWidget *parent) :
    QDialog(parent), engine(e)
{

    this->setWindowTitle(this->tr("UCI Engine Options: ").append(e.getName()));
    QVector<EngineOption> uciOptions = this->engine.getUciOptions();

    this->getOptionsFromEngine();

    QGridLayout *grid = new QGridLayout();
    int count = this->engine.getUciOptions().count();
    int rowwidth = count / 4;
    int x = 0;
    int y = 0;

    uciOptions = this->engine.getUciOptions();
    for(int i=0;i<uciOptions.size();i++) {
        EngineOption ei = uciOptions.at(i);
        if(!ei.name.startsWith("UCI_") &&
                !(ei.name == QString("Hash") || ei.name == QString("NalimoPath") ||
                  ei.name == QString("NalimovCache") || ei.name == QString("Ponder") ||
                  ei.name == QString("Ownbook") || ei.name == "MultiPV" ||  ei.name == "Skill Level")
                )
        {
            if(y >= rowwidth) {
                y = 0;
                x ++;
            }
            // crude way of adding spacing
            if(y!=0) {
                grid->addWidget(new QLabel("    ", this),x,y);
                y++;
            }
            y++;

            QLabel *lbl = new QLabel(ei.name, this);
            y++;
            grid->addWidget(lbl,x,y);
            y++;
            if(ei.type == EN_OPT_TYPE_SPIN) {
                QSpinBox *widget = new QSpinBox(this);
                widget->setMinimum(ei.min_spin);
                widget->setMaximum(ei.max_spin);
                widget->setValue(ei.spin_val);
                grid->addWidget(widget,x,y);
                this->spin_widgets.insert(ei.name, widget);
            } else if(ei.type == EN_OPT_TYPE_CHECK) {
                QCheckBox *widget = new QCheckBox(this);
                widget->setChecked(ei.check_val);
                grid->addWidget(widget,x,y);
                this->check_widgets.insert(ei.name,widget);
            } else if(ei.type == EN_OPT_TYPE_COMBO) {
                QComboBox *widget = new QComboBox(this);
                QString active_setting = ei.combo_val;
                for(int i=0;i<ei.combo_options.count();i++) {
                    widget->addItem(ei.combo_options.at(i));
                    if(ei.combo_options.at(i) == active_setting) {
                        widget->setCurrentIndex(i);
                    }
                }
                grid->addWidget(widget,x,y);
                this->combo_widgets.insert(ei.name,widget);
            } else if(ei.type == EN_OPT_TYPE_STRING) {
                QLineEdit *widget = new QLineEdit(this);
                widget->setText(ei.string_val);
                grid->addWidget(widget,x,y);
                this->line_widgets.insert(ei.name,widget);
            }
            y++;
        }
    }

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel, this);
    QVBoxLayout *vbox = new QVBoxLayout();
    vbox->addLayout(grid);

    vbox->addWidget(this->hLine());
    vbox->addWidget(buttonBox);

    this->setLayout(vbox);
    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogEngineOptions::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogEngineOptions::reject);
}

void DialogEngineOptions::updateEngineOptionsFromEntries() {

    // iterate through all widgets and update values from
    // current state of widgets
    // first start with spin widgets
    QMapIterator<QString, QSpinBox*> i1(this->spin_widgets);
    QVector<EngineOption> en_opts = this->engine.getUciOptions();
    while (i1.hasNext()) {
        i1.next();
        QString name = i1.key();
        int j = this->existsEngineOption(en_opts, name);
        if(j!=-1) {
            this->engine.setUciSpinOption(j,i1.value()->value());
            //this->engine->getUciOptions()->at(j)->spin_val = i1.value()->value();
        }
    }
    // same spiel for combo widgets
    QMapIterator<QString, QComboBox*> i2(this->combo_widgets);
    while (i2.hasNext()) {
        i2.next();
        QString name = i2.key();
        int j = this->existsEngineOption(en_opts, name);
        if(j!=-1) {
            this->engine.setUciComboOption(j, i2.value()->currentText());
            //this->engine->getUciOptions()->at(j)->combo_val = i2.value()->currentText();
        }
    }
    // and check widgets
    // same spiel for combo widgets
    QMapIterator<QString, QCheckBox*> i3(this->check_widgets);
    while (i3.hasNext()) {
        i3.next();
        QString name = i3.key();
        int j = this->existsEngineOption(en_opts, name);
        if(j!=-1) {
            this->engine.setUciCheckOption(j, i3.value()->isChecked());
            //this->engine->getUciOptions()->at(j)->check_val = i3.value()->isChecked();
        }
    }
    // finally line edits
    QMapIterator<QString, QLineEdit*> i4(this->line_widgets);
    while (i4.hasNext()) {
        i4.next();
        QString name = i4.key();
        int j = this->existsEngineOption(en_opts, name);
        if(j!=-1) {
            this->engine.setUciStringOption(j, i4.value()->text());
            //this->engine->getUciOptions()->at(j)->string_val = i4.value()->text();
        }
    }
}

void DialogEngineOptions::delay(int ms)
{

    QTime dieTime= QTime::currentTime().addMSecs(ms);
    while (QTime::currentTime() < dieTime)
        QCoreApplication::processEvents(QEventLoop::AllEvents, 100);

}


QFrame* DialogEngineOptions::hLine() {

    QFrame *line = new QFrame(this);
    line->setFrameShape(QFrame::HLine);
    line->setFrameShadow(QFrame::Sunken);
    return line;

}



// return -1 if not found, otherwise index of list
int DialogEngineOptions::existsEngineOption(QVector<EngineOption> &options, QString &name) {
    for(int i=0;i<options.count();i++) {
        EngineOption ei = options.at(i);
        if(ei.name == name) {
            return i;
        }
    }
    return -1;
}

// get all options by querying engine
// if option already exists take value from existing engine
// otherwise create new option with queried default values
void DialogEngineOptions::getOptionsFromEngine() {

    this->setEnabled(false);

    QVector<EngineOption> engine_options = this->engine.getUciOptions();
    // execute engine, call uci, parse options
    QProcess process;
    process.start(this->engine.getPath(),QIODevice::ReadWrite);
    // Wait for process to start
    if(!process.waitForStarted(500)) {
        // if process doesn't start, just ignore
    } else {
        process.write("uci\n");
        process.waitForBytesWritten();
        // give the engine 700 ms to respond to
        // the uci command
        this->delay(700);

        // read generated output
        QString output = QString("");
        // give another 50 ms until the engine outputs info
        process.waitForReadyRead(50) ;
        output.append(process.readAllStandardOutput());
        // look for all possible options
        QStringList output_lines = output.split("\n");
        for(int i=0; i<output_lines.count();i++) {
            QString output_i = output_lines.at(i);
            QRegularExpressionMatch m_opt = REG_EXP_OPTION_NAME.match(output_i);
            if(m_opt.hasMatch() && !m_opt.captured(1).isNull()) {
                QString opt_name = m_opt.captured(1);
                // if options already exists, delete current entry in list
                // and append it (to preserve order. otherwise just process
                // as new option
                int opt_idx = this->existsEngineOption(engine_options, opt_name);
                if(opt_idx >= 0) {
                    EngineOption eo = engine_options.at(opt_idx);
                    engine_options.removeAt(opt_idx);
                    engine_options.append(eo);
                } else {
                    // means engine was queried first time i.e. option doesn't exist yet
                    // spin option
                    QRegularExpression regExpTypeSpin = QRegularExpression(".*?type spin default (\\d+) min (\\d+) max (\\d+)");
                    QRegularExpressionMatch m_spin = regExpTypeSpin.match(output_i);
                    if(m_spin.hasMatch()) {
                        int def = m_spin.captured(1).toInt();
                        int min = m_spin.captured(2).toInt();
                        int max = m_spin.captured(3).toInt();
                        EngineOption new_spin;
                        new_spin.default_spin = def;
                        new_spin.min_spin = min;
                        new_spin.max_spin = max;
                        new_spin.type = EN_OPT_TYPE_SPIN;
                        new_spin.name = opt_name;
                        new_spin.spin_val = new_spin.default_spin;
                        engine_options.append(new_spin);
                    }
                    // check option
                    QRegularExpression regExpTypeCheck = QRegularExpression(".*?type check default (true|false)");
                    QRegularExpressionMatch m_check = regExpTypeCheck.match(output_i);
                    if(m_check.hasMatch()) {
                        bool default_check = false;
                        QString default_check_s = m_check.captured(1);
                        if(default_check_s == QString("true")) {
                            default_check = true;
                        }
                        EngineOption new_check;
                        new_check.default_check = default_check;
                        new_check.type = EN_OPT_TYPE_CHECK;
                        new_check.name = opt_name;
                        new_check.check_val = new_check.default_check;
                        engine_options.append(new_check);
                    }
                    QRegularExpression regExpTypeCombo = QRegularExpression(".*?type combo default ([a-zA-Z0-9_ ]*)");
                    QRegularExpressionMatch m_combo = regExpTypeCombo.match(output_i);
                    if(m_combo.hasMatch() && !m_combo.captured(1).isNull()) {
                        QStringList def_plus_vals = m_combo.captured(1).split(" ");
                        EngineOption new_combo;
                        QVector<QString> combo_options;
                        new_combo.default_combo = def_plus_vals.at(0);
                        int size = def_plus_vals.count();
                        for(int i=1;i<size;i++) {
                            if(def_plus_vals.at(i) == QString("var") && i+1 < size) {
                                combo_options.append(def_plus_vals.at(i+1));
                            }
                        }
                        new_combo.type = EN_OPT_TYPE_COMBO;
                        new_combo.combo_options = combo_options;
                        new_combo.combo_val = new_combo.default_combo;
                        new_combo.name = opt_name;
                        //this->engine.addEngineOption(new_combo);
                        engine_options.append(new_combo);
                    }
                    QRegularExpression regExpTypeString = QRegularExpression(".*?type string default ([a-zA-Z0-9\\.]*)");
                    QRegularExpressionMatch m_string= regExpTypeString.match(output_i);
                    if(m_string.hasMatch() && !m_string.captured(1).isNull()) {
                        EngineOption new_string;
                        new_string.type = EN_OPT_TYPE_STRING;
                        new_string.default_string = m_string.captured(1);
                        new_string.string_val = new_string.default_string;
                        new_string.name = opt_name;
                        engine_options.append(new_string);
                    }
                }
            }

        }
        // attempt to quit the engine
        process.write("quit\n");
        process.waitForBytesWritten();
        process.waitForFinished(250);
        // if still running, kill it
        if(process.state()  == QProcess::Running) {
            process.kill();
            process.waitForFinished();
        } else {
            // engine quit gracefully.
        }
    }
    this->engine.setEngineOptions(engine_options);
    this->setEnabled(true);

}
