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


from PyQt4.QtCore import *
import re
import queue
from time import sleep


class Uci_engine(QThread):

    COMMON_WORDS_THRESHOLD = 250
    MIN_WORD_LEN = 3
    MAX_WORD_LEN = 25
    INVALID_FIRST_OR_LAST = frozenset("0123456789_")
    STRIPHTML_RE = re.compile(r"<[^>]*?>", re.IGNORECASE|re.MULTILINE)
    ENTITY_RE = re.compile(r"&(\w+?);|&#(\d+?);")
    SPLIT_RE = re.compile(r"\W+", re.IGNORECASE|re.MULTILINE)

    def __init__(self, lock, parent=None):
        super(Uci_engine, self).__init__(parent)
        self.mutex_readyok = QMutex()
        self.remaining_readyok = 0

        self.mutex_sending = QMutex()

        self.sent_go_infinite = False

        self.re_readyok = re.compile("readyok")

        self.command_queue = queue.Queue()

    def ping_engine(self):
        # ping the engine with isready,
        # then wait, until the engine
        # has responded to _all_ isready
        # calls
        print("pinging start")
        self.mutex_readyok.lock()
        self.process.write(bytes("isready\n","utf-8"))
        self.process.waitForBytesWritten()
        self.remaining_readyok += 1
        self.mutex_readyok.unlock()
        print("pinged")

    def send_to_stdin(self):
        msg = self.command_queue.get(False)
        # ensure that if send_to_stdin
        # is called in parallel, we execute
        # in a serial fashion
        self.mutex_sending.lock()
        print("locked")

        # if the engine is in infinite mode,
        # first send a stop command
        if(self.sent_go_infinite):
            self.process.write(bytes("stop\n","utf-8"))
            self.process.waitForBytesWritten()
            self.sent_go_infinite = False



        #self.process.waitForReadyRead()
        print("finished")

        print("rem "+str(self.remaining_readyok))
            #sleep(10)
            #self.mutex_readyok.lock()

        # finally send the command
        # if the command contains "go infinite"
        # remember that for the next time, so that
        # stop can be sent before issuing the next
        # command
        if(msg == "go infinite"):
            self.sent_go_infinite = True

        print("before sending")
        # finally issue the command
        self.process.write(bytes(msg+"\n","utf-8"))
        self.process.waitForBytesWritten()

        self.mutex_sending.unlock()


    def on_std_out(self):
        print("sig received+++")
        output = str(self.process.readAllStandardOutput(),"utf-8")
        print("output: "+output)
        lines = output.splitlines()
        # process output
        for line in lines:
            if(self.re_readyok.match(line)):
                print("received readok")
                self.mutex_readyok.lock()
                self.remaining_readyok -= 1
                if(self.remaining_readyok == 0 and not self.command_queue.empty()):
                    self.send_to_stdin()
                self.mutex_readyok.unlock()
            else:
                self.emit(SIGNAL("new_std_out(QString)"),line)


    def readErrors(self):
        output = str(self.process.readLineStderr())
        print("error: "+output)
        #self.emit(SIGNAL("boo(QString"),output)


    def run(self):
        print("I am starting\n")
        self.process = QProcess()
        self.process.start("./stockfish-5-64")
        self.process.connect(self.process, SIGNAL("readyReadStandardOutput()"),self.on_std_out)
        self.exec_()
        print("after exit")
        self.process.close()
        print("after close in run")
        self.process.waitForFinished(-1)

        print("after waiting in run")
        self.exit()
        #self.process.close()
        #self.stop()

        #exec("")

        #while True:
        #    time.sleep(0.1)
        #    self.emit(SIGNAL("boo(QString)"),(str(fx)))
        #self.proc.stdin.write(bytes("uci\n", "ascii"))

