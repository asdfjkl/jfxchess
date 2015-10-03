from PyQt4.QtCore import *
import re
from util.proc import set_lowpriority
import time
from uci.uci_worker import Uci_worker
import time

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

        # needed to keep reference?
        self.foobar = self.uci_worker.process

        self.timer.moveToThread(self.thread)
        self.uci_worker.moveToThread(self.thread)

        self.thread.start(QThread.LowestPriority)

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
        print("emitted stop signal")
        self.foobar.kill()
        self.foobar.waitForFinished()
        #time.sleep(1)
        #print("slept for 1")
        #self.thread.killTimer(1)


    def start_engine(self,path):
        self.emit(SIGNAL("new_command(QString)"),"start_engine?"+path)

#    def reset_engine(self,path):
#        self.stop_engine()
#        self.start_engine(path)

    def uci_newgame(self):
        self.emit(SIGNAL("new_command(QString)"),"ucinewgame")

    def uci_send_command(self, command):
        self.emit(SIGNAL("new_command(QString)"),command)

    def uci_send_position(self,uci_string):
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

    def send_engine_options(self, options):
        for (option,val) in options:
            if(option.type == 'spin'):
                self.uci_send_command("setoption name "+option.name+" value "+str(val))
            elif(option.type == 'check'):
                self.uci_send_command("setoption name "+option.name+" value "+str(val).lower())
            else:
                self.uci_send_command("setoption name "+option.name+" value "+val)

    def reset_engine(self, engine):
        self.stop_engine()
        self.start_engine(engine.path)
        self.uci_ok()
        self.send_engine_options(engine.options)
        self.uci_newgame()

