from PyQt4.QtCore import *
import queue
import gc

class Uci_worker(QObject):

    def __init__(self,parent=None):
        super(Uci_worker,self).__init__(parent)
        self.command_queue = queue.Queue()
        print("I was started")
        self.process = None
        #self.process.start("/Users/user/workspace/jerry/engine/stockfish_osx")
        self.go_infinite = False

    def process_command(self):
        gc.disable()

        #print("my own pid: "+str(QThread.currentThreadId()))
        # parse output, if there is output
        if(self.process == None):
            self.process = QProcess()
        print("all error"+str(self.process.readAllStandardError()))
        print("quering:"+str(self.process.pid()))
        print("error:"+str(self.process.error()))
        #ready = self.process.waitForReadyRead(100)
        print("quering1:"+str(self.process.pid()))
        print("error1:"+str(self.process.error()))
        #print("checking if there is new output...")
        if(True):
            output = str(self.process.readAllStandardOutput(),"utf-8")
            print("i has new output:"+output)

            self.emit(SIGNAL("info(QString)"),output)
        if(self.command_queue.empty()):
            pass
        else:
            # first check if we are in go infinite mode
            # then first send a stop command to engine
            # before processing further commands
            if(self.go_infinite):
                self.process.write(bytes(("stop\n"),"utf-8"))
                self.process.waitForBytesWritten()
            # process commands sent to engine
            #print("something on queue")
            msg = self.command_queue.get()
            #print("processing message: "+msg)
            self.go_infinite = False
            if(msg.startswith("start_engine?")):
                # start the engine
                print("starting engine")
                path = msg.split("?")[1]
                self.process.start(path)
                res = self.process.waitForStarted(10000)
                if(res):
                    print("succ started")
                self.process.waitForFinished(100)
            elif(msg.startswith("quit")):
                pass
                #self.process.write(bytes(("quit\n"),"utf-8"))
                #self.process.waitForBytesWritten()
                #self.process.waitForFinished()
            elif(msg.startswith("go infinite")):
                self.go_infinite = True
                self.process.write(bytes(("go infinite\n"),"utf-8"))
                self.process.waitForBytesWritten(1000)
            else:
                if(msg.startswith("quit")):
                    print("quitting engine: "+msg)
                self.process.write(bytes(msg+"\n","utf-8"))
                self.process.waitForBytesWritten()

    def add_command(self,msg):
        #print("adding to queue"+msg)
        self.command_queue.put(msg)
