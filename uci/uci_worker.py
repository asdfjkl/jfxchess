from PyQt4.QtCore import *
import queue
from uci.engine_info import EngineInfo
import gc
import re
from copy import deepcopy

processes = set([])

class Uci_worker(QObject):

    MOVES = re.compile('\s[a-z]\d[a-z]\d([a-z]{0,1})')

    def __init__(self,parent=None):
        super(Uci_worker,self).__init__(parent)
        self.command_queue = queue.Queue()
        print("I was started")
        #self.process = None
        self.process = QProcess()
        self.go_infinite = False
        self.current_fen = ""
        self.engine_info = EngineInfo()

    def process_command(self):
        #output = str(self.process.readAllStandardOutput(),"utf-8")
        #print("current fen"+self.current_fen)
        #gc.disable()
        if(self.process.state() == QProcess.NotRunning and not self.command_queue.empty()):
            msg = self.command_queue.get()
            if(msg.startswith("start_engine?")):
                print("RESTARTING")
                #self.process.start("/Users/user/workspace/jerry/engine/stockfish_osx")
                path = msg.split("?")[1]
                print("path: "+path)
                self.process.start(path+"\n")

                #self.process.write(bytes(("go infinite\n"),"utf-8"))
        elif(self.process.state() == QProcess.Running):
            output = str(self.process.readAllStandardOutput(),"utf-8")
            self.engine_info.update_from_string(output,self.current_fen)
            #print(output)
            self.emit(SIGNAL("info(PyQt_PyObject)"),deepcopy(self.engine_info))
            #if(self.process.state() == QProcess.NotRunning):
            #    print("RESTARTING")
            #    self.process.start("/Users/user/workspace/jerry/engine/stockfish_osx")
            if(not self.command_queue.empty()):
                # first check if we are in go infinite mode
                # then first send a stop command to engine
                # before processing further commands
                if(self.go_infinite):
                    self.process.write(bytes(("stop\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                # process commands sent to engine
                msg = self.command_queue.get()
                self.go_infinite = False
                # if command is position fen moves, first count the
                # numbers of moves so far to generate move numbers in engine info
                if(msg.startswith("position")):
                    match = self.MOVES.findall(msg)
                    #print("MOVE CALCULATIN"+str(moves))
                    if(len(match) > 0):
                        self.engine_info.no_game_halfmoves = len(match)
                if(msg.startswith("quit")):
                    #pass
                    self.process.write(bytes(("quit\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                    self.process.waitForFinished()
                elif(msg.startswith("go infinite")):
                    print("sending go infinite")
                    self.go_infinite = True
                    self.process.write(bytes(("go infinite\n"),"utf-8"))
                    self.process.waitForBytesWritten()
                else:
                    #if(msg.startswith("quit")):
                    print("sending to engine: "+msg)
                    self.process.write(bytes(msg+"\n","utf-8"))
                    self.process.waitForBytesWritten()
        #elif(not self.command_queue.empty()):
        #    pass
            #msg = self.command_queue.get()
            #if(msg.startswith("start_engine?")):
                    # start the engine
                    #print("starting engine")
                    #path = msg.split("?")[1]
                    #print("path: "+path)
                    #self.process.start(path)
                    #self.process.start("/Users/user/workspace/jerry/engine/stockfish_osx")
#if(self.process.state() == QProcess.NotRunning):
            #    print("RESTARTING")
            #    self.process.start("/Users/user/workspace/jerry/engine/stockfish_osx")

                    #res = self.process.waitForStarted(10000)
                    #if(res):
                    #    print("succ started")
                    #self.process.waitForFinished(100)


    def add_command(self,command):
        #print("adding to queue"+msg)
        self.command_queue.put(command)

    def update_fen(self,fen_string):
        print("GETTING NEW FEN"+fen_string)
        self.current_fen = fen_string