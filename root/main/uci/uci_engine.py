from PyQt4.QtCore import *
import re
import queue

class EngineInfo(object):
    def __init__(self):
        self.id = None
        self.score = None
        self.depth = None
        self.mate = None
        self.currmovenumber = None
        self.currmove = None
        self.nps = None
        self.pv = None

    def __str__(self):
        outstr = ""
        if(self.id):
            outstr = self.id+"\n"
        if(self.mate):
            outstr += "#"+str(self.mate)+"     "
        elif(self.score):
            outstr += str(self.score)+"      "
        if(self.currmovenumber):
            outstr += str(self.currmovenumber)
        if(self.currmove):
            outstr += str(self.currmove)
        outstr += "     "
        if(self.nps):
            outstr += str(self.nps)
        outstr += "\n"
        if(self.pv):
            outstr += self.pv
        return outstr


class Uci_engine(QThread):

    READYOK = re.compile('readyok')
    SCORECP = re.compile('score\scp\s-{0,1}(\d)+')
    NPS = re.compile('nps\s(\d)+')
    DEPTH = re.compile('depth\s(\d)+')
    MATE = re.compile('score\smate\s-{0,1}(\d)+')
    CURRMOVENUMBER = re.compile('currmovenumber\s(\d)+')
    CURRMOVE = re.compile('currmove\s[a-z]\d[a-z]\d[a-z]{0,1}')
    BESTMOVE = re.compile('bestmove\s[a-z]\d[a-z]\d[a-z]{0,1}')
    PV = re.compile('pv(\s[a-z]\d[a-z]\d[a-z]{0,1})+')
    IDNAME = re.compile('id\sname\s(\w|\s)+')

    def __init__(self, engine_path, parent=None):
        super(Uci_engine, self).__init__(parent)
        self.mutex_readyok = QMutex()
        self.remaining_readyok = 0
        self.sent_go_infinite = False
        self.command_queue = queue.Queue()
        self.engine_path = ".../stockfish-5-64"
        self.started = False
        self.info = EngineInfo()

    def ping_engine(self):
        self.mutex_readyok.lock()
        print("ping engine")
        self.process.write(bytes("isready\n","utf-8"))
        self.process.waitForBytesWritten()
        self.remaining_readyok += 1
        self.mutex_readyok.unlock()

    def send_to_stdin(self):
        # get top of commands to execute from queue
        msg = self.command_queue.get(False)
        print("executing in send_to_stdin "+msg)
        # if the engine is in infinite mode,
        # first send a stop command
        if(self.sent_go_infinite):
            self.process.write(bytes("stop\n","utf-8"))
            self.process.waitForBytesWritten()
            self.sent_go_infinite = False
        # send the command
        # if the command contains "go infinite"
        # remember that for the next time, so that
        # stop can be sent before issuing the next
        # command
        if(msg == "go infinite"):
            self.sent_go_infinite = True
          # finally issue the command
        self.process.write(bytes(msg+"\n","utf-8"))
        self.process.waitForBytesWritten()
        self.process.write(bytes("isready\n","utf-8"))
        self.process.waitForBytesWritten()

    def queue_to_list(self,q):
        """ Dump a Queue to a list """

        # A new list
        l = []

        while q.qsize() > 0:
            l.append(q.get())
        return l

    def on_std_out(self):
        output = str(self.process.readAllStandardOutput(),"utf-8")
        lines = output.splitlines()
        print("receiv out signal: "+output)
        #l = self.queue_to_list(self.command_queue)
        #print("command queue: "+(" ".join(l)))
        # process output
        for line in lines:
            if(self.READYOK.search(line)):
                print("got readyok")
                self.mutex_readyok.lock()
                self.remaining_readyok -= 1
                if(self.remaining_readyok > 0):
                    print("wont do anything, since rem readyok")
                if(self.command_queue.empty()):
                    print("wont't do anything, since cc empty")
                if(not self.command_queue.empty()):
                    print("executing next command of queue")
                    self.send_to_stdin()
                self.mutex_readyok.unlock()
            else:
                emit_info = False
                cp = self.SCORECP.search(line)
                if(cp):
                    score = float(cp.group()[9:])/100.0
                    self.info.score = score
                    emit_info = True
                nps = self.NPS.search(line)
                if(nps):
                    knps = float(nps.group()[4:])/1000.0
                    self.info.nps = knps
                    emit_info = True
                depth = self.DEPTH.search(line)
                if(depth):
                    d = int(depth.group()[6:])
                    self.info.depth = d
                    self.info.mate = None
                    emit_info = True
                mate = self.MATE.search(line)
                if(mate):
                    m = int(mate.group()[6:])
                    self.info.mate = m
                    emit_info = True
                cmn = self.CURRMOVENUMBER.search(line)
                if(cmn):
                    n = int(cmn.group()[15])
                    self.info.currmovenumber = n
                    emit_info = True
                cm = self.CURRMOVE.search(line)
                if(cm):
                    move = cm.group()[9:]
                    self.info.currmove = move
                    emit_info = True
                pv = self.PV.search(line)
                if(pv):
                    moves = pv.group()[3:]
                    self.info.pv = moves
                    emit_info = True
                id = self.IDNAME.search(line)
                if(id):
                    engine_name = id.group()[8:]
                    self.info.id = engine_name
                    emit_info = True
                if(emit_info):
                    self.emit(SIGNAL("newinfo(QString)"),str(self.info))

                bm = self.BESTMOVE.search(line)
                if(bm):
                    move = bm.group()[9:]
                    self.emit(SIGNAL("bestmove(QString)"),move)


    def on_err_out(self):
        output = str(self.process.readLineStderr(),"utf-8")
        print("error: "+output)
        self.emit(SIGNAL("new_err_out(QString"),output)

    def run(self):
        self.process = QProcess()
        self.process.start("./stockfish-5-64")
        self.process.waitForStarted()
        print("started")
        self.started = True
        self.process.connect(self.process, SIGNAL("readyReadStandardOutput()"),self.on_std_out)
        self.process.connect(self.process, SIGNAL("readyReadStandardError()"),self.on_err_out)
        self.exec_()
        self.process.close()
        self.process.waitForFinished(-1)
        self.exit()

