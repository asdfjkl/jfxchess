#!/usr/bin/env python3
# Copyright (c) 2008-10 Qtrac Ltd. All rights reserved.
# This program or module is free software: you can redistribute it and/or
# modify it under the terms of the GNU General Public License as published
# by the Free Software Foundation, either version 2 of the License, or
# version 3 of the License, or (at your option) any later version. It is
# provided for educational purposes and is distributed in the hope that
# it will be useful, but WITHOUT ANY WARRANTY; without even the implied
# warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
# the GNU General Public License for more details.

import collections
import sys
from PyQt4.QtCore import *
from PyQt4.QtGui import *
import uci_engine as uci_engine

class Form(QDialog):

    def __init__(self, parent=None):
        super(Form, self).__init__(parent)

        self.lock = QReadWriteLock()
        layout = QVBoxLayout()
        self.text = QPlainTextEdit()
        layout.addWidget(self.text)

        self.start = QPushButton("start engine")
        layout.addWidget(self.start)
        self.ucinewgame = QPushButton("send uci newgame")
        layout.addWidget(self.ucinewgame)
        self.ucisendpos = QPushButton("send pos")
        layout.addWidget(self.ucisendpos)
        self.uciinf = QPushButton("go infinite")
        layout.addWidget(self.uciinf)
        self.stop = QPushButton("stop")
        layout.addWidget(self.stop)
        self.send = QPushButton("send")
        layout.addWidget(self.send)
        self.setLayout(layout)

        self.connect(self.stop, SIGNAL("clicked()"),self.stop_engine)
        self.connect(self.send, SIGNAL("clicked()"),self.start_engine)
        self.connect(self.ucinewgame, SIGNAL("clicked()"),self.uci_newgame)
        self.connect(self.ucisendpos, SIGNAL("clicked()"),self.uci_send_position)
        self.connect(self.uciinf, SIGNAL("clicked()"),self.uci_go_infinite)
        self.connect(self.start, SIGNAL("clicked()"),self.start_engine)

        self.engine = None
        self.setWindowTitle("UCI Engine Controller")

        print("starting up")

    def new_engine_output(self,msg):
        self.text.setPlainText(str(msg))

    def new_err_output(self,msg):
        print("error: "+msg)

    def stop_engine(self):
        self.engine.quit();
        self.engine.wait();

    def start_engine(self):
        self.engine = uci_engine.Uci_engine(self)
        self.connect(self.engine, SIGNAL("new_std_out(QString)"), self.new_engine_output,Qt.QueuedConnection)
        self.engine.start()

    def uci_newgame(self):
        self.engine.command_queue.put("ucinewgame")
        self.engine.ping_engine()

    def uci_send_position(self):
        self.engine.command_queue.put("position startpos moves e2e4 e7e5 f2f4")
        self.engine.ping_engine()

    def uci_go_infinite(self):
        self.engine.command_queue.put("go infinite")
        self.engine.ping_engine()


app = QApplication(sys.argv)
form = Form()
form.show()
app.exec_()

