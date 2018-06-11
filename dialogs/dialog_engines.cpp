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

#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QPushButton>
#include <QListWidget>
#include <QDialogButtonBox>
#include <QDebug>
#include <QFileDialog>
#include <QProcess>
#include <QTime>
#include <QCoreApplication>
#include "dialog_engines.h"
#include "dialog_engineoptions.h"
#include "model/engine_option.h"

DialogEngines::DialogEngines(GameModel *gameModel, QWidget *parent) :
    QDialog(parent)
{

    this->setWindowTitle(this->trUtf8("Chess Engines"));
    // create copy of engines, so that if user
    // clicks cancel later on, nothing is changed
    this->engines = gameModel->getEngines();
    this->activeEngineIdx = gameModel->getActiveEngineIdx();

    this->lastAddedEnginePath = gameModel->getLastAddedEnginePath();

    QVBoxLayout *vbox_right = new QVBoxLayout();

    this->btnAdd = new QPushButton(this->tr("Add..."),this);
    this->btnRemove = new QPushButton(this->tr("Remove..."),this);
    this->btnParameters = new QPushButton(this->tr("Edit Parameters..."),this);
    this->btnResetParameters = new QPushButton(this->tr("Reset Parameters..."),this);
    vbox_right->addWidget(btnAdd);
    vbox_right->addWidget(btnRemove);
    vbox_right->addStretch(0);
    vbox_right->addWidget(btnParameters);
    vbox_right->addWidget(btnResetParameters);


    QHBoxLayout *hbox_up = new QHBoxLayout();
    this->lstEngines = new QListWidget(this);
    hbox_up->addStretch(0);
    hbox_up->addWidget(lstEngines);
    hbox_up->addLayout(vbox_right);
    hbox_up->addStretch(0);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel, this);

    QVBoxLayout *vbox = new QVBoxLayout();
    vbox->addLayout(hbox_up);
    vbox->addWidget(buttonBox);

    for(int i=0;i<this->engines.size();i++) {
        QListWidgetItem *item;
        Engine engine_i = this->engines.at(i);
        QString engine_name_i = engine_i.getName();
        if(i==0) {
            engine_name_i.append((" (internal"));
            item = new QListWidgetItem(engine_name_i);
        } else {
            item = new QListWidgetItem(engine_name_i);
        }
        this->lstEngines->addItem(item);
        if(i == gameModel->getActiveEngineIdx()) {
            item->setSelected(true);
            if(i == 0) {
                this->btnRemove->setEnabled(false);
            }
        }
    }

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogEngines::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogEngines::reject);
    connect(this->btnAdd,&QPushButton::clicked, this, &DialogEngines::onAddEngine);
    connect(this->btnRemove,&QPushButton::clicked, this, &DialogEngines::onRemoveEngine);
    connect(this->btnParameters,&QPushButton::clicked, this, &DialogEngines::onEditParameters);
    connect(this->btnResetParameters,&QPushButton::clicked, this, &DialogEngines::onResetParameters);
    connect(this->lstEngines, &QListWidget::itemSelectionChanged, this, &DialogEngines::onSelectEngine);

    this->setLayout(vbox);

}

void DialogEngines::delay(int ms)
{
    QTime dieTime= QTime::currentTime().addMSecs(ms);
    while (QTime::currentTime() < dieTime)
        QCoreApplication::processEvents(QEventLoop::AllEvents, 100);
}

