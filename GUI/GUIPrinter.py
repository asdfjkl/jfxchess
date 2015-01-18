from PyQt4 import QtGui
from chess.pgn import *

class GUIPrinter():
    def __init__(self):
        self.current = None
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()
        self.cache = {}
        self.uci = ""

    def add_to_offset_table(self,node,san):
        self.qtextedit.setHtml(self.san_html)
        plain_san = self.qtextedit.toPlainText()
        offset_end = len(plain_san)
        self.qtextedit.setHtml(san)
        plain_move = self.qtextedit.toPlainText()
        offset_start = offset_end - len(plain_move) - 1
        self.offset_table.append((offset_start,offset_end,node))

    def to_san_html(self,current):
        self.current = current
        self.offset_table = []
        self.san_html = ""
        #self.print_san(current.root(),0,False)
        #return self.san_html
        exporter = StringExporter(columns=None)
        current.root().export_html(exporter,current,self.offset_table)
        return exporter.__str__()
        #return self.export_string(current.root())