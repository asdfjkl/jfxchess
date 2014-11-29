from GUI.PieceImages import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *
from chess.pgn import Game, GameNode

class DisplayBoard(QWidget):

    def __init__(self, node = None):
        super(QWidget, self).__init__()
        policy = QSizePolicy(QSizePolicy.Preferred, QSizePolicy.Preferred)
        self.setSizePolicy(policy)

        if(node == None):
            node = Game()

        self.current = node

        self.borderWidth = 12
        self.pieceImages = PieceImages()

        self.initUI()



    def initUI(self):
        self.show()

    def calculateBoardSize(self):
        size = self.size()
        boardSize = min(size.width(), size.height())
        squareSize = (boardSize-(2*self.borderWidth))//8
        boardSize = 8 * squareSize + 2 * self.borderWidth
        return (boardSize,squareSize)

    def resizeEvent(self, e):
        self.setMinimumWidth(self.height())

    def paintEvent(self, event):

        qp = QPainter()
        qp.begin(self)
        self.drawBoard(event, qp)
        qp.end()

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

        # draw Board

        for i in range(0,8):
            for j in range(0,8):
                if((j%2 == 0 and i%2 ==1) or (j%2 == 1 and i%2 ==0)):
                    qp.setBrush(lightBlue)
                else:
                    qp.setBrush(lightBlue2)
                #draw Square
                x = boardOffsetX+(i*squareSize)
                # drawing coordinates are from top left
                # whereas chess coords are from bottom left
                y = boardOffsetY+((7-j)*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #draw Piece
                piece = self.current.board().piece_at(j*8+i)
                if(piece != None and piece.symbol() in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    qp.drawImage(x,y,self.pieceImages.getWp(piece.symbol(), squareSize))

        qp.setPen(darkWhite)
        qp.setFont(QFont('Decorative',8))

        for i in range(0,8):
            idx = str(chr(65+i))
            qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                            boardOffsetY+(8*squareSize)+(self.borderWidth-3),idx)
            qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(8-i))


class DialogEnterPosition(QDialog):

    def __init__(self, node=None, parent=None):
        super(DialogEnterPosition, self).__init__(parent)
        self.resize(300, 90)

        self.setWindowTitle("Enter Position")

        self.displayBoard = DisplayBoard(node)


        self.cbWhiteShort = QCheckBox("White O-O")
        self.cbWhiteLong = QCheckBox("White O-O-O")
        self.cbBlackShort = QCheckBox("Black O-O")
        self.cbBlackLong = QCheckBox("Black O-O-O")
        grpBox_castle = QGroupBox("Castling Rights")
        vbox_castle = QVBoxLayout()
        vbox_castle.addWidget(self.cbWhiteShort)
        vbox_castle.addWidget(self.cbWhiteLong)
        vbox_castle.addWidget(self.cbBlackShort)
        vbox_castle.addWidget(self.cbBlackLong)
        vbox_castle.addStretch(1)
        grpBox_castle.setLayout(vbox_castle)

        self.rbWhite = QRadioButton("White To Move")
        self.rbBlack = QRadioButton("Black To Move")
        grpBox_turn = QGroupBox("Turn")
        vbox_radio = QVBoxLayout()
        vbox_radio.addWidget(self.rbWhite)
        vbox_radio.addWidget(self.rbBlack)
        vbox_radio.addStretch(1)
        grpBox_turn.setLayout(vbox_radio)

        self.buttonInit = QPushButton("Initial Position")
        self.buttonClear = QPushButton("Clear Board")
        self.buttonCurrent = QPushButton("Current Position")

        vbox_config = QVBoxLayout()
        vbox_config.addWidget(grpBox_castle)
        vbox_config.addWidget(grpBox_turn)
        vbox_config.addStretch(1)
        vbox_config.addWidget(self.buttonInit)
        vbox_config.addWidget(self.buttonClear)
        vbox_config.addWidget(self.buttonCurrent)

        hbox = QHBoxLayout()
        hbox.addWidget(self.displayBoard)
        hbox.addLayout(vbox_config)

        vbox = QVBoxLayout()
        vbox.addLayout(hbox)
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        vbox.addWidget(buttonBox)

        self.setLayout(vbox)