void DialogEngines::onAddEngine() {

    QString fileName = QFileDialog::getOpenFileName(this,
        tr("Add UCI Engine"), this->lastAddedEnginePath, tr("UCI Engines (*)"));

    fileName = QString('"').append(fileName).append('"');

    QDir d = QFileInfo(fileName).absoluteDir();
    this->lastAddedEnginePath = d.absolutePath();

    this->setEnabled(false);

    QProcess process;
    process.start(fileName,QIODevice::ReadWrite);
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
        // look for engine id
        QString engine_name = QString("");
        QRegularExpression regExpEngineName = QRegularExpression("id\\sname\\s(\\w|\\s|\\S)+");
        QRegularExpressionMatch m_id = regExpEngineName.match(output);
        if(m_id.hasMatch()) {
            int len = m_id.capturedLength(0);
            engine_name = m_id.captured(0).mid(8,len-1).split("\n").at(0);
        }
        // attempt to quit the engine
        process.write("quit\n");
        process.waitForBytesWritten();
        process.waitForFinished(250);
        // if still running, kill it
        if(process.state()  == QProcess::Running) {
            // if engine doesn't response, it could mean that
            // this is no engine _or_ (as in the case of e.g arasanx-64
            // takes an extremely long time to respond to "quit".
            // kill it ...
            process.kill();
            process.waitForFinished();
        }
        // ... however even if we had to kill the engine, as
        // long as the engine provided us with a proper name, we
        // assume that we found a real uci engine
        if(!engine_name.isEmpty()) {
            Engine new_engine = Engine();
            new_engine.setName(engine_name);
            new_engine.setPath(fileName);
            this->engines.append(new_engine);
            QListWidgetItem *item = new QListWidgetItem(new_engine.getName());
            this->lstEngines->addItem(item);
            item->setSelected(true);
            this->update();
        }
    }
    this->setEnabled(true);

}

void DialogEngines::onRemoveEngine() {
    for(int i=0;i<this->lstEngines->count();i++) {
        if(this->lstEngines->item(i)->isSelected()) {
            this->engines.removeAt(i);
            this->lstEngines->takeItem(i);
            this->activeEngineIdx = i-1;
            this->lstEngines->item(i-1)->setSelected(true);
            this->lstEngines->update();
        }
    }
}

void DialogEngines::onEditParameters() {
    Engine active = this->engines.at(this->activeEngineIdx);
    DialogEngineOptions *dlg = new DialogEngineOptions(active, this);
    //Engine active = this->engines.at(this->activeEngineIdx);
    //DialogEngineOptions dlg(active, this);

    if(dlg->exec() == QDialog::Accepted) {
        dlg->updateEngineOptionsFromEntries();
        this->engines[this->activeEngineIdx] = active;
    }
    //delete dlg;
    /*
    dlgEngOpt = DialogEngineOptions(self.active_engine)
    if dlgEngOpt.exec_() == QDialog.Accepted:
        # first delete all engine options from active
        # engine, then
        # pick up those engine options that are
        # different from default values, and store
        # them in engine.options
        self.active_engine.options = []
        for opt,widget in dlgEngOpt.optionWidgets:
            if type(widget) == QSpinBox:
                if(not widget.value() == opt.default):
                    self.active_engine.options.append((opt.name,opt.type,widget.value()))
            elif type(widget) == QCheckBox:
                if(not (widget.isChecked() == opt.default)):
                    self.active_engine.options.append((opt.name,opt.type,widget.isChecked()))
            elif type(widget) == QComboBox:
                if(not widget.currentText == opt.default):
                    self.active_engine.options.append((opt.name,opt.type,widget.currentText()))
            elif type(widget) == QLineEdit:
                if(not widget.text() == opt.default):
                    self.active_engine.options.append((opt.name,opt.type,widget.text()))
*/
}

void DialogEngines::onResetParameters() {
    this->engines[this->activeEngineIdx].clearAllEngineOptions();
}

void DialogEngines::onSelectEngine() {
    for(int i=0;i<this->lstEngines->count();i++) {
        if(this->lstEngines->item(i)->isSelected()) {
            //this->active_engine = this->engines.at(i);
            this->activeEngineIdx = i;
            if(i==0) {
                this->btnRemove->setEnabled(false);
            } else {
                this->btnRemove->setEnabled(true);
            }
        }
    }
}
