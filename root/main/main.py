import sys
from PyQt4 import QtGui

#!/usr/bin/python

# boxlayout.py
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from PyQt4.QtSvg import *

#import GameState
from GameState import *
from GameTree import *
#!/usr/bin/python

from chess.pgn import *
from chess.polyglot import *
import io
# menubar.py 

import sys, random, time
from PyQt4 import QtGui, QtCore, QtSvg

from PGNParser import *
from GameState import Point


class PieceImages:
    def __init__(self):
        self.pieces = {'P': {} , 'R':{},'N':{},'B':{},'Q':{},'K':{},
                       'p':{},'r':{},'n':{},'b':{},'q':{},'k':{}}
        self.rens = {}
        wb = ["w","b"]
        pcs = ["p","r","b","n","q","k"]
        for fst in wb:
            for snd in pcs:
                ren = QSvgRenderer()
                QSvgRenderer.load(ren,"../res/pieces/"+fst+snd+".svg")
                if fst == "w":
                    self.rens[snd.upper()] = ren
                else:
                    self.rens[snd] = ren
        print("piece images")
                 
    def getWp(self, piece, size):
        imgs = self.pieces[piece]
        if size in imgs:
            return imgs[size]
        else:
            img = QImage(int(size), int(size), QImage.Format_ARGB32) 
            img.fill(QColor(1,1,1,1))
            painter = QPainter()
            painter.begin(img)
            self.rens[piece].render(painter)
            painter.end()
            imgs[size] = img
            return img
        
class DialogWithPlainText(QDialog):
    def __init__(self, parent=None):
        super(DialogWithPlainText, self).__init__(parent) 
        
        self.plainTextEdit = QtGui.QPlainTextEdit()
        self.saved_text = ""
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        layout = QGridLayout()
        layout.addWidget(self.plainTextEdit,0,1)
        layout.addWidget(buttonBox, 3, 0, 1, 3)
        self.setLayout(layout)
        self.plainTextEdit.setPlainText("foo")
        self.saved_text = "foo"
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.plainTextEdit.textChanged.connect(self.update_text)
        self.resize(300, 150)
        
    def update_text(self):
        temp = self.plainTextEdit.toPlainText()
        self.saved_text = temp.replace('\n', ' ').replace('\r', '')

class DialogEditGameData(QDialog):

    def __init__(self, root, parent=None):
        super(DialogEditGameData,self).__init__(parent)
        self.setWindowTitle("Edit Game Data")

        self.ed_event = QLineEdit()
        self.ed_event.setText(root.headers["Event"])
        self.lbl_event = QLabel("Event")
        self.lbl_event.setBuddy(self.ed_event)

        self.ed_site = QLineEdit()
        self.ed_site.setText(root.headers["Site"])
        self.lbl_site = QLabel("Site")
        self.lbl_site.setBuddy(self.ed_site)

        self.ed_date = QLineEdit()
        self.ed_date.setText(root.headers["Date"])
        self.lbl_date = QLabel("Date")
        self.lbl_date.setBuddy(self.ed_date)

        self.ed_round = QLineEdit()
        self.ed_round.setText(root.headers["Round"])
        self.lbl_round = QLabel("Round")
        self.lbl_round.setBuddy(self.ed_round)

        self.ed_white = QLineEdit()
        self.ed_white.setText(root.headers["White"])
        self.lbl_white = QLabel("White")
        self.lbl_white.setBuddy(self.ed_white)

        self.ed_black = QLineEdit()
        self.ed_black.setText(root.headers["Black"])
        self.lbl_black = QLabel("Black")
        self.lbl_black.setBuddy(self.ed_black)

        #self.ed_eco = QLineEdit()
        #self.ed_eco.setText(root.headers["ECO"])
        #self.lbl_eco = QLabel("ECO")
        #self.lbl_eco.setBuddy(self.ed_eco)

        self.rb_ww = QRadioButton("1-0")
        self.rb_bw = QRadioButton("0-1")
        self.rb_draw = QRadioButton("½-½")
        self.rb_unclear = QRadioButton("*")
        self.lbl_result = QLabel("Result")

        if(root.headers["Result"]=="1-0"):
            self.rb_ww.setChecked(True)
        elif(root.headers["Result"]=="0-1"):
            self.rb_bw.setChecked(True)
        elif(root.headers["Result"]=="1/2-1/2"):
            self.rb_draw.setChecked(True)
        else:
            self.rb_unclear.setChecked(True)

        grpBox = QGroupBox("Result")
        hbox = QHBoxLayout()
        hbox.addWidget(self.rb_ww)
        hbox.addWidget(self.rb_draw)
        hbox.addWidget(self.rb_bw)
        hbox.addWidget(self.rb_unclear)
        hbox.addStretch(1)
        grpBox.setLayout(hbox)

        #self.plainTextEdit = QtGui.QPlainTextEdit()
        self.saved_text = ""
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        layout = QGridLayout()
        layout.addWidget(self.lbl_event,0,0)
        layout.addWidget(self.ed_event,0,1,1,2)

        layout.addWidget(self.lbl_site,1,0)
        layout.addWidget(self.ed_site,1,1,1,2)

        layout.addWidget(self.lbl_date,2,0)
        layout.addWidget(self.ed_date,2,1,1,1)

        layout.addWidget(self.lbl_round,3,0)
        layout.addWidget(self.ed_round,3,1)

        layout.addWidget(self.lbl_white,4,0)
        layout.addWidget(self.ed_white,4,1,1,2)

        layout.addWidget(self.lbl_black,5,0)
        layout.addWidget(self.ed_black,5,1,1,2)

        #layout.addWidget(self.lbl_eco,6,0)
        #layout.addWidget(self.ed_eco,6,1)

        layout.addWidget(grpBox,7,0,1,3)

        layout.addWidget(buttonBox, 8, 2, 1, 1)
        self.setLayout(layout)
        #self.ed_eco.setText("D42")
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        # self.ed_eco.textChanged.connect(self.update_text)
        self.resize(370, 150)

    def update_text(self):
        print("text changed")

