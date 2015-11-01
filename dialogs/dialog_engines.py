from copy import deepcopy

from PyQt4.QtGui import *
from PyQt4.QtCore import *

from model.user_settings import Engine
from chess.uci import popen_engine
from dialogs.dialog_engine_options import DialogEngineOptions


class DialogEngines(QDialog):

    def __init__(self, parent = None, user_settings=None):
        super(DialogEngines,self).__init__(parent)
        self.setWindowTitle(self.trUtf8("Chess Engines"))

        # so that when dialog is not accepted, we can
        # revert to original state w/o side-effects
        self.engines = deepcopy(user_settings.engines)
        idx = user_settings.engines.index(user_settings.active_engine)
        # should point to the same from copied engines(!)
        self.active_engine = self.engines[idx]

        vbox_right = QVBoxLayout()
        btnAdd = QPushButton(self.trUtf8("Add..."))
        self.btnRemove = QPushButton(self.trUtf8("Remove..."))
        btnParameters = QPushButton(self.trUtf8("Edit Parameters..."))
        btnResetParameters = QPushButton(self.trUtf8("Reset Parameters..."))
        vbox_right.addWidget(btnAdd)
        vbox_right.addWidget(self.btnRemove)
        vbox_right.addStretch(0)
        vbox_right.addWidget(btnParameters)
        vbox_right.addWidget(btnResetParameters)

        hbox_up = QHBoxLayout()
        self.lstEngines = QListWidget()
        hbox_up.addStretch(0)
        hbox_up.addWidget(self.lstEngines)
        hbox_up.addLayout(vbox_right)
        hbox_up.addStretch(0)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)

        vbox = QVBoxLayout()
        vbox.addLayout(hbox_up)
        vbox.addWidget(buttonBox)

        #self.lstEngines.addItems([engine.name for engine in engines])
        for idx,engine in enumerate(self.engines):
            item = None
            if(idx == 0):
                item = QListWidgetItem(engine.name+" (internal)")
            else:
                item = QListWidgetItem(engine.name)
            self.lstEngines.addItem(item)
            # made a deepcopy for self.engines, hence
            # need to check active engine of user-config
            # by index of user_settings_engines
            if(user_settings.active_engine == user_settings.engines[idx]):
                item.setSelected(True)
                # if internal one is active, deactivate remove button
                if(idx == 0):
                    self.btnRemove.setEnabled(False)

        self.connect(btnAdd,SIGNAL("clicked()"),self.on_add)
        self.connect(self.btnRemove,SIGNAL("clicked()"),self.on_remove)
        self.connect(btnParameters,SIGNAL("clicked()"),self.on_parameters)
        self.connect(btnResetParameters,SIGNAL("clicked()"),self.on_reset)
        self.connect(self.lstEngines,SIGNAL("itemSelectionChanged()"),self.on_select_engine)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))

        self.setLayout(vbox)

    def on_reset(self):
        self.active_engine.options = []

    def on_select_engine(self):
        for i in range(0,self.lstEngines.count()):
            if(self.lstEngines.item(i).isSelected()):
                self.active_engine = self.engines[i]
                if(i==0): # deactivate remove button if internal
                          # engine is selected
                    self.btnRemove.setEnabled(False)
                else:
                    self.btnRemove.setEnabled(True)

    def on_parameters(self):
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


    def on_remove(self):
        for i in range(0,self.lstEngines.count()):
            # find selected engine
            if(self.lstEngines.item(i).isSelected()):
                # delete selected item from widget and from engine list
                del self.engines[i]
                # removeItem won't update GUI state - bug in Qt 4.8
                self.lstEngines.takeItem(i)
                # set the active engine to the previous in the list
                # (always exists, since internal can't be deleted
                self.active_engine = self.engines[i-1]
                self.lstEngines.item(i-1).setSelected(True)
                self.lstEngines.update()


    def on_add(self):
        dialog = QFileDialog()
        dialog.setFilter(QDir.Executable | QDir.Files)
        filename = dialog.getOpenFileName(self, self.trUtf8('Select Engine'), None, '',QFileDialog.DontUseNativeDialog)
        if(filename):
                engine = Engine()
                engine.path = filename
                try:
                    eng = popen_engine(filename)
                    command = eng.uci(async_callback=True)
                    command.result(timeout=1.5)
                    engine.name = eng.name
                    self.engines.append(engine)
                    item = QListWidgetItem(engine.name)
                    self.lstEngines.addItem(item)
                    item.setSelected(True)
                    eng.quit()
                    #self.active_engine = engine
                except BaseException as e:
                    print(e)
                finally: pass
                    # todo: better error handling if
                    # a non-chess engine is chosen