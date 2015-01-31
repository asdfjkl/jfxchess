from gui.GUIPrinter import GUIPrinter
from gui.PieceImages import PieceImages
from gui.MovesEdit import MovesEdit
from dialogs.DialogEditGameData import DialogEditGameData
from dialogs.DialogPromotion import DialogPromotion
from dialogs.DialogEnterPosition import DialogEnterPosition
from dialogs.DialogAbout import DialogAbout
from uci.uci_controller import Uci_controller

# python chess
from chess.polyglot import *
from chess.pgn import Game
import chess

# PyQt and python system functions
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
import io
import sys, random, time

from logic.gamestate import MODE_ENTER_MOVES,MODE_GAME_ANALYSIS,MODE_PLAY_BLACK,MODE_ANALYSIS,MODE_PLAY_WHITE,MODE_PLAYOUT_POS

def idx_to_str(x):
    return chr(97 + x % 8)

class Point():
    def __init__(self,x,y):
        self.x = x
        self.y = y

    def to_str(self):
        return idx_to_str(self.x) + str(self.y+1)

    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    def __ne__(self, other):
        return not self.__eq__(other)



class ChessboardView(QWidget):

    def __init__(self,gamestate,engine):
        super(ChessboardView, self).__init__()
        #super(QWidget, self).__init__()
        policy = QSizePolicy(QSizePolicy.Preferred, QSizePolicy.Preferred)
        self.setSizePolicy(policy)

        self.gs = gamestate
        self.engine = engine

        self.setup_headers(self.gs.game)

        self.pieceImages = PieceImages()

        self.borderWidth = 12

        self.moveSrc = None
        self.grabbedPiece = None
        self.grabbedX = None
        self.grabbedY = None
        self.drawGrabbedPiece = False

        self.flippedBoard = False

        self.initUI()

    def initUI(self):
        self.show()



    def setup_headers(self,game):
        game.headers["Event"] = ""
        game.headers["Site"] = "MyTown"
        game.headers["Date"] = time.strftime("%Y.%m.%d")
        game.headers["Black"] = "Jerry (Level "+str(1200 + 100*self.gs.strength_level)+")"
        game.headers["White"] = "N.N."
        game.headers["Round"] = ""
        game.headers["Result"] = "*"


    def heightForWidth(self, width):
        return width

    def resizeEvent(self, e):
        self.setMinimumWidth(self.height())

    def paintEvent(self, event):

        qp = QPainter()
        qp.begin(self)
        self.drawBoard(event, qp)
        qp.end()

    def getBoardPosition(self,x,y):
        (boardSize,squareSize) = self.calculateBoardSize()
        # check if x,y are actually on the board
        if(x > self.borderWidth and y > self.borderWidth
           and x < (boardSize - self.borderWidth)
           and y < (boardSize - self.borderWidth)):
            x = x - self.borderWidth
            y = y - self.borderWidth
            x = x // squareSize
            y = 7 - (y // squareSize)
            if(self.flippedBoard):
                return Point(7-x,7-y)
            else:
                return Point(x,y)
        return None

    @pyqtSlot()
    def flip_board(self):
        if(self.flippedBoard):
            self.flippedBoard = False
        else:
            self.flippedBoard = True
        self.update()


    def touchPiece(self, x, y, mouse_x, mouse_y):
        self.moveSrc = Point(x,y)
        piece = self.gs.current.board().piece_at(y*8+x).symbol()
        self.grabbedPiece = piece
        self.grabbedX = mouse_x
        self.grabbedY = mouse_y
        self.drawGrabbedPiece = True


        #self.gt.current.board.set_at(x,y,'e')

    def _make_uci(self,point_src,point_dst,promote_to=None):
        uci = point_src.to_str() + point_dst.to_str()
        if(promote_to != None):
            uci += promote_to
        return uci

    def executeMove(self, uci):
        print(uci)
        temp = self.gs.current
        move = chess.Move.from_uci(uci)
        # check if move already exists
        variation_moves = [ x.move for x in self.gs.current.variations ]
        if(move in variation_moves):
            for node in self.gs.current.variations:
                if(node.move == move):
                    self.gs.current = node
        # otherwise create a new node
        else:
            self.gs.current.add_variation(move)
            self.gs.current = self.gs.current.variation(move)
        #"+str(( self.gs.board().turn + 1)%2))
        #new_node = chess.pgn.GameNode()
        #new_node.parent = temp
        #new_node.move = chess.Move.from_uci(uci)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
        #self.movesEdit = MovesEdit(self)
        #self.movesEdit.update_san()
        print(self.gs.current.root())
        print("castling rights: "+str(self.gs.current.board().castling_rights))
        # check if game is drawn, or checkmate due to various conditions
        if(self.gs.current.board().is_checkmate()): # due to checkmate
            msgBox = QMessageBox()
            msgBox.setText("Checkmate.")
            msgBox.setInformativeText("The game is over!")
            msgBox.exec_()
            self.emit(SIGNAL("checkmate"))
        elif(self.gs.current.board().is_stalemate()): # due to stalemate
            msgBox = QMessageBox()
            msgBox.setText("Stalemate.")
            msgBox.setInformativeText("The game is drawn!")
            msgBox.exec_()
            self.emit(SIGNAL("drawn"))
        elif(self.gs.current.board().is_insufficient_material()): # due to insufficient material
            msgBox = QMessageBox()
            msgBox.setText("Insufficient material to win.")
            msgBox.setInformativeText("The game is drawn!")
            msgBox.exec_()
            self.emit(SIGNAL("drawn"))
        elif(self.gs.current.board().can_claim_threefold_repitition()): # due to threefold repetition
            msgBox = QMessageBox()
            msgBox.setText("Threefold repetition.")
            msgBox.setInformativeText("The game is drawn!")
            msgBox.exec_()
            self.emit(SIGNAL("drawn"))

        if(self.gs.mode == MODE_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_infinite()
        #self.update()
        if((self.gs.mode == MODE_PLAY_WHITE and self.gs.current.board().turn == chess.BLACK) or
            (self.gs.mode == MODE_PLAY_BLACK and self.gs.current.board().turn == chess.WHITE)):
            book_move = None
            with open_reader("./books/varied.bin") as reader:
                entries = reader.get_entries_for_position(self.gs.current.board())
                moves = []
                for entry in entries:
                    move = entry.move().uci()
                    print("GOT MOVE FROM OPENING: "+move)
                    moves.append(move)
                    #self.emit(SIGNAL("bestmove(QString)"),move)
                l = len(moves)
                if(l > 0):
                    book_move = True
                    n = random.randint(0,l-1)
                    book_move = moves[n]
                    print("SELECTEDMOVE: "+move)
            if(book_move != None):
                print("EMITTING BOOK MOVE"+book_move)
                self.emit(SIGNAL("bestmove(QString)"),book_move)
            else:
                uci_string = self.gs.printer.to_uci(self.gs.current)
                self.engine.uci_send_position(uci_string)
                self.engine.uci_go_movetime(self.gs.computer_think_time)
        elif(self.gs.mode == MODE_PLAY_WHITE or self.gs.mode == MODE_PLAY_BLACK): pass
            #self.engine.uci_go_infinite()
        elif(self.gs.mode == MODE_PLAYOUT_POS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)
        print("Now its this turn: "+str(self.gs.current.board().turn))
        self.emit(SIGNAL("statechanged()"))


    def resetMove(self):
        #self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False

    def __players_turn(self):
        if(self.gs.mode == MODE_PLAYOUT_POS):
            return True
        if(self.gs.mode == MODE_PLAY_BLACK and self.gs.current.board().turn == chess.WHITE):
            return False
        if(self.gs.mode == MODE_PLAY_WHITE and self.gs.current.board().turn == chess.BLACK):
            return False
        return True

    def _is_valid_and_promotes(self,uci):
        if(not self.__players_turn()):
            return False
        legal_moves = self.gs.current.board().legal_moves
        for lm in legal_moves:
            if(uci == lm.uci()[0:4] and len(lm.uci())==5):
                return True
        return False

    def _is_valid(self,uci):
        if(not self.__players_turn()):
            return False
        legal_moves = self.gs.current.board().legal_moves
        return (len([x for x in legal_moves if x.uci() == uci]) > 0)

    def mousePressEvent(self, mouseEvent):
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())
        if(pos):
            i = pos.x
            j = pos.y
            if(self.grabbedPiece):
                #m = Point(i,j)
                uci = self._make_uci(self.moveSrc,pos)
                if(self._is_valid_and_promotes(uci)):
                    promDialog = DialogPromotion(self.gs.current.board().turn == chess.WHITE)
                    answer = promDialog.exec_()
                    if(answer):
                        uci += promDialog.final_piece.lower()
                        self.executeMove(uci)
                elif(self._is_valid(uci)):
                    self.executeMove(uci)
                else:
                    self.resetMove()
                    if(self.gs.current.board().piece_at(j*8+i) != None):
                        self.touchPiece(i,j,mouseEvent.x(),mouseEvent.y())
            else:
                if(self.gs.current.board().piece_at(j*8+i) != None):
                    self.touchPiece(i,j,mouseEvent.x(),mouseEvent.y())
                    print("set picked up at: "+str(i) + str(j))
        self.update()

    def mouseMoveEvent(self, mouseEvent):
        button = mouseEvent.button()
        if(button == 0 and (not self.grabbedPiece == None)):
            self.grabbedX = mouseEvent.x()
            self.grabbedY = mouseEvent.y()
            self.drawGrabbedPiece = True
            self.update()

    def mouseReleaseEvent(self, mouseEvent):
        self.drawGrabbedPiece = False
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())
        if(pos and self.grabbedPiece != None):
            if(pos != self.moveSrc):
                uci = self._make_uci(self.moveSrc, pos)
                if(self._is_valid_and_promotes(uci)):
                    promDialog = DialogPromotion(self.gs.current.board().turn == chess.WHITE)
                    answer = promDialog.exec_()
                    if(answer):
                        uci += promDialog.final_piece.lower()
                        self.executeMove(uci)
                elif(self._is_valid(uci)):
                    self.executeMove(uci)
                else:
                    self.resetMove()
        self.update()



    def calculateBoardSize(self):
        size = self.size()
        boardSize = min(size.width(), size.height())
        squareSize = (boardSize-(2*self.borderWidth))//8
        boardSize = 8 * squareSize + 2 * self.borderWidth
        return (boardSize,squareSize)

    def on_statechanged(self):
        self.update()

    def drawBoard(self, event, qp):
        penZero = QPen(Qt.black, 1, Qt.NoPen)
        qp.setPen(penZero)

        darkBlue = QColor(56,66,91)
        #Fritz 13
        #lightBlue = QtGui.QColor(111,132,181)
        #darkWhite = QtGui.QColor(239,239,239)
        #Fritz 6
        lightBlue = QColor(90,106,173)
        lightBlue2 = QColor(166,188,231)
        darkWhite = QColor(239,239,239)

        qp.setBrush(darkBlue)

        (boardSize,squareSize) = self.calculateBoardSize()

        qp.drawRect(1,1,boardSize,boardSize)

        boardOffsetX = self.borderWidth;
        boardOffsetY = self.borderWidth;

        # if the board is flipped, also
        # flip colors so that the board
        # loop works without any change/additional
        # if-checks
        dark = lightBlue2
        light = lightBlue
        if(self.flippedBoard):
            dark = lightBlue
            light = lightBlue2

        # draw Board
        for i in range(0,8):
            for j in range(0,8):
                if((j%2 == 0 and i%2 ==1) or (j%2 == 1 and i%2 ==0)):
                    qp.setBrush(dark)
                else:
                    qp.setBrush(light)
                #draw Square
                if(self.flippedBoard):
                    x = boardOffsetX+((7-i)*squareSize)
                else:
                    x = boardOffsetX+(i*squareSize)
                # drawing coordinates are from top left
                # whereas chess coords are from bottom left
                y = boardOffsetY+((7-j)*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #draw Piece
                piece = None
                if(self.flippedBoard):
                    piece = self.gs.current.board().piece_at((7-j)*8+i)
                else:
                    piece = self.gs.current.board().piece_at(j*8+i)
                if(piece != None and piece.symbol() in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    # skip piece that is currently picked up
                    if(not self.flippedBoard):
                        if(not (self.drawGrabbedPiece and i == self.moveSrc.x and j == self.moveSrc.y)):
                            qp.drawImage(x,y,self.pieceImages.getWp(piece.symbol(), squareSize))
                    else:
                        if(not (self.drawGrabbedPiece and i == self.moveSrc.x and (7-j) == self.moveSrc.y)):
                            qp.drawImage(x,y,self.pieceImages.getWp(piece.symbol(), squareSize))

        if(self.drawGrabbedPiece):
            offset = squareSize // 2
            qp.drawImage(self.grabbedX-offset,self.grabbedY-offset,self.pieceImages.getWp(self.grabbedPiece, squareSize))


        qp.setPen(darkWhite)
        qp.setFont(QFont('Decorative',8))

        for i in range(0,8):
            if(self.flippedBoard):
                idx = str(chr(65+(7-i)))
                qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                            boardOffsetY+(8*squareSize)+(self.borderWidth-3),idx)
                qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(i+1))
            else:
                idx = str(chr(65+i))
                qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                            boardOffsetY+(8*squareSize)+(self.borderWidth-3),idx)
                qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(8-i))

