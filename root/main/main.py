#!/usr/bin/python

# classes from Jerry:
from GUI.GUIPrinter import GUIPrinter
from GUI.PieceImages import PieceImages
from GUI.MovesEdit import MovesEdit
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

MODE_ENTER_MOVES = 0
MODE_PLAY_BLACK = 1
MODE_PLAY_WHITE = 2
MODE_ANALYSIS = 3

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

class GameState(Game):

    def __init__(self):
        super(GameState, self).__init__()
        self.current = self.root()
        self.mode = MODE_ENTER_MOVES
        self.printer = GUIPrinter()

class ChessboardView(QWidget):
    
    def __init__(self,gamestate,engine):
        super(ChessboardView, self).__init__()
        #super(QWidget, self).__init__()
        policy = QSizePolicy(QSizePolicy.Preferred, QSizePolicy.Preferred)
        self.setSizePolicy(policy)

        self.gs = gamestate
        self.engine = engine

        self.setup_headers(self.gs.current)

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

    def print_game(self):
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            exporter = chess.pgn.StringExporter()
            self.gs.current.root().export(exporter, headers=True, variations=True, comments=True)
            pgn = str(exporter)
            QPgn = QPlainTextEdit(pgn)
            QPgn.print_(dialog.printer())

    def print_position(self):
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            p = QPixmap.grabWindow(self.winId())
            painter = QPainter(dialog.printer())
            dst = QRect(0,0,200,200)
            painter.drawPixmap(dst, p)
            del painter

    def save_image(self):
        filename = QFileDialog.getSaveFileName(self, 'Save Image', None, 'JPG (*.jpg)', QFileDialog.DontUseNativeDialog)
        if(filename):
            p = QPixmap.grabWindow(self.winId())
            p.save(filename,'jpg')


    def setup_headers(self,game):
        game.headers["Event"] = ""
        game.headers["Site"] = "MyTown"
        game.headers["Date"] = time.strftime("%Y.%m.%d")
        game.headers["Round"] = ""
        game.headers["White"] = "N.N."
        game.headers["Black"] = "Jerry (PC)"
        game.headers["Result"] = "*"

    def append_to_pgn(self):
        filename = QFileDialog.getSaveFileName(self, 'Append to PGN', None,
                                                     'PGN (*.pgn)', QFileDialog.DontConfirmOverwrite)
        if(filename):
            print("append saver")

    def save_to_pgn(self):
        filename = QFileDialog.getSaveFileName(self, 'Save PGN', None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
        if(filename):
            f = open(filename,'w')
            print(self.gs.current.root(), file=f, end="\n\n")
            print("pgn saver")

    def open_pgn(self):
        filename = QFileDialog.getOpenFileName(self, 'Open PGN', None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
        if(filename):
            pgn = open(filename)
            first_game = chess.pgn.read_game(pgn)
            self.gs.current = first_game
            self.update()
            self.emit(SIGNAL("statechanged()"))
            #self.movesEdit.bv = self

            #self.movesEdit.update_san()
            #self.movesEdit.setFocus()

            print("open pgn dummy")

    def editGameData(self):
        ed = DialogEditGameData(self.gs.current.root())
        answer = ed.exec_()
        if(answer):
            root = self.gs.current.root()
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
        self.mainWindow.setLabels(self.gs.current)

    def show_about(self):
        d = DialogAbout()
        d.exec_()

    def game_to_clipboard(self):
        clipboard = QApplication.clipboard()
        exporter = chess.pgn.StringExporter()
        self.gs.current.root().export(exporter, headers=True, variations=True, comments=True)
        pgn_string = str(exporter)
        clipboard.setText(pgn_string)

    def pos_to_clipboard(self):
        clipboard = QApplication.clipboard()
        clipboard.setText(self.gs.current.board().fen())

    def from_clipboard(self):
        clipboard = QApplication.clipboard()
        try:
            root = chess.pgn.Game()
            self.setup_headers(root)
            root.headers["FEN"] = ""
            root.headers["SetUp"] = ""
            board = chess.Bitboard(clipboard.text())
            root.setup(board)
            if(root.board().status() == 0):
                self.gs.current = root
        except ValueError:
            pgn = io.StringIO(clipboard.text())
            first_game = chess.pgn.read_game(pgn)
            self.gs.current = first_game

        self.update()
        self.emit(SIGNAL("statechanged()"))
        #self.movesEdit.bv = self

        #self.movesEdit.update_san()
        #self.mainWindow.setLabels(self.gs.current)
        #self.movesEdit.setFocus()

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

    def enter_position(self):
        dialog = DialogEnterPosition(self.gs.current.board())
        answer = dialog.exec_()
        if(answer):
            root = chess.pgn.Game()
            root.headers["FEN"] = ""
            root.headers["SetUp"] = ""
            root.setup(dialog.displayBoard.board)
            self.gs.current = root
            #self.movesEdit.current = root
            #self.movesEdit.update_san()
            self.emit(SIGNAL("statechanged()"))
            self.setup_headers(self.gs.current)
            self.mainWindow.setLabels(self.gs.current)
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
        #new_node = chess.pgn.GameNode()
        #new_node.parent = temp
        #new_node.move = chess.Move.from_uci(uci)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False
        #self.movesEdit = MovesEdit(self)
        #self.movesEdit.update_san()
        self.emit(SIGNAL("statechanged()"))
        print(self.gs.current.root())
        print("castling rights: "+str(self.gs.current.board().castling_rights))
        if(self.gs.mode == MODE_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_infinite()

        
    def resetMove(self):
        #self.gt.current.board.set_at(self.moveSrc.x,self.moveSrc.y,self.grabbedPiece)
        self.moveSrc = None
        self.grabbedPiece = None
        self.drawGrabbedPiece = False

    def __players_turn(self):
        if(self.gs.mode == MODE_PLAY_BLACK and self.current.board().turn == chess.WHITE):
            return False
        if(self.gs.mode == MODE_PLAY_WHITE and self.current.board().turn == chess.BLACK):
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



class MainWindow(QMainWindow):
    def __init__(self):
        QMainWindow.__init__(self)

        self.resize(800, 500)
        self.setWindowTitle('Jerry - Chess')
        self.centerOnScreen()

        exit = QAction(QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip('Exit application')
        self.connect(exit, SIGNAL('triggered()'), SLOT('close()'))

        self.gs = GameState()
        self.gs.mode = MODE_ENTER_MOVES
        self.engine = Uci_controller()

        #self.engine.start_engine("/Users/user/workspace/Jerry/root/main/stockfish-5-64")
        #self.engine.uci_newgame()
        #self.engine.uci_ok()

        self.board = ChessboardView(self.gs,self.engine)

        movesEdit = MovesEdit(self.gs)

        #board.getState().setInitPos()
        
        spLeft = QSizePolicy();
        spLeft.setHorizontalStretch(1);

        ok = QPushButton("OK")
        cancel = QPushButton("Cancel")

        mainWidget = QWidget()

        hbox = QHBoxLayout()
        hbox.addWidget(self.board)
        
        spRight = QSizePolicy();
        spRight.setHorizontalStretch(2);
 
        lcd1 = QLCDNumber(self)
        
        lcd1.setSegmentStyle(QLCDNumber.Flat)
        lcd1.display(time.strftime("%H"+":"+"%M"))
        lcd1.setFrameStyle(QFrame.NoFrame)
        
        lcd2 = QLCDNumber(self)
        lcd2.setSegmentStyle(QLCDNumber.Flat)
        lcd2.display(time.strftime("%H"+":"+"%M"))

        lcd2.setFrameStyle(QFrame.NoFrame)

        
        hboxLcd = QHBoxLayout()
        
        
        pixmapWhite = QPixmap("../res/icons/whiteClock.png")
        pixmapBlack = QPixmap("../res/icons/blackClock.png")
        
        labelWhite = QLabel()
        labelWhite.setPixmap(pixmapWhite)
        labelWhite.setAlignment(Qt.AlignRight)
        
        labelBlack = QLabel()
        labelBlack.setPixmap(pixmapBlack)
        labelBlack.setAlignment(Qt.AlignRight)
        
        hboxLcd.addWidget(labelWhite)
        hboxLcd.addWidget(lcd1)
        hboxLcd.addStretch(1)
        hboxLcd.addWidget(labelBlack)
        hboxLcd.addWidget(lcd2)

        vbox = QVBoxLayout();
        vbox.addLayout(hboxLcd)

        self.name = QLabel()
        self.name.setAlignment(Qt.AlignCenter)

        self.name.setBuddy(movesEdit)
        vbox.addWidget(self.name)

        vbox.addWidget(movesEdit)
        self.board.movesEdit = movesEdit

        self.engineOutput = QTextEdit()

        vbox.addWidget(self.engineOutput)
        
        hbox.addLayout(vbox)
        
        
        mainWidget.setLayout(hbox)
        self.setCentralWidget(mainWidget);

        statusbar = self.statusBar()
        statusbar.showMessage('Ready')

        self.menubar = self.menuBar()

        self.setLabels(self.gs.current)

        m_file = self.menuBar().addMenu('File ')
        new_game_white = m_file.addAction('New Game (White)')
        new_game_black = m_file.addAction("New Game (Black)")
        m_file.addSeparator()
        load_game = m_file.addAction("Load PGN")
        load_game.triggered.connect(self.board.open_pgn)
        save_game = m_file.addAction("Save PGN")
        save_game.triggered.connect(self.board.save_to_pgn)
        append_game = m_file.addAction("Append to PGN")
        append_game.triggered.connect(self.board.append_to_pgn)
        m_file.addSeparator()
        save_diag = m_file.addAction("Save Position as Image")
        save_diag.triggered.connect(self.board.save_image)
        m_file.addSeparator()
        print_game = m_file.addAction("Print Game")
        print_game.triggered.connect(self.board.print_game)
        print_pos = m_file.addAction("Print Position")
        print_pos.triggered.connect(self.board.print_position)
        m_file.addSeparator()
        exit_item = m_file.addAction("Quit")
        exit_item.triggered.connect(QApplication.quit)
        m_edit = self.menuBar().addMenu('Edit ')
        copy_game = m_edit.addAction("Copy Game")
        copy_game.triggered.connect(self.board.game_to_clipboard)
        copy_pos = m_edit.addAction("Copy Position")
        copy_pos.triggered.connect(self.board.pos_to_clipboard)
        paste = m_edit.addAction("Paste")
        paste.triggered.connect(self.board.from_clipboard)
        m_edit.addSeparator()
        enter_pos = m_edit.addAction("Enter Position")
        enter_pos.triggered.connect(self.board.enter_position)
        m_edit.addSeparator()
        edit_game_data = m_edit.addAction("Edit Game Data")
        edit_game_data.triggered.connect(self.board.editGameData)
        flip = m_edit.addAction("Flip Board")
        flip.triggered.connect(self.board.flip_board)
        m_edit.addSeparator()
        offer_draw = m_edit.addAction("Offer Draw")
        give_up = m_edit.addAction("Give Up")
        m_edit.addSeparator()
        stop_clock = m_edit.addAction("Stop Clock")
        setup_clock = m_edit.addAction("Setup Clock")
        m_mode = self.menuBar().addMenu("Mode")
        ag = QActionGroup(self, exclusive=True)
        analysis = QAction("Analysis Mode",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(analysis))
        analysis.triggered.connect(self.on_analysis_mode)
        play_white = QAction("Play as White",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(play_white))
        play_black = QAction("Play as Black",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(play_black))

        enter_moves = QAction("Enter Moves",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(enter_moves))
        enter_moves.triggered.connect(self.on_enter_moves_mode)
        enter_moves.setChecked(True)
        m_mode.addSeparator()
        set_strength = m_mode.addAction("Strength Level")
        m_mode.addSeparator()
        analyze_game = m_mode.addAction("Full Game Analysis")
        play_out_pos = m_mode.addAction("Play out Position")
        m_help = self.menuBar().addMenu("Help")
        about = m_help.addAction("About")
        about.triggered.connect(self.board.show_about)
        m_help.addSeparator()    
        # self.connect(action2, QtCore.SIGNAL('triggered()'), QtCore.SLOT(board.flip_board()))

        self.connect(self.engine, SIGNAL("updateinfo(QString)"),self.engineOutput.setHtml)
        self.connect(movesEdit, SIGNAL("statechanged()"),self.board.on_statechanged)
        self.connect(movesEdit, SIGNAL("statechanged()"),self.on_statechanged)
        self.connect(self.board, SIGNAL("statechanged()"),movesEdit.on_statechanged)

    def on_statechanged(self):
        if(self.gs.mode == MODE_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_infinite()

    def on_analysis_mode(self):
        self.engine.start_engine("mooh")
        self.engine.uci_ok()
        self.engine.uci_newgame()
        uci_string = self.gs.printer.to_uci(self.gs.current)
        self.engine.uci_send_position(uci_string)
        self.engine.uci_go_infinite()
        self.gs.mode = MODE_ANALYSIS

    def on_enter_moves_mode(self):
        # stop any engine
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.gs.mode = MODE_ENTER_MOVES

    def on_play_as_black(self): pass

    def on_play_as_white(self): pass
        
        
    def centerOnScreen (self):
        '''centerOnScreen()
           Centers (vertically in the upper third) the window on the screen.'''
        resolution = QDesktopWidget().screenGeometry()
        self.move((resolution.width() / 2) - (self.frameSize().width() / 2),
                  (resolution.height() / 2) - (self.frameSize().height()*2 / 3))


    def setLabels(self,game):
        self.name.setText("<b>"+game.headers["White"]+
                      " - "+
                      game.headers["Black"]+"</b><br>"+
                      game.headers["Site"]+ " "+
                      game.headers["Date"])

    def closeEvent(self, event):
        self.board.engine.stop_engine()
        print ("inside the close")



app = QApplication(sys.argv)
main = MainWindow()
app.setActiveWindow(main)
main.show()
sys.exit(app.exec_())