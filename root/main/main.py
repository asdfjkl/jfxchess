import sys
from PyQt4 import QtGui

#!/usr/bin/python

# boxlayout.py
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from PyQt4.QtSvg import *

#import GameState
from GameState import *
#!/usr/bin/python

# menubar.py 

import sys, random, time
from PyQt4 import QtGui, QtCore, QtSvg


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
        self.gt = GameTree()
        self.pieceImages = PieceImages()
        
        self.movesEdit = None
        
        hf = Point(4,1)
        print(hf.to_str())
        
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
    
    def touchPiece(self, x, y):
        self.moveSrc = Point(x,y)
        piece = self.gt.current.board.get_at(x,y)
        self.grabbedPiece = piece
        self.gt.current.board.set_at(x,y,'e')        
        
    def executeMove(self, x, y):
        # put the grabbed piece to where it was before executing
        self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
        m = Move(self.moveSrc,Point(x,y),self.grabbedPiece)
        self.gt.execute_move(m)
        self.moveSrc = None 
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
        text = self.gt.to_san()
        print("moves:"+text)
        self.movesEdit.setHtml(text)
        self.movesEdit.update()
        
    def resetMove(self):
        self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
                
    def mousePressEvent(self, mouseEvent):
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())
        if(pos):
            i = pos.x
            j = pos.y
            if(self.grabbedPiece):
                #m = Point(i,j)
                if(self.gt.is_valid_move(Move(self.moveSrc, pos, self.grabbedPiece))):
                    self.executeMove(i, j)
                else:
                    self.resetMove()
                    if(self.gt.current.board.get_at(i,j) != 'e'):
                        self.touchPiece(i,j)
                        self.grabbedX = mouseEvent.x()
                        self.grabbedY = mouseEvent.y()
                        self.drawGrabbedPiece = True
            else:
                if(self.gt.current.board.get_at(i,j) != 'e'):
                    self.touchPiece(i,j)
                    self.grabbedX = mouseEvent.x()
                    self.grabbedY = mouseEvent.y()
                    self.drawGrabbedPiece = True
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
        if(pos): 
            i = pos.x
            j = pos.y
            if(self.grabbedPiece != None):
                if(pos != self.moveSrc):
                    m = Move(self.moveSrc, pos, self.grabbedPiece)
                    if(self.gt.is_valid_move(m)):
                        self.executeMove(i, j)
                    else:
                        self.resetMove()
                else:
                    self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
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
                x = boardOffsetX+(i*squareSize)
                y = boardOffsetY+(j*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #draw Piece
                piece = None
                if(self.flippedBoard):
                    piece = self.gt.current.board.get_at(7-i,j)
                else:
                    piece = self.gt.current.board.get_at(i,7-j)
                if(piece in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    qp.drawImage(x,y,self.pieceImages.getWp(piece, squareSize))

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
        self.old_cursor_pos = 0
        self.setCursorWidth(0)
        self.viewport().setCursor(Qt.ArrowCursor)
        self.cursorPositionChanged.connect(self.gotopos)
    
    #def mousePressEvent(self, mouseEvent):
    #    if mouseEvent.button() == Qt.LeftButton:
    #        moves_string = self.toHtml()
    #        x = self.textCursor().position()
    #        y = self.cursorRect().y()
    #        print("left pressed, pos"+str(x)+" and "+str(y))
    
    def print_stuff(self,offset_table):
        string = ""
        for i in range(0,len(offset_table)):
            string = string + "(" + str(offset_table[i][0]) + "," + str(offset_table[i][1]) +","+ offset_table[i][2].to_console()+ ") "
        return string
    
    def gotopos(self):
        offset = self.textCursor().position()
        if(offset != self.old_cursor_pos):
            self.old_cursor_pos = offset
            self.bv.gt.offset_table=[]
            text = self.bv.gt.get_san_plain()
            ot = self.bv.gt.offset_table
            print("coming fro to_san_plain "+text)
            print("is in text widget "+self.toPlainText())
            #print("offset_table "+str(len(ot)))
            print("offset: "+str(offset))
            j = 0
            #print(self.print_stuff(ot))
            for i in range(0,len(ot)):
                if(offset>= ot[i][0] and offset<= ot[i][1]):
                    j = i
            print("taking index "+str(j))
            self.bv.gt.current = ot[j][2]
            self.bv.update()
            self.old_cursor_pos = 0

            self.setHtml(self.bv.gt.to_san())
            
        
    def keyPressEvent(self, event):
        key = event.key()
        if key == QtCore.Qt.Key_Left: 
            print("left pressed")
            self.bv.gt.prev()
            self.setHtml(self.bv.gt.to_san())
            self.bv.update()
        elif key == QtCore.Qt.Key_Right:
            print("message ok")
            if(self.bv.gt.exist_variants()):
                dialog = DialogWithListView(self.bv.gt.move_list())
                dialog.setWindowTitle("Next Move")
                dialog.listWidget.setFocus()
                answer = dialog.exec_()
                if answer == True:
                    print("message ok")
                    idx = dialog.selected_idx
                    print("selected idx via dialog"+str(idx))
                    self.bv.gt.next(idx)
            else:
                self.bv.gt.next()
            self.setHtml(self.bv.gt.to_san())
            self.bv.update()

class MainWindow(QtGui.QMainWindow):
    def __init__(self):
        QtGui.QMainWindow.__init__(self)

        test = State()
        #test.test()
        #test.toFen()

        self.resize(640, 480)
        self.setWindowTitle('Jerry - Chess')
                
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

        menu = self.menuBar().addMenu('File')
        action1 = menu.addAction('Change File Path')
        
        menu = self.menuBar().addMenu('Edit ')
        flipAction = QtGui.QAction('Flip Board', self)
        flipAction.triggered.connect(board.flip_board)
        menu.addAction(flipAction)
        # self.connect(action2, QtCore.SIGNAL('triggered()'), QtCore.SLOT(board.flip_board()))

        

app = QtGui.QApplication(sys.argv)
main = MainWindow()
app.setActiveWindow(main)
main.show()
sys.exit(app.exec_())