class DialogPromotion(QDialog):
 
    def __init__(self, whitePromotes, parent=None):
        super(DialogPromotion, self).__init__(parent) 

        self.border = 10
        h = self.size().height() - (2*self.border)
        w = (self.size().width()-(2*self.border))/4
        # piece size
        self.ps = min(h,w)
        self.resize(300, 90)
        self.whitePromotes = whitePromotes
        self.final_piece = "Q"
        self.sel_idx = 0
        self.pieceImages = PieceImages()

    def paintEvent(self, event):
        qp = QtGui.QPainter()
        qp.begin(self)
        # draw images
        h = self.size().height() - (2*self.border)
        w = (self.size().width()-(2*self.border))/4
        self.ps = min(h,w)
        s = self.ps
        print("size is: "+str(s))
        
        lightBlue2 = QtGui.QColor(166,188,231)
        qp.setBrush(lightBlue2)
        for i in range(0,4):
            if self.sel_idx == i:
                qp.drawRect(self.border+i*s,self.border,s,s)
            qp.drawImage(self.border+i*s,self.border,self.pieceImages.getWp(self.piece_by_idx(i),s))
        
        #lightBlue = QtGui.QColor(90,106,173) 
        #lightBlue2 = QtGui.QColor(166,188,231)
        #darkWhite = QtGui.QColor(239,239,239)
        #boardOffsetX = self.borderWidth;
        #boardOffsetY = self.borderWidth;
        # qp.setBrush(darkBlue)
        # qp.drawImage(0,0,self.pieceImages.getWp("Q", 40))
        # (boardSize,squareSize) = self.calculateBoardSize()
        
        # qp.drawRect(1,1,boardSize,boardSize)
        
        qp.end()
    
    def piece_by_idx(self,idx):
        if(self.whitePromotes):
            if(idx == 0):
                return "Q"
            elif(idx == 1):
                return "R"
            elif(idx == 2):
                return "B"
            elif(idx == 3):
                return "N"
        if(not self.whitePromotes):
            if(idx == 0):
                return "q"
            elif(idx == 1):
                return "r"
            elif(idx == 2):
                return "b"
            elif(idx == 3):
                return "n"    
        
    def mousePressEvent(self, mouseEvent):
        print("mouse x:"+str(mouseEvent.x())+"y:"+str(mouseEvent.y()))
        sel_idx = mouseEvent.x() // self.ps
        print("sel idx: "+str(sel_idx))
        self.sel_idx = sel_idx
        self.update()
    
    def mouseReleaseEvent(self, mouseEvent):
        print("mouse release event")
        self.final_piece = self.piece_by_idx(self.sel_idx);
        self.done(True)
                
    def keyPressEvent(self, event):
        key = event.key()
        if key == QtCore.Qt.Key_Left:
            self.sel_idx = max(self.sel_idx-1,0)
            self.update()
        elif key == QtCore.Qt.Key_Right:
            self.sel_idx = min(self.sel_idx+1,3)
            self.update()
        elif key == QtCore.Qt.Key_Return :
            self.final_piece = self.piece_by_idx(self.sel_idx);
            self.done(True)

