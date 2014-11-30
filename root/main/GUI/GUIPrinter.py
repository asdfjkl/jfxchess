from PyQt4 import QtGui
from chess.pgn import *

class GUIPrinter():
    def __init__(self,current):
        self.current = current
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()
        self.cache = {}

    def node_to_san(self,parent_board,node):
        return parent_board.san(node.move)

    def add_to_offset_table(self,node,san):
        self.qtextedit.setHtml(self.san_html)
        plain_san = self.qtextedit.toPlainText()
        offset_end = len(plain_san)
        self.qtextedit.setHtml(san)
        plain_move = self.qtextedit.toPlainText()
        offset_start = offset_end - len(plain_move) - 1
        self.offset_table.append((offset_start,offset_end,node))

    def get_board(self,node):
        if(node in self.cache):
            return self.cache[node]
        else:
            board = node.board()
            self.cache.update({node:board})
            return board

    def pgn_code_to_txt(self,nag):
        if(nag == 1):
            return "!"
        elif(nag==2):
            return "?"
        elif(nag==3):
            return "!!"
        elif(nag==4):
            return "??"
        elif(nag==5):
            return "!?"
        elif(nag==6):
            return "?!"
        elif(nag==13):
            return "∞"
        elif(nag==14):
            return "+/="
        elif(nag==15):
            return "=/+"
        elif(nag==16):
            return "+/−"
        elif(nag==17):
            return "-/+"
        elif(nag==18):
            return "+-"
        elif(nag==19):
            return "-+"
        elif(nag==44):
            return "=/∞"
        elif(nag==45):
            return "∞/="

    def print_move(self,node,child):
        move_san = ""
        board = self.get_board(node)
        if(self.current == child):
            move_san += '<span style="color:darkgoldenrod">'
            move_san += self.node_to_san(board,child)
            for nag in sorted(list(child.nags)):
                move_san += self.pgn_code_to_txt(nag)
            move_san += '</span>'
        else:
            move_san += self.node_to_san(board,child)
            for nag in sorted(list(child.nags)):
                move_san += self.pgn_code_to_txt(nag)
        if(child.comment != ""):
            move_san += '<span style="color:darkblue">'
            move_san += " "+child.comment+" "
            move_san += '</span>'
        return move_san

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

            # if we are at a setup position where
            # starts moving, also print the move no plus ...
            if(node.parent == None and board.turn == chess.BLACK):
                self.san_html += str(move_no-1) + ". ... "

            # print move of main variation
            move_san = self.print_move(node,node.variation(0))
            self.san_html += move_san
            self.add_to_offset_table(node.variation(0),move_san)

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
                move_san = self.print_move(node,nodei)
                self.san_html += move_san
                self.add_to_offset_table(nodei,move_san)
                self.print_san(nodei,move_no, True)

                self.san_html = self.san_html[:-1]
                if(not inner_variant):
                    self.san_html += "]</dd></em></span>"
                else:
                    self.san_html += ")"

            # if variations exist, the move was by black, and
            # the main variation itself has variations, then
            # add some spacer
            if(len(node.variations) > 1 and (board.turn == chess.WHITE) and node.variation(0).variations != None):
                self.san_html += str(move_no) + ". ..."

            # continue
            self.print_san(node.variation(0),move_no, inner_variant)
    
    def to_san_html(self,current):
        self.current = current
        self.san_html = ""
        self.print_san(current.root(),1)
        return self.san_html