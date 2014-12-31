#!/usr/bin/python

# classes from Jerry:
from GUI.GUIPrinter import GUIPrinter
from GUI.PieceImages import PieceImages
from GUI.MovesEdit import MovesEdit
from GUI.ChessBoardView import ChessboardView, GameState
from GUI.ChessBoardView import MODE_PLAY_WHITE,MODE_ANALYSIS,MODE_ENTER_MOVES,MODE_PLAY_BLACK
from dialogs.DialogEditGameData import DialogEditGameData
from dialogs.DialogPromotion import DialogPromotion
from dialogs.DialogEnterPosition import DialogEnterPosition
from dialogs.DialogAbout import DialogAbout
from dialogs.DialogNewGame import DialogNewGame
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

        self.movesEdit = MovesEdit(self.gs)

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

        self.lcd1 = QLCDNumber(self)

        self.lcd1.setSegmentStyle(QLCDNumber.Flat)
        self.lcd1.display(time.strftime("%H"+":"+"%M"))
        self.lcd1.setFrameStyle(QFrame.NoFrame)

        self.lcd2 = QLCDNumber(self)
        self.lcd2.setSegmentStyle(QLCDNumber.Flat)
        self.lcd2.display(time.strftime("%H"+":"+"%M"))

        self.lcd2.setFrameStyle(QFrame.NoFrame)


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
        hboxLcd.addWidget(self.lcd1)
        hboxLcd.addStretch(1)
        hboxLcd.addWidget(labelBlack)
        hboxLcd.addWidget(self.lcd2)

        vbox = QVBoxLayout();
        vbox.addLayout(hboxLcd)

        self.name = QLabel()
        self.name.setAlignment(Qt.AlignCenter)

        self.name.setBuddy(self.movesEdit)
        vbox.addWidget(self.name)

        vbox.addWidget(self.movesEdit)
        self.board.movesEdit = self.movesEdit

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
        new_game = m_file.addAction('New Game')
        new_game.triggered.connect(self.on_newgame)
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
        setup_clock = m_edit.addAction("Time Controls...")
        m_mode = self.menuBar().addMenu("Mode")
        ag = QActionGroup(self, exclusive=True)
        analysis = QAction("Analysis Mode",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(analysis))
        analysis.triggered.connect(self.on_analysis_mode)
        play_white = QAction("Play as White",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(play_white))
        play_white.triggered.connect(self.on_play_as_white)

        play_black = QAction("Play as Black",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(play_black))
        play_black.triggered.connect(self.on_play_as_black)

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

        # timer
        self.blitz_timer = QTimer()
        self.blitz_timer.timeout.connect(self.update_timer)
        self.blitz_timer.start(1000)

        # in seconds
        self.gs.time_white = 500
        self.gs.time_black = 500
        self.gs.timed_game = True

        #QTimer.singleShot(3000, self.update_timer)

        self.connect(self.engine, SIGNAL("updateinfo(QString)"),self.engineOutput.setHtml)
        self.connect(self.movesEdit, SIGNAL("statechanged()"),self.board.on_statechanged)
        self.connect(self.movesEdit, SIGNAL("statechanged()"),self.on_statechanged)
        self.connect(self.board, SIGNAL("statechanged()"),self.movesEdit.on_statechanged)
        self.connect(self.engine, SIGNAL("bestmove(QString)"),self.board.on_bestmove)

    def hms_from_secs(self,secs):
        hh = secs // (60*60)
        mm = secs // 60
        ss = secs % 60
        return (hh,mm,ss)

    def update_timer(self):
        if(self.gs.timed_game):
            if(self.gs.current.board().turn == chess.WHITE):
                self.gs.time_white -= 1
            if(self.gs.current.board().turn == chess.BLACK):
                self.gs.time_black -= 1
            w_hh,w_mm,w_ss = self.hms_from_secs(self.gs.time_white)
            b_hh,b_mm,b_ss = self.hms_from_secs(self.gs.time_black)

            self.lcd1.display("%02d:%02d:%02d" % (w_hh,w_mm,w_ss))
            self.lcd2.display("%02d:%02d:%02d" % (b_hh,b_mm,b_ss))

            self.update()

        #print("triggered")


    def on_newgame(self):
        dialog = DialogNewGame()
        print("exec dialog")
        if dialog.exec_() == QDialog.Accepted:
            self.gs = GameState()
            self.board.gs = self.gs
            self.movesEdit.gs = self.gs
            self.movesEdit.update()
            self.gs.strength_level = dialog.slider.value()
            if(dialog.rb_untimed.isChecked()):
                self.gs.computer_think_time = dialog.think_secs.value()*1000
            else:
                self.gs.timed_game = True
                if(dialog.rb_blitz1):
                    self.gs.time_white = 60
                    self.gs.time_black = 60
                elif(dialog.rb_blitz2_12):
                    self.gs.time_white = 120
                    self.gs.time_black = 120
                    self.gs.add_secs_per_move = 12
                elif(dialog.rb_blitz3_5):
                    self.gs.time_white = 180
                    self.gs.time_black = 180
                    self.gs.add_secs_per_move = 5
                elif(dialog.rb_blitz5):
                    self.gs.time_white = 300
                    self.gs.time_black = 300
                elif(dialog.rb_blitz15):
                    self.gs.time_white = 900
                    self.gs.time_black = 900
                elif(dialog.rb_blitz30):
                    self.gs.time_white = 1800
                    self.gs.time_black = 1800
            print("calling update board")
            self.board.on_statechanged()
            print("BOARD UPDATED")
            if(dialog.rb_plays_white.isChecked()):
                print("plays white")
                self.on_play_as_white()
            else:
                print("plays black")
                self.board.flippedBoard = True
                self.on_play_as_black()


    def on_statechanged(self):
        if(self.gs.mode == MODE_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_infinite()
        if((self.gs.mode == MODE_PLAY_WHITE and self.gs.current.board().turn == chess.BLACK) or
            (self.gs.mode == MODE_PLAY_BLACK and self.gs.current.board().turn == chess.WHITE)):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)

    def on_analysis_mode(self):
        self.engine.start_engine("mooh")
        self.engine.flip_eval(False)
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

    def on_play_as_black(self):
        self.gs.mode = MODE_PLAY_BLACK
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.engine.start_engine("mooh")
        self.engine.flip_eval(False)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.engine.uci_strength(self.gs.strength_level)
        print("MOVE : "+str(self.gs.board().turn == chess.BLACK))
        if(self.gs.current.board().turn == chess.WHITE):
            print("white to move")
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)

    def on_play_as_white(self):
        self.gs.mode = MODE_PLAY_WHITE
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.engine.start_engine("mooh")
        self.engine.flip_eval(True)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.engine.uci_strength(self.gs.strength_level)
        print("MOVE : "+str(self.gs.board().turn))
        if(self.gs.current.board().turn == chess.BLACK):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)


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