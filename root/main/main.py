import sys
from PyQt4 import QtGui

#!/usr/bin/python

# boxlayout.py
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from PyQt4.QtSvg import *


#!/usr/bin/python

# menubar.py 

import sys, random, time
from PyQt4 import QtGui, QtCore, QtSvg

class GameState():
    
    # PBNRKQ pbnrkq
    def __init__(self): 
        self.board = [['e' for x in range(8)] for x in range(8)]
        self.castleWhiteShort = False
        self.castleBlackShort = False
        self.castleWhiteLong = False
        self.castleBlackLong = False

    def get(self,x,y):
        return self.board[x][y]

    def setInitPos(self):
        #pawns
        for x in range(0,8):
            self.board[x][1] = 'P'
            self.board[x][6] = 'p'
        #knights
        self.board[1][0] = 'N'
        self.board[6][0] = 'N'
        self.board[1][7] = 'n'
        self.board[6][7] = 'n'
        #bishops
        self.board[2][0] = 'B'
        self.board[5][0] = 'B'
        self.board[2][7] = 'b'
        self.board[5][7] = 'b'
        #rooks
        self.board[0][0] = 'R'
        self.board[7][0] = 'R'
        self.board[0][7] = 'r'
        self.board[7][7] = 'r'
        #queens
        self.board[3][0] = 'Q'
        self.board[3][7] = 'q'
        #kings
        self.board[4][0] = 'K'
        self.board[4][7] = 'k'

class PieceImages:
    def __init__(self):
        self.pieces = {'P': {} , 'R':{},'N':{},'B':{},'Q':{},'K':{},
                       'p':{},'r':{},'n':{},'b':{},'q':{},'k':{}}
        self.rens = {}
        renWp = QSvgRenderer()
        wb = ["w","b"]
        pcs = ["p","r","b","n","q","k"]
        for fst in wb:
            for snd in pcs:
                ren = QSvgRenderer()
                QSvgRenderer.load(ren,"../res/pieces/"+fst+snd+".svg")
                if fst == "w":
                    self.rens[snd] = ren
                else:
                    self.rens[snd.upper()] = ren
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
        #policy.setHeightForWidth(True)
        self.setSizePolicy(policy)
        self.board = GameState()
        self.pieceImages = PieceImages()
        self.initUI()
        
    def initUI(self):      
        self.show()
    
    def getBoard(self):
        return self.board
        
    def heightForWidth(self, width):
        return width    

    def resizeEvent(self, e):
        self.setMinimumWidth(self.height())

    def paintEvent(self, event):

        qp = QtGui.QPainter()
        qp.begin(self)
        #self.drawText(event, qp)
        self.drawPoints(event, qp)
        qp.end()
    
    def drawPoints(self, event, qp):

        penZero = QtGui.QPen(QtCore.Qt.black, 1, QtCore.Qt.NoPen)
        qp.setPen(penZero)

        black = QtGui.QColor(0,0,0)
        darkBlue = QtGui.QColor(56,66,91)
        lightBlue = QtGui.QColor(111,132,181)
        darkWhite = QtGui.QColor(239,239,239) 
        
        qp.setBrush(darkBlue)
        
        size = self.size()
        borderWidth = 12        
        boardSize = min(size.width(), size.height())
        squareSize = (boardSize-(2*borderWidth))//8
        boardSize = 8 * squareSize + 2 * borderWidth
        
        qp.drawRect(1,1,boardSize,boardSize)
        
        boardOffsetX = borderWidth;
        boardOffsetY = borderWidth;
        
        # draw Board


        #qp.setFont(QtGui.QFont(self.chessFontFamily,80))
        #qp.setFont(QtGui.QFont('Decorative',40))
        
        for i in range(0,8):
            for j in range(0,8):
                if((j%2 == 0 and i%2 ==1) or (j%2 == 1 and i%2 ==0)):
                    qp.setBrush(lightBlue)
                else:
                    qp.setBrush(darkWhite)        
                #draw Square
                x = boardOffsetX+(i*squareSize)
                y = boardOffsetY+(j*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #qp.drawText(50,50,'♙')
                #draw Piece
                piece = self.getBoard().get(i,j)
                if(piece in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    qp.drawImage(x,y,self.pieceImages.getWp(piece, squareSize))

        qp.setPen(darkWhite)
        qp.setFont(QtGui.QFont('Decorative',8))
        
        for i in range(0,8):
            idx = str(chr(65+i))
            qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                        boardOffsetY+(8*squareSize)+(borderWidth-3),idx)
            qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(i+1))

        
    def drawText(self, event, qp):
        x = 5
        #qp.setPen(QtGui.QColor(168, 34, 3))
        #qp.setFont(QtGui.QFont('Decorative', 10))
        #qp.drawText(event.rect(), QtCore.Qt.AlignCenter, self.text)   

class MainWindow(QtGui.QMainWindow):
    def __init__(self):
        QtGui.QMainWindow.__init__(self)

        test = GameState()

        self.resize(640, 480)
        self.setWindowTitle('menubar')
        
        
        #qp.drawImage(10,10,qim,0,0,0,0)
        

        exit = QtGui.QAction(QtGui.QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip('Exit application')
        self.connect(exit, QtCore.SIGNAL('triggered()'), QtCore.SLOT('close()'))

        board = ChessboardView()
        board.getBoard().setInitPos()
        
        #board.setSizePolicy(QtGui.QSizePolicy.Expanding, QtGui.QSizePolicy.Expanding)
        spLeft = QtGui.QSizePolicy();
        spLeft.setHorizontalStretch(1);
        # board.setSizePolicy(spLeft)
        ok = QtGui.QPushButton("OK")
        cancel = QtGui.QPushButton("Cancel")

        mainWidget = QtGui.QWidget()

        hbox = QtGui.QHBoxLayout()
        #hbox.addStretch(3)
        #hbox.addStretch(1)
        hbox.addWidget(board)
        
        spRight = QtGui.QSizePolicy();
        spRight.setHorizontalStretch(2);
        # ok.setSizePolicy(spRight)
        #hbox.addStretch(2)
        #hbox.addWidget(ok)
        #hbox.addWidget(cancel)

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
        #label.setAutoFillBackground(True)
        #palette = QtGui.QPalette()
        #palette.setColor(QtGui.QPalette.Foreground,black)
        #palette.setColor(QtGui.QPalette.Background,black)
        #label.setPalette(palette)
        
        spacerLcd = QtGui.QSpacerItem(20,10)
        #icon = QtGui.QIcon()
        #checkBox = QtGui.QCheckBox()
        #checkBox.setPalette(palette)
        
        hboxLcd.addWidget(labelWhite)
        #hboxLcd.addWidget(checkBox)
        #hboxLcd.addWidget(toolButtonWhite)
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