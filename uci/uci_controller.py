from PyQt4.QtCore import *
from uci import uci_engine as uci_engine
from time import sleep
from uci.engine_info import EngineInfo

class Uci_controller(QObject):

    def __init__(self,parent=None):
        super(Uci_controller,self).__init__(parent)
        self.engine = QProcess()


    def new_err_output(self):
        pass
        #print("engine error: "+msg)

    def bestmove(self,msg):
        self.emit(SIGNAL("bestmove(QString)"),msg)

    def newinfo(self):
        output = str(self.engine.readAllStandardOutput(),"utf-8")
        self.emit(SIGNAL("updateinfo(PyQt_PyObject)"),output)

    def stop_engine(self):
        if(self.engine):
            self.engine.kill()
            #self.engine.wait()

    def start_engine(self,path):
        self.engine.start(path)
        self.connect(self.engine, SIGNAL("readyReadStandardOutput()"),self.newinfo)
        self.connect(self.engine, SIGNAL("readyReadStandardError()"),self.new_err_output)

    def uci_newgame(self):
        self.engine.write(bytes("ucinewgame\n","utf-8"))

    def uci_send_position(self,uci_string):
        self.engine.write(bytes(uci_string+"\n","utf-8"))

    def uci_ok(self):
        self.engine.write(bytes("uci"+"\n","utf-8"))

    def uci_go_movetime(self,ms):
        self.engine.write(bytes("go movetime "+str(ms)+"\n"))

    # works only with stockfish
    def uci_strength(self,level):
        self.engine.command_queue.put("setoption name Skill Level value "+str(level))
        self.engine.ping_engine()

    def uci_go_infinite(self):
        self.engine.write(bytes(("stop\n"),"utf-8"))
        self.engine.write(bytes(("go infinite\n"),"utf-8"))

