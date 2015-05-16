from PyQt4.QtGui import *
from PyQt4.QtCore import *
import sys
from logic.user_settings import Engine
from chess.uci import popen_engine, TimeoutError
from copy import deepcopy

class DialogEngines(QDialog):

    def __init__(self, parent = None, user_settings=None):
        super(DialogEngines,self).__init__(parent)
        self.setWindowTitle(self.trUtf8("Chess Engines"))

        # so that when dialog is not accepted, we can
        # revert to original state w/o side-effects
        self.engines = deepcopy(user_settings.engines)
        self.active_engine = deepcopy(user_settings.active_engine)

        vbox_right = QVBoxLayout()
        btnAdd = QPushButton(self.trUtf8("Add..."))
        btnRemove = QPushButton(self.trUtf8("Remove..."))
        btnParameters = QPushButton(self.trUtf8("Parameters..."))
        vbox_right.addWidget(btnAdd)
        vbox_right.addWidget(btnRemove)
        vbox_right.addStretch(0)
        vbox_right.addWidget(btnParameters)

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
        for engine in self.engines:
            item = QListWidgetItem(engine.name)
            print("adding "+engine.name)
            self.lstEngines.addItem(item)
            if(user_settings.active_engine == engine):
                self.lstEngines.setItemSelected(item,True)

        self.connect(btnAdd,SIGNAL("clicked()"),self.on_add)
        self.connect(self.lstEngines,SIGNAL("currentItemChanged"),self.on_select_engine)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))

        self.setLayout(vbox)

    def on_select_engine(self):
        print("foobar!")

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
                    self.lstEngines.addItem(engine.name)
                except: pass
                finally: pass
                    # todo: better error handling if
                    # a non-chess engine is chosen