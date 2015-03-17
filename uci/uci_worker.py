from PyQt4.QtCore import *
import queue

class Uci_worker(QObject):

    def __init__(self,parent=None):
        super(Uci_worker,self).__init__(parent)
        self.command_queue = queue.Queue()
        print("I was started")

    def process_command(self,msg):
        if(self.command_queue.empty()):
            print("queue empty")
            pass
        else:
            # process commands sent to engine
            print("something started")
            pass

    def add_command(self):
        print("adding to queue")
        self.command_queue.put("foo")
