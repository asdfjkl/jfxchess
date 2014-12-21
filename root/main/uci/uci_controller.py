from PyQt4.QtCore import *
from uci import uci_engine as uci_engine
from time import sleep

class Uci_controller(QObject):

    def __init__(self,parent=None):
        super(Uci_controller,self).__init__(parent)

        self.engine = None

    def new_err_output(self,msg):
        print("engine error: "+msg)

    def bestmove(self,msg):
        self.emit(SIGNAL("bestmove(QString)"),msg)

    def newinfo(self,msg):
        self.emit(SIGNAL("updateinfo(QString)"),msg)

    def stop_engine(self):
        if(self.engine):
            self.engine.quit()
            self.engine.wait()

    def start_engine(self,path):
        self.engine = uci_engine.Uci_engine("/Users/user/workspace/Jerry/root/main/stockfish-5-64",self)
        self.connect(self.engine, SIGNAL("bestmove(QString)"),self.bestmove,Qt.QueuedConnection)
        self.connect(self.engine, SIGNAL("newinfo(QString)"),self.newinfo,Qt.QueuedConnection)
        self.connect(self.engine, SIGNAL("new_err_out(QString)"), self.new_err_output,Qt.QueuedConnection)
        self.engine.start()
        while(self.engine.started == False):
            sleep(1)

    def uci_newgame(self):
        self.engine.command_queue.put("ucinewgame")
        self.engine.ping_engine()

    def uci_send_position(self,uci_string):
        self.engine.command_queue.put(uci_string)
        self.engine.ping_engine()

    def uci_ok(self):
        self.engine.command_queue.put("uci")
        self.engine.ping_engine()

    def uci_go_movetime(self,ms):
        self.engine.command_queue.put("go movetime "+str(ms))
        self.engine.pring_engine()

    def uci_go_infinite(self):
        self.engine.command_queue.put("go infinite")
        self.engine.ping_engine()

