from PyQt4.QtGui import *
from PyQt4.QtCore import *
import sys
from logic.user_settings import Engine
from chess.uci import popen_engine, TimeoutError
from copy import deepcopy

class DialogEngineOptions(QDialog):

    def __init__(self, engine, parent = None):
        super(DialogEngineOptions,self).__init__(parent)
        self.setWindowTitle(self.trUtf8("UCI Engine Options: ")+engine.name)

        # first parse all options form uci response
        eng = popen_engine(engine.path)
        command = eng.uci(async_callback=True)
        command.result(timeout=1.5)
        #eng.options

        optionWidgets = []
        grid = QGridLayout()
        count = len(eng.options)
        rowwidth = count/3
        x = 0
        y = 0
        for key in eng.options:
            if(y >= rowwidth):
                y = 0
                x += 1
            # crude way of adding spacing
            if(not y==0):
                grid.addWidget(QLabel("    "),x,y)
                y +=1
            # now add option
            opt = eng.options[key]
            lbl = QLabel(opt.name)
            grid.addWidget(lbl,x,y)
            spinbox = QSpinBox()
            y += 1
            grid.addWidget(spinbox,x,y)
            optionWidgets.append((opt.name,spinbox))
            y+=1
            #x+=1            #

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        vbox = QVBoxLayout()
        vbox.addLayout(grid)
        vbox.addWidget(buttonBox)

        eng.quit()
        self.setLayout(vbox)