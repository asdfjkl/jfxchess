from PyQt4 import QtGui
from chess.pgn import *

class GamePrinter(): 
    def __init__(self,current):
        self.current = current
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()
        self.cache = {}

    def node_to_san(self,parent_board,node):
        #return "a"
        #return move.uci()
        return parent_board.san(node.move)
        
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

    def get_board(self,node):
        if(node in self.cache):
            return self.cache[node]
        else:
            board = node.board()
            self.cache.update({node:board})
            return board

    def print_move(self,node,child):
        board = self.get_board(node)
        if(self.current == child):
            self.san_html += '<span style="color:darkgoldenrod">'
            self.san_html += self.node_to_san(board,child)
            self.san_html += '</span>'
        else:
            self.san_html += self.node_to_san(board,child)

    def print_san(self,node,move_no,inner_variant = False):

        board = self.get_board(node)
        # after a move by black,
        # increase move counter
        if(board.turn == chess.BLACK):
            move_no += 1

        # add space before each move
        self.san_html += " "

        if(len(node.variations) > 0):

            # if the next move is by white, print
            if(board.turn == chess.WHITE):
                self.san_html += str(move_no) + "."

            # print move of main variation
            self.print_move(node,node.variation(0))

            # print all other variations
            for i in range(1,len(node.variations)):

                nodei = node.variation(i)
                boardi= self.get_board(node)

                # if we are in the main variation
                if(not inner_variant):
                    self.san_html += '<dd><em><span style="color:gray">['
                else:
                    self.san_html += " ("
                if(boardi.turn == chess.WHITE):
                    self.san_html += str(move_no)+"."
                else:
                    self.san_html += str(move_no-1)+"."
                    self.san_html += " ... "
                self.print_move(node,nodei)
                self.print_san(nodei,move_no, True)

                self.san_html = self.san_html[:-1]
                if(not inner_variant):
                    self.san_html += "]</dd></em></span>"
                else:
                    self.san_html += ") "

            # if variations exist, the move was by black, and
            # the main variation itself has variations, then
            # add some spacer
            if(len(node.variations) > 1 and (board.turn == chess.WHITE) and node.variation(0).variations != None):
                self.san_html += str(move_no) + ". ..."

            # continue
            self.print_san(node.variation(0),move_no, inner_variant)

    
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
        self.sans = []
        #self.rec_san_html(self.current.root(), 1, True)
        self.print_san(current.root(),1)
        return self.san_html