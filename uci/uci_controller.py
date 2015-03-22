from PyQt4.QtCore import *
import re
from util.proc import set_lowpriority
import time
from uci.uci_worker import Uci_worker

class Uci_controller(QObject):

    def __init__(self,parent=None):
        super(Uci_controller,self).__init__(parent)
        self.thread = QThread()
        self.uci_worker = Uci_worker()
        self.timer = QTimer()

        self.timer.timeout.connect(self.uci_worker.process_command)
        self.timer.start(50)

        self.connect(self.uci_worker,SIGNAL("bestmove(QString)"),self.on_bestmove)
        self.connect(self.uci_worker,SIGNAL("info(PyQt_PyObject)"),self.on_info)
        self.connect(self.uci_worker,SIGNAL("on_error(QString)"),self.on_error)

        self.connect(self,SIGNAL("new_command(QString)"),self.uci_worker.add_command)
        self.connect(self,SIGNAL("new_fen(QString)"),self.uci_worker.update_fen)
        #self.emit(SIGNAL("foobar"))

        self.foobar = self.uci_worker.process

        self.timer.moveToThread(self.thread)
        self.uci_worker.moveToThread(self.thread)
        #self.uci_worker.process.moveToThread(self.thread)

        #self.emit(SIGNAL("foobar"))

        self.thread.start()
        print("thread started")

    def on_error(self,msg):
        pass
        #print("engine error: "+msg)

    def send_fen(self,fen_string):
        self.emit(SIGNAL("new_fen(QString)"),fen_string)

    def on_bestmove(self,msg):
        self.emit(SIGNAL("bestmove(QString)"),msg)

    def on_info(self,engine_info):
        self.emit(SIGNAL("updateinfo(PyQt_PyObject)"),engine_info)

    def stop_engine(self):
        self.emit(SIGNAL("new_command(QString)"),"quit")

    def start_engine(self,path):
        #print("starting engine "+path)
        self.emit(SIGNAL("new_command(QString)"),"start_engine?"+path)

    def reset_engine(self,path):
        self.stop_engine()
        self.start_engine(path)

    def uci_newgame(self):
        self.emit(SIGNAL("new_command(QString)"),"ucinewgame")

    def uci_send_position(self,uci_string):
        print("sending"+uci_string)
        self.emit(SIGNAL("new_command(QString)"),uci_string)

    def uci_ok(self):
        self.emit(SIGNAL("new_command(QString)"),"uci")

    def uci_go_movetime(self,ms):
        self.emit(SIGNAL("new_command(QString)"),"go movetime "+str(ms))

    # works only with stockfish
    def uci_strength(self,level):
        self.emit(SIGNAL("new_command(QString)"),"setoption name Skill Level value "+str(level))

    def uci_go_infinite(self):
        self.emit(SIGNAL("new_command(QString)"),"go infinite")

