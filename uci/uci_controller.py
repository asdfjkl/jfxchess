from PyQt4.QtCore import *
import re
from util.proc import set_lowpriority

class Uci_controller(QObject):

    BESTMOVE = re.compile('bestmove\s[a-z]\d[a-z]\d[a-z]{0,1}')

    def __init__(self,parent=None):
        super(Uci_controller,self).__init__(parent)
        self.engine = QProcess()


    def new_err_output(self):
        pass
        #print("engine error: "+msg)

    def bestmove(self,msg):
        self.emit(SIGNAL("bestmove(QString)"),msg)

    def new_std_output(self):
        output = str(self.engine.readAllStandardOutput(),"utf-8")
        #self.emit(SIGNAL("updateinfo(QString)"),output)
        lines = output.splitlines()
        #print("uci in: "+output)
        #l = self.queue_to_list(self.command_queue)
        #print("command queue: "+(" ".join(l)))
        # process output
        for line in lines:
            bm = self.BESTMOVE.search(line)
            if(bm):
                move = bm.group()[9:]
                self.emit(SIGNAL("bestmove(QString)"),move)
            else:
                self.emit(SIGNAL("updateinfo(QString)"),output)



    def add_move_numbers_to_info(self):
        replaced_all = False
        move_no = (self.info.no_game_halfmoves//2)+1
        white_moves = True
        s = ""
        if(self.info.no_game_halfmoves % 2 == 1):
            s += str(move_no)+". ...?"+self.info.pv[:]
            move_no += 1
        else:
            white_moves = False
            s += str(move_no) +"."+self.info.pv[:]
        while(replaced_all == False):
            mv = self.MOVE.search(s)
            if(mv):
                idx = mv.start()
                if(white_moves):
                    s = s[:(idx)] + " "+str(move_no) + "."+ s[(idx+1):]
                else:
                    move_no += 1
                    s = s[:(idx)] + "?"+\
                                           s[(idx+1):]
                white_moves = not white_moves
            else:
                replaced_all = True
        #print(s)
        s = s.replace('?',' ')
        return s

    def stop_engine(self):
        if(self.engine):
            print("call close")
            self.engine.terminate()
            print("close end")
            #self.engine.wait()

    def start_engine(self,path):
        self.engine.start(path)
        self.engine.waitForStarted()
        set_lowpriority(self.engine.pid())
        self.connect(self.engine, SIGNAL("readyReadStandardOutput()"),self.new_std_output)
        self.connect(self.engine, SIGNAL("readyReadStandardError()"),self.new_err_output)

    def uci_newgame(self):
        self.engine.write(bytes("ucinewgame\n","utf-8"))

    def uci_send_position(self,uci_string):
        self.engine.write(bytes(uci_string+"\n","utf-8"))

    def uci_ok(self):
        self.engine.write(bytes("uci"+"\n","utf-8"))

    def uci_go_movetime(self,ms):
        self.engine.write(bytes("go movetime "+str(ms)+"\n","utf-8"))

    # works only with stockfish
    def uci_strength(self,level):
        self.engine.write(bytes("setoption name Skill Level value "+str(level)+"\n","utf-8"))

    def uci_go_infinite(self):
        self.engine.write(bytes(("stop\n"),"utf-8"))
        self.engine.write(bytes(("go infinite\n"),"utf-8"))