class DialogWithListView(QDialog):
 
    def __init__(self, moveList, parent=None):
        super(DialogWithListView, self).__init__(parent) 

        self.resize(20, 40)
        
        for mv in moveList :
            print(mv)

        self.selected_idx = 0
 
        self.listWidget = QListWidget()
        
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        
        self.okButton = QPushButton("&OK")
        cancelButton = QPushButton("Cancel")
        
        buttonLayout = QHBoxLayout() 
        buttonLayout.addStretch() 
        buttonLayout.addWidget(self.okButton) 
        buttonLayout.addWidget(cancelButton)
        layout = QGridLayout()
        layout.addWidget(self.listWidget,0,1)
        #layout.addLayout(buttonLayout, 2, 0, 1, 3)
        layout.addWidget(buttonBox, 3, 0, 1, 3)
        self.setLayout(layout)
        self.listWidget.addItems(moveList)
        self.listWidget.item(0).setSelected(True)

        self.connect(buttonBox, SIGNAL("accepted()"),
                 self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),
                 self, SLOT("reject()"))
        
        #self.connect(self.okButton, SIGNAL("clicked()"),
        #         self, SLOT("accept()"))

        self.connect(self,SIGNAL("rightclick()"), SLOT("accept()") )
        self.connect(self,SIGNAL("leftclick()"), SLOT("reject()") )
        #self.connect(self.listWidget, QtCore.SIGNAL("itemDoubleClicked(QListWidgetItem *)"), SLOT("accept()"))   
            
        #self.connect(self.listWidget, SIGNAL("itemDoubleClicked()"), SLOT("accept()"))
        self.listWidget.itemDoubleClicked.connect(self.accept)
        self.listWidget.currentItemChanged.connect(self.on_item_changed)
    
    def on_item_changed(self):
        self.selected_idx = self.listWidget.currentRow()    
        
    def keyPressEvent(self, event):
        key = event.key()
        print("CURRENT ROW:" + str(self.listWidget.currentRow()))
        if key == QtCore.Qt.Key_Left or key == QtCore.Qt.Key_Escape: 
            print("left key or esc pressed")
            self.emit(SIGNAL("leftclick()"))
        elif key == QtCore.Qt.Key_Right or key == QtCore.Qt.Key_Return :
            print("right key or return pressed")
            self.emit(SIGNAL("rightclick()"))
 

