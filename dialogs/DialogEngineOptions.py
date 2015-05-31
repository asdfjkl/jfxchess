from PyQt4.QtGui import *
from PyQt4.QtCore import *
import sys
from logic.user_settings import Engine
from chess.uci import popen_engine, TimeoutError
from copy import deepcopy
from logic.user_settings import InternalEngine

class DialogEngineOptions(QDialog):

    def HLine(self):
        toto = QFrame()
        toto.setFrameShape(QFrame.HLine)
        toto.setFrameShadow(QFrame.Sunken)
        return toto

    def __init__(self, engine, parent = None):
        super(DialogEngineOptions,self).__init__(parent)
        self.setWindowTitle(self.trUtf8("UCI Engine Options: ")+engine.name)

        # first parse all options form uci response
        eng = popen_engine(engine.path)
        command = eng.uci(async_callback=True)
        command.result(timeout=1.5)
        # eng.options
        # create sorted shallow copy
        eng_opts = []
        for key in eng.options:
            eng_opts.append(eng.options[key])
        eng_opts.sort(key=lambda x: x.name)

        self.optionWidgets = []
        grid = QGridLayout()
        count = len(eng.options)
        rowwidth = count/4
        x = 0
        y = 0
        for opt in eng_opts:
            #opt = eng.options[key]
            # add a widget for that option depending on its type
            # we will ignore options where the name starts with UCI_
            # or is one of the following:
            #   Hash, NalimovPath, NalimovCache, Ponder, Ownbook, MultiPV
            # (see UCI protocol specification)
            # in addition:
            # - for internal engine also block 'Skill Level' (handled at other place via GUI)
            # - settings of button type (maybe implement later, but for now we just
            #   save all settings, and send them to the gui right before engine is
            #   activated via switching to resp. game mode, and not while in this dialog
            if not opt.name.startswith('UCI_') and not \
                (opt.name == 'Hash' or opt.name == 'NalimovPath' or \
                 opt.name == 'NalimovCache' or opt.name == 'Ponder' or \
                 opt.name == 'Ownbook' or opt.name =='MultiPV') and not \
                (type(engine) == InternalEngine and opt.name == 'Skill Level') and \
                not opt.type == 'button':

                if(y >= rowwidth):
                    y = 0
                    x += 1
                # crude way of adding spacing
                if(not y==0):
                    grid.addWidget(QLabel("    "),x,y)
                    y +=1

                # label for option
                lbl = QLabel(opt.name)
                grid.addWidget(lbl,x,y)
                # actual widget for manipulating value
                if opt.type == 'spin':
                    widget = QSpinBox()
                    widget.setMinimum(opt.min)
                    widget.setMaximum(opt.max)
                    if(engine.exists_option_value(opt.name)):
                        widget.setValue(engine.get_option_value(opt.name))
                    else:
                        widget.setValue(opt.default)
                elif opt.type == 'check':
                    widget = QCheckBox()
                    if(engine.exists_option_value(opt.name)):
                        widget.setChecked(engine.get_option_value(opt.name))
                    else:
                        widget.setChecked(opt.default)
                elif opt.type == 'combo':
                    widget = QComboBox()
                    if(engine.exists_option_value(opt.name)):
                        active_setting = engine.get_option_value(opt.name)
                    else:
                        active_setting = opt.default
                    for idx,val in enumerate(opt.var):
                        widget.addItem(val)
                        if(val == active_setting):
                            widget.setCurrentIndex(idx)
                #elif opt.type == 'button':
                #    widget = QPushButton(opt.name)
                elif opt.type == 'string':
                    widget = QLineEdit()
                    if(engine.exists_option_value(opt.name)):
                        active_setting = engine.get_option_value(opt.name)
                    else:
                        active_setting = opt.default
                    widget.setText(active_setting)
                y += 1
                grid.addWidget(widget,x,y)
                self.optionWidgets.append((opt,widget))
                y+=1
                #x+=1            #

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        vbox = QVBoxLayout()
        vbox.addLayout(grid)

        vbox.addWidget(self.HLine())
        #vbox.addLayout(hbox)
        vbox.addWidget(buttonBox)

        eng.quit()
        self.setLayout(vbox)

        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))