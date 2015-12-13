from views.piece_images import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *
from chess.pgn import Game, GameNode
from chess import Piece, WHITE, BLACK,Board
import chess
#from chess import CASTLING, CASTLING_BLACK_KINGSIDE, CASTLING_BLACK_QUEENSIDE, \
#    CASTLING_WHITE_KINGSIDE, CASTLING_WHITE_QUEENSIDE, CASTLING_NONE

class DisplayBoard(QWidget):

    def __init__(self, board = None, parent = None):
        super(QWidget, self).__init__()
        policy = QSizePolicy(QSizePolicy.Preferred, QSizePolicy.Preferred)
        self.setSizePolicy(policy)

        if(board == None):
            board = Board()
            board.castling_rights = 0

        self.board = board

        self.parent = parent

        self.borderWidth = 12
        self.pieceImages = PieceImages()

        self.selected_xy = None
        self.pcs = [['P','R','B','N','Q','K'],['p','r','b','n','q','k']]


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
        self.setMinimumWidth(self.height()*1.35)

    def paintEvent(self, event):

        qp = QPainter()
        qp.begin(self)
        self.drawBoard(event, qp)
        qp.end()

    def mousePressEvent(self, mouseEvent):
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())

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
            return (x,y)
        # check if x,y are on place where pieces
        # are selected
        if(x > self.borderWidth + 9*squareSize and x < self.borderWidth + 11*squareSize
            and y > self.borderWidth and y < self.borderWidth + 6*squareSize):
            x = x - (self.borderWidth + 9*squareSize)
            y = y - self.borderWidth
            x = (x // squareSize)+8
            y = y//squareSize
            return (x,y)
        return None

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

        #draw rect (i.e. border) around board
        qp.drawRect(1,1,boardSize,boardSize)

        #draw rect (i.e. border) around pick up fields
        qp.drawRect(9*squareSize,1,2*squareSize+2*self.borderWidth,6 * squareSize + 2 * self.borderWidth)

        boardOffsetX = self.borderWidth;
        boardOffsetY = self.borderWidth;

        # draw Board

        for i in range(0,8):
            for j in range(0,8):
                if((j%2 == 0 and i%2 ==1) or (j%2 == 1 and i%2 ==0)):
                    qp.setBrush(lightBlue2)
                else:
                    qp.setBrush(lightBlue)
                #draw Square
                x = boardOffsetX+(i*squareSize)
                # drawing coordinates are from top left
                # whereas chess coords are from bottom left
                y = boardOffsetY+((7-j)*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #draw Piece
                piece = self.board.piece_at(j*8+i)
                if(piece != None and piece.symbol() in ('P','R','N','B','Q','K','p','r','n','b','q','k')):
                    qp.drawImage(x,y,self.pieceImages.getWp(piece.symbol(), squareSize))

        for i in range(0,6):
            for j in range(0,2):
                qp.setBrush(lightBlue2)
                if(self.selected_xy != None and self.selected_xy == (j,i)):
                    qp.setBrush(lightBlue)
                #draw Square
                x = boardOffsetX+((9+j)*squareSize)
                # drawing coordinates are from top left
                # whereas chess coords are from bottom left
                y = boardOffsetY+(i*squareSize)
                qp.drawRect(x,y,squareSize,squareSize)
                #draw Piece
                qp.drawImage(x,y,self.pieceImages.getWp(self.pcs[j][i], squareSize))

        qp.setPen(darkWhite)
        qp.setFont(QFont('Decorative',8))

        for i in range(0,8):
            idx = str(chr(65+i))
            qp.drawText(boardOffsetX+(i*squareSize)+(squareSize/2)-4,
                            boardOffsetY+(8*squareSize)+(self.borderWidth-3),idx)
            qp.drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,str(8-i))

    def mousePressEvent(self, mouseEvent):
        pos = self.getBoardPosition(mouseEvent.x(), mouseEvent.y())
        if(pos):
            x = pos[0]
            y = pos[1]
            if(x > 7):
                self.selected_xy = (x-8,y)
            else:
                if(self.selected_xy != None):
                    (i,j) = self.selected_xy
                    piece = self.pcs[i][j]
                    square = y*8+x
                    current_piece = self.board.piece_at(square)
                    if(current_piece and current_piece.symbol() == piece):
                        self.board.remove_piece_at(square)
                    else:
                        self.board.set_piece_at(square,Piece.from_symbol(piece))
                    if(self.board.status() == 0):
                        self.parent.enable_ok_button()
                    else:
                        self.parent.disable_ok_button()
        self.update()




class DialogEnterPosition(QDialog):

    def __init__(self, board=None, parent=None):
        super(DialogEnterPosition, self).__init__(parent)
        #self.resize(600, 400)

        self.setWindowTitle(self.trUtf8("Enter Position"))

        self.displayBoard = DisplayBoard(self.deep_copy_board_pos(board),self)

        # create a copy of the current board
        if(board):
            # create a deepcopy of current board
            self.current = self.deep_copy_board_pos(board)
        else:
            self.current = Board()

        self.cbWhiteShort = QCheckBox(self.trUtf8("White O-O"))
        self.cbWhiteLong = QCheckBox(self.trUtf8("White O-O-O"))
        self.cbBlackShort = QCheckBox(self.trUtf8("Black O-O"))
        self.cbBlackLong = QCheckBox(self.trUtf8("Black O-O-O"))
        grpBox_castle = QGroupBox(self.trUtf8("Castling Rights"))
        vbox_castle = QVBoxLayout()
        vbox_castle.addWidget(self.cbWhiteShort)
        vbox_castle.addWidget(self.cbWhiteLong)
        vbox_castle.addWidget(self.cbBlackShort)
        vbox_castle.addWidget(self.cbBlackLong)
        vbox_castle.addStretch(1)
        grpBox_castle.setLayout(vbox_castle)

        self.rbWhite = QRadioButton(self.trUtf8("White To Move"))
        self.rbBlack = QRadioButton(self.trUtf8("Black To Move"))
        grpBox_turn = QGroupBox(self.trUtf8("Turn"))
        vbox_radio = QVBoxLayout()
        vbox_radio.addWidget(self.rbWhite)
        vbox_radio.addWidget(self.rbBlack)
        vbox_radio.addStretch(1)
        grpBox_turn.setLayout(vbox_radio)

        self.buttonInit = QPushButton(self.trUtf8("Initial Position"))
        self.buttonClear = QPushButton(self.trUtf8("Clear Board"))
        self.buttonCurrent = QPushButton(self.trUtf8("Current Position"))

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
        self.buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        vbox.addWidget(self.buttonBox)

        self.setLayout(vbox)

        self.connect(self.buttonBox, SIGNAL("accepted()"),
                 self, SLOT("accept()"))
        self.connect(self.buttonBox, SIGNAL("rejected()"),
                 self, SLOT("reject()"))

        self.cbWhiteShort.toggled.connect(self.set_castling_rights)
        self.cbWhiteLong.toggled.connect(self.set_castling_rights)
        self.cbBlackShort.toggled.connect(self.set_castling_rights)
        self.cbBlackLong.toggled.connect(self.set_castling_rights)

        self.rbWhite.toggle()
        self.rbWhite.toggled.connect(self.set_turn)
        self.rbBlack.toggled.connect(self.set_turn)

        self.buttonInit.clicked.connect(self.initial_position)
        self.buttonClear.clicked.connect(self.clear_board)
        self.buttonCurrent.clicked.connect(self.set_current)

        # reset who's current turn it is and the current
        # castling rights of the position
        self.set_castling_rights()
        self.set_turn()

    def set_castling_rights(self):
        self.displayBoard.board.castling_rights = 0
        if(self.cbWhiteShort.isChecked()):
            self.displayBoard.board.castling_rights = self.displayBoard.board.castling_rights  | chess.BB_H1
        if(self.cbWhiteLong.isChecked()):
            self.displayBoard.board.castling_rights = self.displayBoard.board.castling_rights | chess.BB_A1
        if(self.cbBlackShort.isChecked()):
            self.displayBoard.board.castling_rights = self.displayBoard.board.castling_rights | chess.BB_H8
        if(self.cbBlackLong.isChecked()):
            self.displayBoard.board.castling_rights = self.displayBoard.board.castling_rights | chess.BB_A8
        if(self.displayBoard.board.status() == chess.STATUS_VALID):
            self.enable_ok_button()
        else:
            self.disable_ok_button()

    def set_turn(self):
        if(self.rbWhite.isChecked()):
            self.displayBoard.board.turn = WHITE
        else:
            self.displayBoard.board.turn = BLACK
        if(self.displayBoard.board.status() == 0):
            self.enable_ok_button()
        else:
            self.disable_ok_button()

    def clear_board(self):
        self.displayBoard.board.clear()
        self.set_castling_rights()
        self.set_turn()
        self.update()

    def initial_position(self):
        self.displayBoard.board.reset()
        self.set_castling_rights()
        self.set_turn()
        self.update()

    def set_current(self):
        fen = self.current.fen()
        board = Board(fen)
        self.displayBoard.board = board
        self.set_castling_rights()
        self.set_turn()
        self.update()

    # creates a deep copy of the given
    # board into a clean new board, resetting
    # all move histories, castling rights, etc.
    def deep_copy_board_pos(self,board):
        fresh = Board()
        for i in range(0,8):
            for j in range(0,8):
                piece = board.piece_at(j*8+i)
                if(piece):
                    sym = piece.symbol()
                    fresh.set_piece_at(j*8+i,Piece.from_symbol(sym))
                else:
                    fresh.remove_piece_at(j*8+i)
        return fresh

    def enable_ok_button(self):
        self.buttonBox.button(QDialogButtonBox.Ok).setEnabled(True)
        self.update()

    def disable_ok_button(self):
        self.buttonBox.button(QDialogButtonBox.Ok).setEnabled(False)
        self.update()