class ChessboardView(QtGui.QWidget):
    
    def __init__(self):
        #super(ChessboardView, self).__init__()
        super(QtGui.QWidget, self).__init__()
        policy = QtGui.QSizePolicy(QtGui.QSizePolicy.Preferred, QtGui.QSizePolicy.Preferred)
        self.setSizePolicy(policy)
        self.current = chess.pgn.Game()

        self.current.headers["Event"] = ""
        self.current.headers["Site"] = ""
        self.current.headers["Date"] = time.strftime("%Y.%m.%d")
        self.current.headers["Round"] = ""
        self.current.headers["White"] = "N.N."
        self.current.headers["Black"] = "Jerry (PC)"
        self.current.headers["Result"] = "*"

        self.pieceImages = PieceImages()
        
        self.movesEdit = None

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

    def append_to_pgn(self):
        filename = QtGui.QFileDialog.getSaveFileName(self, 'Append to PGN', '*.pgn',
                                                     None, QFileDialog.DontConfirmOverwrite)
        if(filename):
            print("append saver")

    def save_to_pgn(self):
        filename = QtGui.QFileDialog.getSaveFileName(self, 'Save PGN', 'PGN (*.pgn)', None, QFileDialog.DontUseNativeDialog)
        if(filename):
            f = open(filename,'w')
            print(self.current.root(), file=f, end="\n\n")
            print("pgn saver")

    def open_pgn(self):
        filename = QtGui.QFileDialog.getOpenFileName(self, 'Open PGN', None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
        if(filename):
            pgn = open(filename)
            first_game = chess.pgn.read_game(pgn)
            self.current = first_game
            self.update()
            self.movesEdit.bv = self

            self.movesEdit.update_san()
            self.movesEdit.setFocus()

            print("open pgn dummy")

    def editGameData(self):
        ed = DialogEditGameData(self.current.root())
        answer = ed.exec_()
        if(answer):
            root = self.current.root()
            root.headers["Event"] = ed.ed_white.text()
            root.headers["Site"] = ed.ed_site.text()
            root.headers["Date"] = ed.ed_date.text()
            root.headers["Round"] = ed.ed_round.text()
            root.headers["White"] = ed.ed_white.text()
            root.headers["Black"] = ed.ed_black.text()
            #root.headers["ECO"] = ed.ed_eco.text()
            if(ed.rb_ww.isChecked()):
                root.headers["Result"] = "1-0"
            elif(ed.rb_bw.isChecked()):
                root.headers["Result"] = "0-1"
            elif(ed.rb_draw.isChecked()):
                root.headers["Result"] = "1/2-1/2"
            elif(ed.rb_unclear.isChecked()):
                root.headers["Result"] = "*"

    def game_to_clipboard(self):
        clipboard = QtGui.QApplication.clipboard()
        exporter = chess.pgn.StringExporter()
        self.current.root().export(exporter, headers=True, variations=True, comments=True)
        pgn_string = str(exporter)
        clipboard.setText(pgn_string)

    def pos_to_clipboard(self):
        clipboard = QtGui.QApplication.clipboard()
        clipboard.setText(self.gt.current.to_fen())

    def game_from_clipboard(self):
        clipboard = QtGui.QApplication.clipboard()
        pgn = io.StringIO(clipboard.text())
        first_game = chess.pgn.read_game(pgn)
        self.current = first_game
        self.update()
        self.movesEdit.bv = self

        self.movesEdit.update_san()
        self.movesEdit.setFocus()

    def heightForWidth(self, width):
        return width    

    def resizeEvent(self, e):
        self.setMinimumWidth(self.height())

    def paintEvent(self, event):

        qp = QtGui.QPainter()
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
        print("flipping board")
        if(self.flippedBoard):
            self.flippedBoard = False
        else:
            self.flippedBoard = True
        self.update()
    
    def touchPiece(self, x, y, mouse_x, mouse_y):
        self.moveSrc = Point(x,y)
        piece = self.current.board().piece_at(y*8+x).symbol()
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
        temp = self.current
        move = chess.Move.from_uci(uci)
        self.current.add_variation(move)
        #new_node = chess.pgn.GameNode()
        #new_node.parent = temp
        #new_node.move = chess.Move.from_uci(uci)
        self.current = self.current.variation(move)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
        #self.movesEdit = MovesEdit(self)
        self.movesEdit.update_san()
        print(self.current.root())
        
    def resetMove(self):
        #self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False

    def _is_valid_and_promotes(self,uci):
        legal_moves = self.current.board().legal_moves
        for lm in legal_moves:
            if(uci == lm.uci()[0:4] and len(lm.uci())==5):
                return True
        return False

    def _is_valid(self,uci):
        legal_moves = self.current.board().legal_moves
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
                    promDialog = DialogPromotion(self.current.board().turn == chess.WHITE)
                    answer = promDialog.exec_()
                    if(answer):
                        uci += promDialog.final_piece.lower()
                        self.executeMove(uci)
                elif(self._is_valid(uci)):
                    self.executeMove(uci)
                else:
                    self.resetMove()
                    if(self.current.board().piece_at(j*8+i) != None):
                        self.touchPiece(i,j,mouseEvent.x(),mouseEvent.y())
            else:
                if(self.current.board().piece_at(j*8+i) != None):
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
                    promDialog = DialogPromotion(self.current.board().turn == chess.WHITE)
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
            
    def drawBoard(self, event, qp):
        penZero = QtGui.QPen(QtCore.Qt.black, 1, QtCore.Qt.NoPen)
        qp.setPen(penZero)

        darkBlue = QtGui.QColor(56,66,91)
        #Fritz 13
        #lightBlue = QtGui.QColor(111,132,181)
        #darkWhite = QtGui.QColor(239,239,239)
        #Fritz 6
        lightBlue = QtGui.QColor(90,106,173) 
        lightBlue2 = QtGui.QColor(166,188,231)
        darkWhite = QtGui.QColor(239,239,239)
        
        qp.setBrush(darkBlue)
        
        (boardSize,squareSize) = self.calculateBoardSize()
        
        qp.drawRect(1,1,boardSize,boardSize)
        
        boardOffsetX = self.borderWidth;
        boardOffsetY = self.borderWidth;
        
        # draw Board     
        
        for i in range(0,8):
            for j in range(0,8):
                if((j%2 == 0 and i%2 ==1) or (j%2 == 1 and i%2 ==0)):
                    qp.setBrush(lightBlue)
                else:
                    qp.setBrush(lightBlue2)        
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
                    piece = self.current.board().piece_at((7-j)*8+i)
                else:
                    piece = self.current.board().piece_at(j*8+i)
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
        qp.setFont(QtGui.QFont('Decorative',8))
        
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

class MovesEdit(QtGui.QTextEdit):
    
    def __init__(self,chessboardView):
        super(QtGui.QTextEdit, self).__init__()
        self.bv = chessboardView
        #self.printer = self.bv.printer
        self.printer = GamePrinter(self.bv.current)
        self.old_cursor_pos = 0
        self.setCursorWidth(2)
        self.viewport().setCursor(Qt.ArrowCursor)
        self.setContextMenuPolicy(Qt.CustomContextMenu)
        #self.cursorPositionChanged.connect(self.go_to_pos)
        self.customContextMenuRequested.connect(self.context_menu)
    
    def context_menu(self):
        menu = QMenu(self)
        sub_move_annotation = QtGui.QMenu(menu)
        sub_move_annotation.setTitle("Move Annotation")
        ann_blunder = sub_move_annotation.addAction("?? Blunder")
        ann_blunder.triggered.connect(lambda: self.move_annotation("??"))
        ann_mistake = sub_move_annotation.addAction("? Mistake")
        ann_mistake.triggered.connect(lambda: self.move_annotation("?"))
        ann_dubious = sub_move_annotation.addAction("?! Dubious Move")
        ann_dubious.triggered.connect(lambda: self.move_annotation("?!"))
        ann_interesting = sub_move_annotation.addAction("!? Interesting Move")
        ann_interesting.triggered.connect(lambda: self.move_annotation("!?"))
        ann_good = sub_move_annotation.addAction("! Good Move")
        ann_good.triggered.connect(lambda: self.move_annotation("!"))
        ann_brilliant = sub_move_annotation.addAction("!! Brilliant Move")
        ann_brilliant.triggered.connect(lambda: self.move_annotation("!!"))
        ann_empty = sub_move_annotation.addAction("No Annotation")
        ann_empty.triggered.connect(lambda: self.move_annotation(""))
        
        sub_pos_annotation = QtGui.QMenu(menu)
        sub_pos_annotation.setTitle("Position Annotation")
        pos_unclear = sub_pos_annotation.addAction("∞ Unclear")
        pos_unclear.triggered.connect(lambda: self.pos_annotation("∞"))
        pos_comp_w = sub_pos_annotation.addAction("=/∞ With Compensation for White")
        pos_comp_w.triggered.connect(lambda: self.pos_annotation("=/∞"))
        pos_comp_b =sub_pos_annotation.addAction("∞/= With Compensation for Black")
        pos_comp_b.triggered.connect(lambda: self.pos_annotation("∞/="))
        pos_wsb = sub_pos_annotation.addAction("+/= White Slightly Better")
        pos_wsb.triggered.connect(lambda: self.pos_annotation("+/="))
        pos_bsb = sub_pos_annotation.addAction("=/+ Black Slightly Better")
        pos_bsb.triggered.connect(lambda: self.pos_annotation("=/+"))
        pos_wb = sub_pos_annotation.addAction("+/- White Better")
        pos_wb.triggered.connect(lambda: self.pos_annotation("+/-"))
        pos_bb = sub_pos_annotation.addAction("-/+ Black Better")
        pos_bb.triggered.connect(lambda: self.pos_annotation("-/+"))
        pos_wmb = sub_pos_annotation.addAction("+- White Much Better")
        pos_wmb.triggered.connect(lambda: self.pos_annotation("+-"))
        pos_bmb = sub_pos_annotation.addAction("-+ Black Much Better")
        pos_bmb.triggered.connect(lambda: self.pos_annotation("-+"))
        pos_none = sub_pos_annotation.addAction("No Annotation")
        pos_none.triggered.connect(lambda: self.pos_annotation(""))

                
        add_comment = menu.addAction("Add/Edit Comment")
        add_comment.triggered.connect(self.add_comment)
        menu.addMenu(sub_move_annotation)
        menu.addMenu(sub_pos_annotation)
        menu.addSeparator()
        variant_up = menu.addAction("Move Variant Up")
        variant_up.triggered.connect(self.variant_up)
        variant_down = menu.addAction("Move Variant Down")
        variant_down.triggered.connect(self.variant_down)
        delete_variant = menu.addAction("Delete Variant")
        delete_variant.triggered.connect(self.delete_variant)
        delete_here = menu.addAction("Delete From Here")
        delete_here.triggered.connect(self.delete_from_here)
        menu.addSeparator()
        delete_all_comments = menu.addAction("Delete All Comments")
        delete_all_comments.triggered.connect(self.delete_all_comments)
        delete_all_variants = menu.addAction("Delete All Variants")
        delete_all_variants.triggered.connect(self.delete_all_variants)
        menu.exec_(QCursor.pos())
        
    def mousePressEvent(self, mouseEvent):
        cursor = self.cursorForPosition(mouseEvent.pos())
        cursor_pos = cursor.position()
        self.go_to_pos(cursor_pos)
        self.old_cursor_pos = cursor_pos
        
    def move_annotation(self,string):
        offset = self.old_cursor_pos
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            selected_state.move.move_annotation = string
        self.setHtml(self.printer.to_san_html())
        
    def pos_annotation(self, string):
        offset = self.old_cursor_pos
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            selected_state.move.pos_annotation = string
        self.setHtml(self.printer.to_san_html())
                
    def delete_all_comments(self):
        self.bv.gt.delete_all_comments()
        self.bv.update()
        self.setHtml(self.printer.to_san_html())
        
    def add_comment(self):
        offset = self.old_cursor_pos
        print("cursor_offset "+str(offset))
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            dialog = DialogWithPlainText()
            dialog.setWindowTitle("Add/Edit Comment")
            dialog.plainTextEdit.setPlainText(selected_state.move.comment)
            answer = dialog.exec_()
            if answer == True:
                print("message ok")
                typed_text = dialog.saved_text
                selected_state.move.comment = typed_text
                self.setHtml(self.printer.to_san_html())

    def variant_up(self):
        offset = self.old_cursor_pos
        print("cursor_offset "+str(offset))
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            self.bv.gt.variant_up(selected_state)
            self.bv.update()
            self.setHtml(self.printer.to_san_html())
        
    def delete_from_here(self):
        offset = self.old_cursor_pos
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            self.bv.gt.delete_from_here(selected_state)
            self.bv.update()
            self.setHtml(self.printer.to_san_html())

    def delete_variant(self):
        offset = self.old_cursor_pos
        print("cursor_offset "+str(offset))
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            self.bv.gt.delete_variant(selected_state)
            self.bv.update()
            self.setHtml(self.printer.to_san_html())

    def variant_down(self):
        offset = self.old_cursor_pos
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            self.bv.gt.variant_down(selected_state)
            self.bv.update()
            self.setHtml(self.printer.to_san_html())
    
    def delete_all_variants(self):
        offset = self.old_cursor_pos
        selected_state = self.bv.gt.get_state_from_offset(offset,self.printer)
        if(selected_state != None):
            self.bv.gt.delete_all_variants(selected_state)
            self.bv.update()
            self.setHtml(self.printer.to_san_html())

    def _get_state_from_offset(self, offset):
        # next is to update to current status
        text = self.printer.to_san_html(self.bv.current)
        # print(str(self.printer.offset_table))
        offset_index = self.printer.offset_table
        j = 0
        for i in range(0,len(offset_index)):
            if(offset>= offset_index[i][0] and offset<= offset_index[i][1]):
                j = i
        try:
            return offset_index[j][2]
        except IndexError:
            return None

    def go_to_pos(self,cursor_pos):
        #offset = self.textCursor().position()
        print("triggered with "+str(cursor_pos))
        if(cursor_pos > 0):
        #if(offset != self.old_cursor_pos):
            #self.old_cursor_pos = offset
            selected_state = self._get_state_from_offset(cursor_pos)
            self.bv.current = selected_state
            self.bv.update()
            #self.old_cursor_pos = 0
            self.setHtml(self.printer.to_san_html(self.bv.current))

    def update_san(self):
        self.setHtml(self.printer.to_san_html(self.bv.current))

    def keyPressEvent(self, event):
        key = event.key()
        if key == QtCore.Qt.Key_Left: 
            print("left pressed")
            if(self.bv.current.parent):
                self.bv.current = self.bv.current.parent
            self.setHtml(self.printer.to_san_html(self.bv.current))
            #print("received from printer "+self.printer.to_pgn())
            self.bv.update()
        elif key == QtCore.Qt.Key_Right:
            print("message ok")
            variations = self.bv.current.variations
            if(len(variations) > 1):
                move_list = [ self.bv.current.board().san(x.move)
                              for x in self.bv.current.variations ]
                dialog = DialogWithListView(move_list)
                dialog.setWindowTitle("Next Move")
                dialog.listWidget.setFocus()
                answer = dialog.exec_()
                if answer == True:
                    print("message ok")
                    idx = dialog.selected_idx
                    print("selected idx via dialog"+str(idx))
                    self.bv.current = self.bv.current.variation(idx)
            elif(len(variations) == 1):
                self.bv.current = self.bv.current.variation(0)
            #self.setHtml(self.printer.to_san_html())
            self.bv.update()

class MainWindow(QtGui.QMainWindow):
    def __init__(self):
        QtGui.QMainWindow.__init__(self)

        test = State()
        test.test()
        #test.toFen()

        self.resize(640, 480)
        self.setWindowTitle('Jerry - Chess')
        self.centerOnScreen()

        #qp.drawImage(10,10,qim,0,0,0,0)

        exit = QtGui.QAction(QtGui.QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip('Exit application')
        self.connect(exit, QtCore.SIGNAL('triggered()'), QtCore.SLOT('close()'))

        board = ChessboardView()
        #board.getState().setInitPos()
        
        spLeft = QtGui.QSizePolicy();
        spLeft.setHorizontalStretch(1);

        ok = QtGui.QPushButton("OK")
        cancel = QtGui.QPushButton("Cancel")

        mainWidget = QtGui.QWidget()

        hbox = QtGui.QHBoxLayout()
        hbox.addWidget(board)
        
        spRight = QtGui.QSizePolicy();
        spRight.setHorizontalStretch(2);
 
        lcd1 = QtGui.QLCDNumber(self)
        
        lcd1.setSegmentStyle(QtGui.QLCDNumber.Flat)
        lcd1.display(time.strftime("%H"+":"+"%M"))
        lcd1.setFrameStyle(QtGui.QFrame.NoFrame)
        
        lcd2 = QtGui.QLCDNumber(self)
        lcd2.setSegmentStyle(QtGui.QLCDNumber.Flat)
        lcd2.display(time.strftime("%H"+":"+"%M"))

        lcd2.setFrameStyle(QtGui.QFrame.NoFrame)

        
        hboxLcd = QtGui.QHBoxLayout()
        
        
        pixmapWhite = QtGui.QPixmap("../res/icons/whiteClock.png")
        pixmapBlack = QtGui.QPixmap("../res/icons/blackClock.png")
        
        labelWhite = QtGui.QLabel()
        labelWhite.setPixmap(pixmapWhite)
        labelWhite.setAlignment(QtCore.Qt.AlignRight)
        
        labelBlack = QtGui.QLabel()
        labelBlack.setPixmap(pixmapBlack)
        labelBlack.setAlignment(QtCore.Qt.AlignRight)
        
        spacerLcd = QtGui.QSpacerItem(20,10)
        
        hboxLcd.addWidget(labelWhite)
        hboxLcd.addWidget(lcd1)
        hboxLcd.addItem(spacerLcd)
        hboxLcd.addWidget(labelBlack)
        hboxLcd.addWidget(lcd2)
        
        
        vbox = QtGui.QVBoxLayout();
        vbox.addLayout(hboxLcd)
        
        movesEdit = MovesEdit(board)
        vbox.addWidget(movesEdit)
        board.movesEdit = movesEdit

        engineOutput = QtGui.QPlainTextEdit()
        vbox.addWidget(engineOutput)
        
        hbox.addLayout(vbox)
        
        
        mainWidget.setLayout(hbox)
        self.setCentralWidget(mainWidget);

        statusbar = self.statusBar()
        statusbar.showMessage('Ready')

        self.menubar = self.menuBar()

        m_file = self.menuBar().addMenu('File ')
        new_game_white = m_file.addAction('New Game (White)')
        new_game_black = m_file.addAction("New Game (Black)")
        m_file.addSeparator()
        load_game = m_file.addAction("Load PGN")
        load_game.triggered.connect(board.open_pgn)
        save_game = m_file.addAction("Save PGN")
        save_game.triggered.connect(board.save_to_pgn)
        append_game = m_file.addAction("Append to PGN")
        append_game.triggered.connect(board.append_to_pgn)
        m_file.addSeparator()
        print_game = m_file.addAction("Print Game")
        print_pos = m_file.addAction("Print Position")
        m_file.addSeparator()
        exit_item = m_file.addAction("Quit")
        exit_item.triggered.connect(QApplication.quit)
        m_edit = self.menuBar().addMenu('Edit ')
        copy_game = m_edit.addAction("Copy Game")
        copy_game.triggered.connect(board.game_to_clipboard)
        copy_pos = m_edit.addAction("Copy Position")
        copy_pos.triggered.connect(board.pos_to_clipboard)
        paste = m_edit.addAction("Paste")
        paste.triggered.connect(board.game_from_clipboard)
        m_edit.addSeparator()
        enter_pos = m_edit.addAction("Enter Position")
        m_edit.addSeparator()
        edit_game_data = m_edit.addAction("Edit Game Data")
        edit_game_data.triggered.connect(board.editGameData)
        flip = m_edit.addAction("Flip Board")
        flip.triggered.connect(board.flip_board)
        m_edit.addSeparator()
        offer_draw = m_edit.addAction("Offer Draw")
        give_up = m_edit.addAction("Give Up")
        m_edit.addSeparator()
        stop_clock = m_edit.addAction("Stop Clock")
        setup_clock = m_edit.addAction("Setup Clock")
        m_mode = self.menuBar().addMenu("Mode")
        analysis = m_mode.addAction("Analysis Mode")
        play_white = m_mode.addAction("Play as White")
        play_black = m_mode.addAction("Play as Black")
        enter_moves = m_mode.addAction("Enter Moves")
        m_mode.addSeparator()
        set_strength = m_mode.addAction("Strength Level")
        m_mode.addSeparator()
        analyze_game = m_mode.addAction("Full Game Analysis")
        play_out_pos = m_mode.addAction("Play out Position")
        m_help = self.menuBar().addMenu("Help")
        about = m_help.addAction("About")
        about.triggered.connect(board.flip_board)  
        m_help.addSeparator()    
        # self.connect(action2, QtCore.SIGNAL('triggered()'), QtCore.SLOT(board.flip_board()))
        
        
    def centerOnScreen (self):
        '''centerOnScreen()
           Centers (vertically in the upper third) the window on the screen.'''
        resolution = QtGui.QDesktopWidget().screenGeometry()
        self.move((resolution.width() / 2) - (self.frameSize().width() / 2),
                  (resolution.height() / 2) - (self.frameSize().height()*2 / 3))



        

app = QtGui.QApplication(sys.argv)
main = MainWindow()
app.setActiveWindow(main)
main.show()
sys.exit(app.exec_())