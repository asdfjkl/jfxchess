from PyQt4 import QtGui
from chess.pgn import *

class GamePrinter(): 
    def __init__(self,current):
        self.current = current
        self.san_html = ""
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()

    def node_to_san(self,node):
        return node.parent.board().san(node.move)
        
    def variant_start(self, node, child, moveNo):    
        if(node.config.whiteToMove):
            self.san_plain = self.san_plain + str(moveNo)+"."
        else:
            self.san_plain = self.san_plain + str(moveNo-1)+"."
        if(not node.config.whiteToMove):
            self.san_plain = self.san_plain + " ... "
        self.san_plain = self.san_plain + child.move.to_san()
    
    def add_to_offset_table(self,node):
        self.qtextedit.setHtml(self.san_html)
        plain_san = self.qtextedit.toPlainText()
        offset_end = len(plain_san)
        self.qtextedit.setHtml(self.node_to_san(node))
        plain_move = self.qtextedit.toPlainText()
        offset_start = offset_end - len(plain_move)
        self.offset_table.append((offset_start,offset_end,node))
    
    def print_highlighted(self, node):
        san = self.node_to_san(node)
        string = ""
        if(self.current == node):
            return '<span style="color:darkgoldenrod">'+san+'</span>'
        else:
            return san
    
    def variant_start_highlighted(self, node, child, moveNo):    
        if(node.board().turn == chess.WHITE):
            self.san_html += str(moveNo)+"."
        else:
            self.san_html += str(moveNo-1)+"."
            self.san_html = self.san_html + " ... "
        self.san_html += self.print_highlighted(child)
    
    def rec_san_html(self, node, moveNo, mainVariant = True):
        if(node.board().turn == chess.BLACK):
            moveNo += 1
        self.san_html = self.san_html + " "
        len_var = len(node.variations)
        print("has " + str(len_var) + "variations")
        if(len_var > 0):
            if(node.board().turn == chess.WHITE):
                self.san_html = self.san_html + str(moveNo) + "."
            # print first move
            print("it: "+node.variation(0).move.uci())
            self.san_html += self.print_highlighted(node.variation(0))
            self.add_to_offset_table(node.variation(0))
            # print all alternatives
            for i in range(1,len_var):
                if(mainVariant):
                    self.san_html += '<dd><em><span style="color:gray">[ '
                    self.variant_start_highlighted(node, node.variation(i), moveNo)
                    self.add_to_offset_table(node.variation(i))
                    self.san_html += '</span><span style="color:gray">'
                    self.rec_san_html(node.variation(i),moveNo,False)
                    self.san_html += "]"
                    self.san_html += "</dd></em></span>"
                else:
                    self.san_html += " ("
                    self.variant_start_highlighted(node, node.variation(i), moveNo)
                    self.add_to_offset_table(node.variation(i))
                    self.rec_san_html(node.variation(i),moveNo,False)
                    self.san_html += ") "
            # continue
            if(len_var > 1 and (node.board().turn == chess.WHITE) and node.variation(0).variations != None):
                self.san_html += str(moveNo) + ". ..."
            self.rec_san_html(node.variation(0),moveNo, mainVariant)
        elif(not node.variations == []):
            self.san_html += self.node_to_san(node.variation(0))
            self.add_to_offset_table(node.variation(0))
            self.rec_san_html(node.variation(0),moveNo, mainVariant)

    
    def to_san_html(self,current):
        self.current = current
        self.san_html = ""
        self.rec_san_html(self.current.root(), 1, True)
        return self.san_html