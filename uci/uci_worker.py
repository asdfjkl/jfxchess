from PyQt4.QtCore import *
import queue
from uci.engine_info import EngineInfo
import gc
import re
from copy import deepcopy
import time

processes = set([])

class Uci_worker(QObject):

    MOVES = re.compile('\s[a-z]\d[a-z]\d([a-z]{0,1})')
    BESTMOVE = re.compile('bestmove\s[a-z]\d[a-z]\d[a-z]{0,1}')
    STRENGTH = re.compile('Skill Level value \d+')


    def __init__(self,parent=None):
        super(Uci_worker,self).__init__(parent)
        self.command_queue = queue.Queue()
        self.process = QProcess()
        self.go_infinite = False
        self.current_fen = ""
        self.engine_info = EngineInfo()

    def process_command(self):
        if(self.process.state() == QProcess.NotRunning and not self.command_queue.empty()):
            msg = self.command_queue.get()
            if(msg.startswith("start_engine?")):
                path = msg.split("?")[1]
                self.process.start(path+"\n")
                self.engine_info.strength = None
        elif(self.process.state() == QProcess.Running):
            output = str(self.process.readAllStandardOutput(),"utf-8")
            if(not output == ""):
                self.engine_info.update_from_string(output,self.current_fen)
                self.emit(SIGNAL("info(PyQt_PyObject)"),deepcopy(self.engine_info))
                lines = output.splitlines()
                for line in lines:
                    bm = self.BESTMOVE.search(line)
                    if(bm):
                        move = bm.group()[9:]
                        self.emit(SIGNAL("bestmove(QString)"),move)
            if(not self.command_queue.empty()):
                # first check if we are in go infinite mode
                # then first send a stop command to engine
                # before processing further commands
                if(self.go_infinite):
                    self.process.write(bytes(("stop\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                msg = self.command_queue.get()
                self.go_infinite = False
                # if command is position fen moves, first count the
                # numbers of moves so far to generate move numbers in engine info
                if(msg.startswith("position")):
                    match = self.MOVES.findall(msg)
                    if(len(match) > 0):
                        self.engine_info.no_game_halfmoves = len(match)
                if(msg.startswith("quit")):
                    self.process.write(bytes(("quit\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                    self.process.waitForFinished()
                elif(msg.startswith("go infinite")):
                    self.go_infinite = True
                    self.process.write(bytes(("go infinite\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                elif(msg.startswith("setoption name Skill Level")):
                    m = self.STRENGTH.search(msg)
                    if(m):
                        self.engine_info.strength = 1200+int(m.group()[18:])*100
                    self.process.write(bytes(msg+"\n","utf-8"))
                    self.process.waitForBytesWritten()
                else:
                    self.process.write(bytes(msg+"\n","utf-8"))
                    self.process.waitForBytesWritten()


    def add_command(self,command):
        self.command_queue.put(command)

    def update_fen(self,fen_string):
        self.current_fen = fen_string