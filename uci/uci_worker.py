from PyQt4.QtCore import *
import queue

class Uci_worker(QObject):

    def __init__(self,parent=None):
        super(Uci_worker,self).__init__(parent)
        self.command_queue = queue.Queue()
        print("I was started")
        self.process = QProcess()
        self.go_infinite = False

    def process_command(self):
        # parse output, if there is output
        ready = self.process.waitForReadyRead(10)
        if(ready):
            output = str(self.process.readAllStandardOutput(),"utf-8")
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
            print("something on queue")
            msg = self.command_queue.get()
            print("processing message: "+msg)
            self.go_infinite = False
            if(msg.startswith("start_engine?")):
                # start the engine
                print("starting engine")
                path = msg.split("?")[1]
                self.process.start(path)
                self.process.waitForStarted()
            elif(msg.startswith("quit")):
                self.process.write(bytes(("quit\n"),"utf-8"))
                self.process.waitForBytesWritten()
                self.process.waitForFinished()
            elif(msg.startswith("go infinite")):
                self.go_infinite = True
                self.process.write(bytes(("go infinite\n"),"utf-8"))
                self.process.waitForBytesWritten()
            else:
                self.process.write(bytes(msg+"\n","utf-8"))
                self.process.waitForBytesWritten()

    def add_command(self,msg):
        print("adding to queue"+msg)
        self.command_queue.put(msg)
