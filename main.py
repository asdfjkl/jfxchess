#!/usr/bin/python

# classes from Jerry:
from GUI.GUIPrinter import GUIPrinter
from GUI.PieceImages import PieceImages
from GUI.MovesEdit import MovesEdit
from GUI.ChessBoardView import ChessboardView, GameState
from GUI.ChessBoardView import MODE_PLAY_WHITE,MODE_ANALYSIS,MODE_ENTER_MOVES,MODE_PLAY_BLACK,MODE_PLAYOUT_POS,MODE_GAME_ANALYSIS
from dialogs.DialogEditGameData import DialogEditGameData
from dialogs.DialogPromotion import DialogPromotion
from dialogs.DialogEnterPosition import DialogEnterPosition
from dialogs.DialogAbout import DialogAbout
from dialogs.DialogNewGame import DialogNewGame
from dialogs.DialogStrengthLevel import DialogStrengthLevel
from uci.uci_controller import Uci_controller
import os

# python chess
from chess.polyglot import *
from chess.pgn import Game
import chess

# PyQt and python system functions
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
import io
import sys, random, time
from uci.engine_info import EngineInfo
import pickle
from util.appdirs import *

class MainWindow(QMainWindow):
    def __init__(self):
        QMainWindow.__init__(self)

        self.resize(800, 470)
        self.setWindowTitle('Jerry - Chess')
        self.centerOnScreen()

        exit = QAction(QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip('Exit application')
        self.connect(exit, SIGNAL('triggered()'), SLOT('close()'))

        self.gs = GameState()
        print(str(self.gs.best_score))
        #self.gs.current = self.gs.game.root()
        print(type(self.gs.game))
        self.gs.mode = MODE_ENTER_MOVES
        self.engine = Uci_controller()

        self.engine_fn = os.path.dirname(os.path.realpath(sys.argv[0]))
        if sys.platform == 'win32':
            self.engine_fn += "/engine/stockfish.exe"
        elif 'linux' in  sys.platform:
            print("yahoo - we are on linux")
            self.engine_fn += "/engine/stockfish_linux"
        elif sys.platform == 'darwin':
            self.engine_fn += '/engine/stockfish_osx'
        print("engine path: "+ self.engine_fn)
        print("os platform: "+sys.platform)

        appname = 'jerry'
        appauthor = 'dkl'
        fn = user_data_dir(appname, appauthor)
        print(fn)
        self.save_state_dir = fn

        try:
            with open(fn+"/current.raw","rb") as pgn:
                #first_game = chess.pgn.read_game(pgn)
                self.gs = pickle.load(pgn)
                #self.gs.game = first_game
                #self.gs.current = self.gs.game
            pgn.close()
        except BaseException as e:
            print(e)
            pass

        self.gs.mode = MODE_ENTER_MOVES

        """
        try:
            with open("current.fen","r") as fen:
                fen_string = fen.readline()
                print("loaded: "+fen_string)
                #current = self.gs.find_fen(fen_string,self.gs.current.root())
                #if(current):
                #    self.gs.current = current
        except FileNotFoundError as e:
            print(e)
            pass
        """

        #self.engine.start_engine("/Users/user/workspace/Jerry/root/main/stockfish-5-64")
        #self.engine.uci_newgame()
        #self.engine.uci_ok()

        self.board = ChessboardView(self.gs,self.engine)
        self.board.on_statechanged()

        self.movesEdit = MovesEdit(self.gs)
        self.movesEdit.setReadOnly(True)
        self.movesEdit.on_statechanged()

        #board.getState().setInitPos()

        spLeft = QSizePolicy();
        spLeft.setHorizontalStretch(1);

        ok = QPushButton("OK")
        cancel = QPushButton("Cancel")

        mainWidget = QWidget()

        hbox = QHBoxLayout()
        hbox.addWidget(self.board)

        spRight = QSizePolicy()
        spRight.setHorizontalStretch(2)

        vbox = QVBoxLayout()

        self.name = QLabel()
        self.name.setAlignment(Qt.AlignCenter)

        self.name.setBuddy(self.movesEdit)
        vbox.addWidget(self.name)

        vbox.addWidget(self.movesEdit)
        self.board.movesEdit = self.movesEdit

        self.engineOutput = QTextEdit()
        self.engineOutput.setReadOnly(True)

        vbox.addWidget(self.engineOutput)

        hbox.addLayout(vbox)


        mainWidget.setLayout(hbox)
        self.setCentralWidget(mainWidget)

        statusbar = self.statusBar()
        statusbar.showMessage('Jerry v0.8')

        self.menubar = self.menuBar()

        self.setLabels(self.gs.game)
        self.old_score = None

        m_file = self.menuBar().addMenu('File ')
        new_game = m_file.addAction('New Game')
        new_game.setShortcut(QKeySequence.New)
        new_game.triggered.connect(self.on_newgame)
        m_file.addSeparator()
        load_game = m_file.addAction("Load PGN")
        load_game.setShortcut(QKeySequence.Open)
        load_game.triggered.connect(self.board.open_pgn)
        save_game = m_file.addAction("Save as new PGN")
        save_game.setShortcut(QKeySequence.Save)
        save_game.triggered.connect(self.board.save_to_pgn)
        append_game = m_file.addAction("Save (Append) to PGN")
        append_game.setShortcut(QKeySequence.SaveAs)
        append_game.triggered.connect(self.board.append_to_pgn)
        m_file.addSeparator()
        save_diag = m_file.addAction("Save Position as Image")
        save_diag.triggered.connect(self.board.save_image)
        m_file.addSeparator()
        print_game = m_file.addAction("Print Game")
        print_game.triggered.connect(self.board.print_game)
        print_game.setShortcut(QKeySequence.Print)
        print_pos = m_file.addAction("Print Position")
        print_pos.triggered.connect(self.board.print_position)
        m_file.addSeparator()
        exit_item = m_file.addAction("Quit")
        exit_item.setShortcut(QKeySequence.Quit)
        exit_item.triggered.connect(QApplication.quit)
        m_edit = self.menuBar().addMenu('Edit ')
        copy_game = m_edit.addAction("Copy Game")
        copy_game.triggered.connect(self.board.game_to_clipboard)
        copy_game.setShortcut(QKeySequence.Copy)
        copy_pos = m_edit.addAction("Copy Position")
        copy_pos.triggered.connect(self.board.pos_to_clipboard)
        paste = m_edit.addAction("Paste")
        paste.setShortcut(QKeySequence.Paste)
        paste.triggered.connect(self.board.from_clipboard)
        m_edit.addSeparator()
        enter_pos = m_edit.addAction("&Enter Position")
        enter_pos.setShortcut('e')
        enter_pos.triggered.connect(self.enter_position)
        m_edit.addSeparator()
        edit_game_data = m_edit.addAction("Edit Game Data")
        edit_game_data.triggered.connect(self.editGameData)
        flip = m_edit.addAction("&Flip Board")
        flip.setShortcut('f')
        flip.triggered.connect(self.board.flip_board)
        self.display_info = QAction("Show Search &Info",m_edit,checkable=True)
        self.display_info.setShortcut('i')
        m_edit.addAction(self.display_info)
        self.display_info.setChecked(True)
        self.display_info.triggered.connect(self.set_display_info)
        m_edit.addSeparator()
        self.offer_draw = m_edit.addAction("Offer Draw")
        self.offer_draw.triggered.connect(self.handle_offered_draw)
        self.offer_draw.setEnabled(False)
        self.give_up = m_edit.addAction("Resign")
        self.give_up.triggered.connect(self.on_player_resigns)
        self.give_up.setEnabled(False)
        m_edit.addSeparator()
        m_mode = self.menuBar().addMenu("Mode")
        ag = QActionGroup(self, exclusive=True)
        analysis = QAction("&Analysis Mode",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(analysis))
        analysis.setShortcut('a')
        analysis.triggered.connect(self.on_analysis_mode)
        self.play_white = QAction("Play as &White",m_mode,checkable=True)
        self.play_white.setShortcut('w')
        m_mode.addAction(ag.addAction(self.play_white))
        self.play_white.triggered.connect(self.on_play_as_white)

        self.play_black = QAction("Play as &Black",m_mode,checkable=True)
        self.play_black.setShortcut('b')
        m_mode.addAction(ag.addAction(self.play_black))
        self.play_black.triggered.connect(self.on_play_as_black)

        self.enter_moves = QAction("Enter &Moves",m_mode,checkable=True)
        self.enter_moves.setShortcuts([Qt.Key_M,Qt.Key_Escape])
        m_mode.addAction(ag.addAction(self.enter_moves))
        self.enter_moves.triggered.connect(self.on_enter_moves_mode)
        self.enter_moves.setChecked(True)
        m_mode.addSeparator()
        set_strength = m_mode.addAction("Strength Level")
        set_strength.triggered.connect(self.on_strength_level)
        m_mode.addSeparator()
        analyze_game = m_mode.addAction("Full Game Analysis")
        analyze_game.triggered.connect(self.on_game_analysis_mode)
        play_out_pos = m_mode.addAction("Play out Position")
        play_out_pos.triggered.connect(self.on_playout_pos)
        m_help = self.menuBar().addMenu("Help")
        about = m_help.addAction("About")
        about.triggered.connect(self.board.show_about)
        m_help.addSeparator()
        # self.connect(action2, QtCore.SIGNAL('triggered()'), QtCore.SLOT(board.flip_board()))

        self.connect(self.engine, SIGNAL("updateinfo(PyQt_PyObject)"),self.update_engine_output)
        self.connect(self.movesEdit, SIGNAL("statechanged()"),self.board.on_statechanged)
        self.connect(self.movesEdit, SIGNAL("statechanged()"),self.on_statechanged)
        self.connect(self.board, SIGNAL("statechanged()"),self.movesEdit.on_statechanged)
        self.connect(self.engine, SIGNAL("bestmove(QString)"),self.on_bestmove)
        self.connect(self.board,SIGNAL("bestmove(QString)"),self.on_bestmove)
        self.connect(self.board,SIGNAL("drawn"),self.draw_game)
        self.connect(self.board,SIGNAL("checkmate"),self.on_checkmate)

    def update_engine_output(self,engine_info):
        print("rec: "+str(engine_info.score))
        if(self.gs.display_engine_info):
            # engine info has no score. reuse
            # old one from gamestate, if available
            # if so, set flip_eval to false, since
            # the gs state is always in side
            # independent format
            if(engine_info.score == None):
                if(self.gs.score != None):
                    engine_info.score = self.old_score
                    engine_info.flip_eval = False
            else:
                print("SCORE RECEIVED: "+str(engine_info.score))
                if(engine_info.flip_eval):
                    if(engine_info.score == 0.0):
                        print("foo")
                    self.gs.score = - engine_info.score
                else:
                    self.gs.score = engine_info.score
            if(engine_info.pv_arr):
                self.gs.pv = engine_info.pv_arr
            self.gs.mate_threat = None
            if(engine_info.mate != None):
                print("GONNA BE MATE")
                self.gs.mate_threat = engine_info.mate
                #if(self.gs.current.board().turn == chess.WHITE):
                #    self.gs.score =  -1000000
                #else:
                #    self.gs.score =  1000000
            self.engineOutput.setHtml(str(engine_info))


    def set_display_info(self):
        if(self.display_info.isChecked()):
            self.gs.display_engine_info = True
        else:
            self.gs.display_engine_info = False
            self.engineOutput.setHtml("")

    def on_strength_level(self):
        dialog = DialogStrengthLevel(gamestate=self.gs)
        if dialog.exec_() == QDialog.Accepted:
            self.gs.strength_level = dialog.slider_elo.value()
            val = dialog.slider_think.value()
            self.gs.computer_think_time = val
            if(val == 4):
                self.gs.computer_think_time = 5
            elif(val == 5):
                self.gs.computer_think_time = 10
            elif(val == 6):
                self.gs.computer_think_time = 15
            elif(val == 7):
                self.gs.computer_think_time = 30
            self.gs.computer_think_time = self.gs.computer_think_time*1000
        if(not self.gs.mode == MODE_ENTER_MOVES):
            self.engine.uci_strength(self.gs.strength_level)

    def on_newgame(self):
        dialog = DialogNewGame(gamestate=self.gs)
        print("exec dialog")
        if dialog.exec_() == QDialog.Accepted:
            self.gs = GameState()
            self.board.gs = self.gs
            self.movesEdit.gs = self.gs
            self.movesEdit.update()
            self.gs.strength_level = dialog.slider_elo.value()
            val = dialog.slider_think.value()
            self.gs.computer_think_time = val
            if(val == 4):
                self.gs.computer_think_time = 5
            elif(val == 5):
                self.gs.computer_think_time = 10
            elif(val == 6):
                self.gs.computer_think_time = 15
            elif(val == 7):
                self.gs.computer_think_time = 30
            self.gs.computer_think_time = self.gs.computer_think_time*1000
            print("calling update board")
            self.movesEdit.on_statechanged()
            self.board.setup_headers(self.gs.game)
            print("BOARD UPDATED")
            if(dialog.rb_plays_white.isChecked()):
                print("plays white")
                self.play_white.setChecked(True)
                self.setLabels(self.gs.game)
                self.on_play_as_white()
            else:
                print("plays black")
                self.play_black.setChecked(True)
                temp = self.gs.game.headers["White"]
                self.gs.game.headers["White"] = self.gs.game.headers["Black"]
                self.gs.game.headers["Black"] = temp
                self.setLabels(self.gs.game)
                self.on_play_as_black()


    def on_statechanged(self):
        if(self.gs.mode == MODE_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_infinite()
        if((self.gs.mode == MODE_PLAY_WHITE and self.gs.current.board().turn == chess.BLACK) or
            (self.gs.mode == MODE_PLAY_BLACK and self.gs.current.board().turn == chess.WHITE) or
            self.gs.mode == MODE_GAME_ANALYSIS):
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)

    def on_analysis_mode(self):
        self.display_info.setChecked(True)
        self.set_display_info()
        self.engine.stop_engine()
        self.engine.start_engine(self.engine_fn)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.give_up.setEnabled(False)
        self.offer_draw.setEnabled(False)
        uci_string = self.gs.printer.to_uci(self.gs.current)
        self.engine.uci_send_position(uci_string)
        self.engine.uci_go_infinite()
        self.gs.mode = MODE_ANALYSIS

    def on_game_analysis_mode(self):
        #self.gs = self.board.gs
        self.display_info.setChecked(True)
        self.set_display_info()
        self.engine.stop_engine()
        self.engine.start_engine(self.engine_fn)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.give_up.setEnabled(False)
        self.offer_draw.setEnabled(False)
        self.movesEdit.delete_all_comments()
        self.movesEdit.delete_all_variants()
        #self.gs.current = self.gs.game
        self.gs.best_score = None
        self.gs.best_pv = []
        self.gs.mate_threat = False
        print(str(self.gs.current.variations[0]))

        while(len(self.gs.current.variations) > 0):
            print("going down")
            self.gs.current = self.gs.current.variations[0]
        if(self.gs.current.board().is_checkmate() or self.gs.current.board().is_stalemate()):
            print("stale or check")
            self.gs.current = self.gs.current.parent
        uci_string = self.gs.printer.to_uci(self.gs.current)
        self.engine.uci_send_position(uci_string)
        self.engine.uci_go_movetime(3000)
        self.gs.mode = MODE_GAME_ANALYSIS
        print("tiggered game analysis")

    def on_enter_moves_mode(self):
        # stop any engine
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.give_up.setEnabled(False)
        self.offer_draw.setEnabled(False)
        self.gs.mode = MODE_ENTER_MOVES

    def on_playout_pos(self):
        self.display_info.setChecked(True)
        self.set_display_info()
        self.engine.stop_engine()
        self.engine.start_engine(self.engine_fn)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.give_up.setEnabled(False)
        self.offer_draw.setEnabled(False)
        uci_string = self.gs.printer.to_uci(self.gs.current)
        self.engine.uci_send_position(uci_string)
        self.engine.uci_go_movetime(self.gs.computer_think_time)
        self.gs.mode = MODE_PLAYOUT_POS
        self.enter_moves.setChecked(False)
        #self.analysis.setChecked(False)
        self.play_white.setChecked(False)
        self.play_black.setChecked(False)

    def on_play_as_black(self):
        self.board.flippedBoard = True
        self.board.on_statechanged()
        self.gs.mode = MODE_PLAY_BLACK
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.engine.start_engine(self.engine_fn)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.give_up.setEnabled(True)
        self.offer_draw.setEnabled(True)
        self.engine.uci_strength(self.gs.strength_level)
        print("MOVE : "+str(self.gs.current.board().turn == chess.BLACK))
        if(self.gs.current.board().turn == chess.WHITE):
            print("white to move")
            uci_string = self.gs.printer.to_uci(self.gs.current)
            self.engine.uci_send_position(uci_string)
            self.engine.uci_go_movetime(self.gs.computer_think_time)

    def on_play_as_white(self):
        self.board.flippedBoard = False
        self.board.on_statechanged()
        self.gs.mode = MODE_PLAY_WHITE
        self.engine.stop_engine()
        self.engineOutput.setHtml("")
        self.engine.start_engine(self.engine_fn)
        self.engine.uci_ok()
        self.engine.uci_newgame()
        self.give_up.setEnabled(True)
        self.offer_draw.setEnabled(True)
        self.engine.uci_strength(self.gs.strength_level)
        print("MOVE : "+str(self.gs.current.board().turn))
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

    def give_up_game(self):
        if(self.gs.mode == MODE_PLAY_WHITE):
            self.gs.headers["Result"] = "1-0"
        elif(self.gs.mode == MODE_PLAY_BLACK):
            self.gs.headers["Result"] = "0-1"
        self.enter_moves.setChecked(True)
        self.on_enter_moves_mode()

    def on_checkmate(self):
        if(self.gs.current.board().turn == chess.WHITE):
            self.gs.headers["Result"] = "1-0"
        else:
            self.gs.headers["Result"] = "0-1"
        self.enter_moves.setChecked(True)
        self.on_enter_moves_mode()

    def editGameData(self):
        ed = DialogEditGameData(self.gs.game)
        answer = ed.exec_()
        if(answer):
            root = self.gs
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
        self.setLabels(self.gs.game)

    def draw_game(self):
        self.gs.headers["Result"] = "1/2-1/2"
        self.enter_moves.setChecked(True)
        self.on_enter_moves_mode()

    def handle_offered_draw(self):
        if((self.gs.mode == MODE_PLAY_WHITE and self.gs.score > 1.1)
            or self.gs.mode == MODE_PLAY_BLACK and self.gs.score < -1.1):
            self.gs.headers["Result"] = "1/2-1/2"
            msgBox = QMessageBox()
            msgBox.setText("The computer accepts.")
            msgBox.setInformativeText("The game ends in a draw.")
            msgBox.exec_()
            self.enter_moves.setChecked(True)
            self.on_enter_moves_mode()
        else:
            msgBox = QMessageBox()
            msgBox.setText("The computer rejects your offer.")
            msgBox.setInformativeText("The game continues.")
            msgBox.exec_()

    def on_player_resigns(self):
        msgBox = QMessageBox()
        msgBox.setText("The computer thanks you.")
        msgBox.setInformativeText("Better luck next time!")
        msgBox.exec_()
        if(self.gs.mode == MODE_PLAY_WHITE):
            self.gs.headers["Result"] = "0-1"
        elif(self.gs.mode == MODE_PLAY_BLACK):
            self.gs.headers["Result"] = "1-0"
        self.on_enter_moves_mode()

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
            #self.emit(SIGNAL("statechanged()"))
            self.board.setup_headers(self.gs)
            self.setLabels(self.gs)
            self.board.on_statechanged()
            self.movesEdit.on_statechanged()
            self.update()

    def exists_best_move_info(self):
        return (self.gs.best_score and
                self.gs.best_pv != [])

    def add_variant_from_pv(self,root, uci_list):
        uci_move = uci_list[0]
        print("UCI MOVE:" + str(uci_move))
        root.add_variation(chess.Move.from_uci(uci_move))
        root = root.variations[1]
        for i in range(1,len(uci_list)):
            print("UCI MOVE:" + str(uci_list[i]))
            root.add_main_variation(chess.Move.from_uci(uci_list[i]))
            root = root.variations[0]

    def on_bestmove(self,move):
        print("bestmove received: "+str(move))
        # execute only if engine plays
        if((self.gs.mode == MODE_PLAY_BLACK and self.gs.current.board().turn == chess.WHITE)
            or
            (self.gs.mode == MODE_PLAY_WHITE and self.gs.current.board().turn == chess.BLACK)
            or self.gs.mode == MODE_PLAYOUT_POS):
            # check if game is drawn due to various conditions
            print("executing bestmove received: "+str(move))
            print("BAD: "+str(self.gs.position_bad))
            # check for bad position
            if(self.gs.mode == MODE_PLAY_BLACK):
                if(self.gs.score < -7.0):
                    self.gs.position_bad += 1
                else:
                    self.gs.position_bad = 0
            if(self.gs.mode == MODE_PLAY_WHITE):
                if(self.gs.score > 7.0):
                    self.gs.position_bad += 1
            # check for draw
            if(self.gs.score == 0.0):
                self.gs.position_draw += 1
            else:
                self.gs.position_draw = 0
            game_over = False
            if(not self.gs.mode == MODE_PLAYOUT_POS):
                if(self.gs.position_bad > 5): # due to computer resigning
                    msgBox = QMessageBox()
                    msgBox.setText("The computer resigns.")
                    msgBox.setInformativeText("Congratulations!")
                    msgBox.exec_()
                    self.give_up_game()
                    game_over = True
                elif(self.gs.position_draw > 5): # due to computer offering draw which is accepted
                    msgBox = QMessageBox()
                    msgBox.setText("The computer offers a draw.")
                    msgBox.setInformativeText("Do you accept?")
                    msgBox.setStandardButtons(QMessageBox.No | QMessageBox.Yes)
                    msgBox.setDefaultButton(QMessageBox.Yes)
                    if(msgBox.exec_() == QMessageBox.Yes):
                        game_over = True
                        self.draw_game()
                        print("DRAW?")
                    else:
                        self.gs.position_draw = 0
            if(not (game_over)):
                # continue normal play
                uci = move
                legal_moves = self.gs.current.board().legal_moves
                if (len([x for x in legal_moves if x.uci() == uci]) > 0):
                    self.board.executeMove(move)
                    self.board.on_statechanged()
        threshold = 0.4
        if(self.gs.mode == MODE_GAME_ANALYSIS):
            if(len(self.gs.current.variations) >0 ):
                print("next move:"+self.gs.current.variations[0].move.uci())
                print("pv:"+str(self.gs.pv))
                print("mate thread: "+str(self.gs.mate_threat))

            if(self.exists_best_move_info()
                and ((self.gs.score - self.gs.best_score > threshold)
                    or (self.gs.mate_threat == None and self.gs.next_mate_threat != None))
                and self.gs.current.variations != []
                and self.gs.best_pv[0] != self.gs.current.variations[0].move.uci()):
                    #pass
                    #self.gs.current.variations[0].comment = str(self.gs.best_score)
                    # add
                    #print("next move:"+self.gs.current.variations[0].move.uci())
                    #print("best_pv:"+str(self.gs.best_pv))
                    self.add_variant_from_pv(self.gs.current,self.gs.pv)
                    if(self.gs.next_mate_threat != None):
                        self.gs.current.variations[0].comment = "#"+str(self.gs.next_mate_threat)
                    else:
                        self.gs.current.variations[0].comment = str(self.gs.best_score)

                    self.gs.current.variations[1].comment = str(self.gs.score)
                    #
            self.gs.best_score = self.gs.score
            self.gs.best_pv = self.gs.pv
            self.gs.next_mate_threat = self.gs.mate_threat
            self.gs.mate_threat = None

            self.board.on_statechanged()
            self.movesEdit.on_statechanged()


            if(self.gs.current.parent):
                print("HAS PARENT: MOVING UP")
                self.gs.current = self.gs.current.parent

                # send uci best move command
                self.on_statechanged()
            else:
                print("finished analysing")
                self.gs.mode = MODE_ENTER_MOVES
                pass # (finished, display messagebox)

    """
    def closeEvent(self, event):
        try:
            with open("current.pgn",'w') as f:
                print(self.gs.current.root(), file=f, end="\n\n")
            with open("current.fen",'w') as f:
                print(self.gs.current.board().fen(), file=f)
        except:
            pass
        event.accept()
    """

def we_are_frozen():
    # All of the modules are built-in to the interpreter, e.g., by py2exe
    return hasattr(sys, "frozen")

def module_path():
    encoding = sys.getfilesystemencoding()
    if we_are_frozen():
        return os.path.dirname(sys.executable)
    return os.path.dirname(__file__)

def about_to_quit():
    try:
        if not os.path.exists(main.save_state_dir):
            os.makedirs(main.save_state_dir)
        with open(main.save_state_dir+"/current.raw",'wb') as f:
        #    print(main.gs.current.root(), file=f, end="\n\n")
            pickle.dump(main.gs,f)
        f.close()
        #with open("current.fen",'w') as f:
        #    print(main.gs.current.board().fen(),file=f)
    except BaseException as e:
        print(e)
        pass


sys.setrecursionlimit(3000)

app = QApplication(sys.argv)
main = MainWindow()




app.setActiveWindow(main)
app.aboutToQuit.connect(about_to_quit) # myExitHandler is a callable
main.show()
sys.exit(app.exec_())
