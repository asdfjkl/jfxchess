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
import walker_26 as walker


class Form(QDialog):

    def __init__(self, parent=None):
        super(Form, self).__init__(parent)

        self.lock = QReadWriteLock()
        layout = QVBoxLayout()
        self.text = QPlainTextEdit()
        layout.addWidget(self.text)

        #self.start = QPushButton("start")
        #layout.addWidget(self.start)
        #self.stop = QPushButton("stop")
        #layout.addWidget(self.stop)
        self.send = QPushButton("send")
        layout.addWidget(self.send)
        self.setLayout(layout)

        #self.walker = walker.Walker(self.lock, self)
        #self.connect(self.walker, SIGNAL("boo(QString)"), self.boo)
        #self.connect(self.start, SIGNAL("clicked()"),self.start_walker)
        #self.connect(self.stop, SIGNAL("clicked()"),self.kill_it)
        self.connect(self.send, SIGNAL("clicked()"),self.send_command)
        self.process = QProcess()
        self.process.start("./stockfish-5-64")
        self.process.connect(self.process, SIGNAL("readyReadStandardOutput()"),self.on_out)

        self.setWindowTitle("Page Indexer")


    def on_out(self):
        output = str(self.process.readAllStandardOutput())
        print("got output: "+output)
        self.text.setPlainText(output)


    def boo(self,msg):
        print("received:"+str(msg))
        self.text.setPlainText(str(msg))

    def kill_it(self):
        self.walker.kill()

    def start_walker(self):
        self.walker.initialize("/foo")
        self.walker.start()

    def send_command(self):
        self.process.write(bytes("uci\n", "utf-8"))

app = QApplication(sys.argv)
form = Form()
form.show()
app.exec_()

