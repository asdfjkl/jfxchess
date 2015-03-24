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
        #current.root().export(exporter,current)

        # do formatting of plain text with regexp here by
        # making all variations grey
        # [ ] must be then blocked for comments, otherwise
        # highlighting doesn't work
        # for highlighting current move, look up current move in offset table
        # then insert highlighting at offsets
        game = exporter.__str__()
        game1 = re.sub("\[",'<dd><em><span style="color:gray">[',game)
        game2 = re.sub("]",'] </dd></em></span>',game1)
        return game2

        #print("GAME: "+game)
        #test = QtGui.QTextEdit()
        #test.setHtml(game2)
        #print("HTML: "+test.toPlainText())

        #with open("html.txt", "w") as text_file:
        #    text_file.write(test.toPlainText())

        #with open("plain.txt", "w") as text_file:
        #    text_file.write(game)

        #print("are equal:"+str(test.toPlainText() == game2))
        #return game1
        #self.write_token('<dd><em><span style="color:gray">[ ')

        #return self.export_string(current.root())
        #return (exporter.__str__())