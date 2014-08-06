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

class ChessboardView(QtGui.QWidget):
    
    def __init__(self):
        super(ChessboardView, self).__init__()
        policy = QtGui.QSizePolicy(QtGui.QSizePolicy.Preferred, QtGui.QSizePolicy.Preferred)
        self.setSizePolicy(policy)
        self.gt = GameTree()
        self.pieceImages = PieceImages()
        
        hf = Point(4,1)
        print(hf.to_str())
        
        self.borderWidth = 12
        
        self.moveSrc = None
        self.grabbedPiece = None
        self.grabbedX = None
        self.grabbedY = None
        self.drawGrabbedPiece = False
        
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
            return Point(x,y)
        return None
    
    def touchPiece(self, x, y):
        self.moveSrc = Point(x,y)
        piece = self.gt.current.board().get_at(x,y)
        self.grabbedPiece = piece
        self.gt.current.board().set_at(x,y,'e')        
        
    def executeMove(self, x, y):
        m = Move(self.moveSrc,Point(x,y),self.grabbedPiece)
        self.gt.execute_move(m)
        self.moveSrc = None 
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
        
    def resetMove(self):
        self.gt.current.board().set_at(self.moveSrc.x(),self.moveSrc.y(),self.grabbedPiece)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False    
            
    def mousePressEvent(self, mouseEvent):
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())
        if(pos):
            i = pos.x()
            j = pos.y()
            if(self.grabbedPiece):
                #m = Point(i,j)
                if(self.gt.is_valid_move(Move(self.moveSrc, pos, self.grabbedPiece))):
                    self.executeMove(i, j)
                else:
                    self.resetMove()
                    if(self.gt.current.board().get_at(i,j) != 'e'):
                        self.touchPiece(i,j)
                        self.grabbedX = mouseEvent.x()
                        self.grabbedY = mouseEvent.y()
                        self.drawGrabbedPiece = True
            else:
                if(self.gt.current.board().get_at(i,j) != 'e'):
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
            i = pos.x()
            j = pos.y()
            if(self.grabbedPiece != None):
                if(pos != self.moveSrc):
                    m = Move(self.moveSrc, pos, self.grabbedPiece)
                    if(self.gt.is_valid_move(m)):
                        self.executeMove(i, j)
                    else:
                        self.resetMove()
                else:
                    self.gt.current.board().set_at(self.moveSrc.x(),self.moveSrc.y(),self.grabbedPiece)
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
                piece = self.gt.current.board().get_at(i,7-j)
                if(piece in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    qp.drawImage(x,y,self.pieceImages.getWp(piece, squareSize))

        if(self.drawGrabbedPiece):
            offset = squareSize // 2          
            qp.drawImage(self.grabbedX-offset,self.grabbedY-offset,self.pieceImages.getWp(self.grabbedPiece, squareSize))


        qp.setPen(darkWhite)
        qp.setFont(QtGui.QFont('Decorative',8))
        
        for i in range(0,8):
            idx = str(chr(65+i))
            qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                        boardOffsetY+(8*squareSize)+(self.borderWidth-3),idx)
            qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(8-i))

         

class MainWindow(QtGui.QMainWindow):
    def __init__(self):
        QtGui.QMainWindow.__init__(self)

        test = State()
        #test.test()
        #test.toFen()

        self.resize(640, 480)
        self.setWindowTitle('menubar')
        
        
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
        
        movesEdit = QtGui.QPlainTextEdit()
        vbox.addWidget(movesEdit)

        engineOutput = QtGui.QPlainTextEdit()
        vbox.addWidget(engineOutput)
        
        hbox.addLayout(vbox)
        
        
        mainWidget.setLayout(hbox)
        self.setCentralWidget(mainWidget);

        statusbar = self.statusBar()
        statusbar.showMessage('Ready')

        menu = self.menuBar().addMenu('File')
        action = menu.addAction('Change File Path')
        

app = QtGui.QApplication(sys.argv)
main = MainWindow()
main.show()
sys.exit(app.exec_())