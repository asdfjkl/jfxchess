from PyQt4 import QtGui
from chess.pgn import *

class GUIPrinter():
    def __init__(self):
        self.current = None
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.uci = ""

    def to_uci(self,current):
        self.current = current
        rev_moves = []
        node = current
        while(node.parent):
            rev_moves.append(node.move.uci())
            node = node.parent
        moves = " ".join(reversed(rev_moves))
        fen = node.board().fen()
        fen_current = self.current.board().fen()
        print("current fen:"+fen_current)
        uci = "position fen "+fen+" moves "+moves
        return fen_current, uci


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