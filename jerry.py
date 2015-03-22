#!/usr/bin/python

# classes from Jerry:
from gui.MovesEdit import MovesEdit
from gui.ChessBoardView import ChessboardView
from logic import file_io
from logic import edit
from logic import gameevents
from logic.gamestate import GameState
from logic.gamestate import MODE_ENTER_MOVES
from dialogs.DialogAbout import DialogAbout
from uci.uci_controller import Uci_controller
import gc

# PyQt and python system functions / external libs
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from functools import partial
import pickle
from util.appdirs import *
from util.messages import display_mbox

class MainWindow(QMainWindow):
    def __init__(self):
        QMainWindow.__init__(self)

        self.resize(800, 470)
        self.setWindowTitle('Jerry - Chess')
        self.centerOnScreen()

        print("jerry id:"+str(QCoreApplication.applicationPid()))


        exit = QAction(QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip('Exit application')
        self.connect(exit, SIGNAL('triggered()'), SLOT('close()'))

        self.gs = GameState()
        self.gs.mode = MODE_ENTER_MOVES
        self.engine = Uci_controller()
        self.engine_fn = os.path.dirname(os.path.realpath(sys.argv[0]))
        #display_mbox("filename",self.engine_fn)
        # get filename of engine depending on os
        if sys.platform == 'win32':
            self.engine_fn += "/engine/stockfish.exe"
        elif 'linux' in  sys.platform:
            self.engine_fn += "/engine/stockfish_linux"
        elif sys.platform == 'darwin':
            self.engine_fn += '/engine/stockfish_osx'
        self.engine_fn = '"'+self.engine_fn+'"'

        # if existing, recover game state that user was in
        # before existing game the last time (by unpickling)
        appname = 'jerry'
        appauthor = 'dkl'
        fn = user_data_dir(appname, appauthor)
        self.save_state_dir = fn
        try:
            with open(fn+"/current.raw","rb") as pgn:
                self.gs = pickle.load(pgn)
                self.gs.mode = MODE_ENTER_MOVES
            pgn.close()
        except BaseException as e:
            print(e)
            pass

        self.board = ChessboardView(self.gs,self.engine)
        self.board.on_statechanged()

        self.movesEdit = MovesEdit(self.gs)
        self.movesEdit.setReadOnly(True)
        self.movesEdit.on_statechanged()

        # setup the main window
        spLeft = QSizePolicy();
        spLeft.setHorizontalStretch(1);
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
        statusbar.showMessage('Jerry v1.01')

        # set labels of game (i.e. player etc.) above move editing panel
        self.setLabels()

        # create the main menu bar, and connect slots for actions
        self.menubar = self.menuBar()

        # FILE MENU
        m_file = self.menuBar().addMenu('File ')
        new_game = m_file.addAction('New Game')
        new_game.setShortcut(QKeySequence.New)
        new_game.triggered.connect(partial(gameevents.on_newgame,self))
        m_file.addSeparator()
        load_game = m_file.addAction("Open Game")
        load_game.setShortcut(QKeySequence.Open)
        load_game.triggered.connect(partial(file_io.open_pgn, self))
        self.save_game = m_file.addAction("Save")
        self.save_game.setShortcut(QKeySequence.Save)
        self.save_game.triggered.connect(partial(file_io.save_to_pgn,self))
        if(self.gs.pgn_filename == None):
            self.save_game.setEnabled(False)
        save_game_as = m_file.addAction("Save as ...")
        save_game_as.setShortcut(QKeySequence.SaveAs)
        save_game_as.triggered.connect(partial(file_io.save_as_to_pgn,self))
        append_game = m_file.addAction("Append to ...")
        #append_game.setShortcut(QKeySequence.SaveAs)
        append_game.triggered.connect(partial(file_io.append_to_pgn,self))
        m_file.addSeparator()
        save_diag = m_file.addAction("Save Position as Image")
        save_diag.triggered.connect(partial(file_io.save_image,self))
        m_file.addSeparator()
        print_game = m_file.addAction("Print Game")
        print_game.triggered.connect(partial(file_io.print_game,self.gs))
        print_game.setShortcut(QKeySequence.Print)
        print_pos = m_file.addAction("Print Position")
        print_pos.triggered.connect(partial(file_io.print_position,self))
        m_file.addSeparator()
        exit_item = m_file.addAction("Quit")
        exit_item.setShortcut(QKeySequence.Quit)
        exit_item.triggered.connect(QApplication.quit)

        # EDIT MENU
        m_edit = self.menuBar().addMenu('Edit ')
        copy_game = m_edit.addAction("Copy Game")
        copy_game.triggered.connect(partial(edit.game_to_clipboard,self.gs))
        copy_game.setShortcut(QKeySequence.Copy)
        copy_pos = m_edit.addAction("Copy Position")
        copy_pos.triggered.connect(partial(edit.pos_to_clipboard,self.gs))
        paste = m_edit.addAction("Paste")
        paste.setShortcut(QKeySequence.Paste)
        paste.triggered.connect(partial(edit.from_clipboard,self))
        m_edit.addSeparator()
        enter_pos = m_edit.addAction("&Enter Position")
        enter_pos.setShortcut('e')
        enter_pos.triggered.connect(partial(edit.enter_position,self))
        m_edit.addSeparator()
        edit_game_data = m_edit.addAction("Edit Game Data")
        edit_game_data.triggered.connect(partial(edit.editGameData,self))
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
        self.offer_draw.triggered.connect(partial(gameevents.handle_offered_draw,self))
        self.offer_draw.setEnabled(False)
        self.give_up = m_edit.addAction("Resign")
        self.give_up.triggered.connect(partial(gameevents.on_player_resigns,self))
        self.give_up.setEnabled(False)
        m_edit.addSeparator()

        # MODE MENU
        m_mode = self.menuBar().addMenu("Mode")
        ag = QActionGroup(self, exclusive=True)
        analysis = QAction("&Analysis Mode",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(analysis))
        analysis.setShortcut('a')
        analysis.triggered.connect(partial(gameevents.on_analysis_mode,self))
        self.play_white = QAction("Play as &White",m_mode,checkable=True)
        self.play_white.setShortcut('w')
        m_mode.addAction(ag.addAction(self.play_white))
        self.play_white.triggered.connect(partial(gameevents.on_play_as_white,self))

        self.play_black = QAction("Play as &Black",m_mode,checkable=True)
        self.play_black.setShortcut('b')
        m_mode.addAction(ag.addAction(self.play_black))
        self.play_black.triggered.connect(partial(gameevents.on_play_as_black,self))

        self.enter_moves = QAction("Enter &Moves",m_mode,checkable=True)
        self.enter_moves.setShortcuts([Qt.Key_M,Qt.Key_Escape])
        m_mode.addAction(ag.addAction(self.enter_moves))
        self.enter_moves.triggered.connect(partial(gameevents.on_enter_moves_mode,self))
        self.enter_moves.setChecked(True)
        m_mode.addSeparator()

        self.analyze_game = QAction("Full Game Analysis",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(self.analyze_game))
        self.analyze_game.triggered.connect(partial(gameevents.on_game_analysis_mode,self))
        self.play_out_pos = QAction("Play out Position",m_mode,checkable=True)
        m_mode.addAction(ag.addAction(self.play_out_pos))
        self.play_out_pos.triggered.connect(partial(gameevents.on_playout_pos,self))
        m_mode.addSeparator()

        set_strength = m_mode.addAction("Strength Level")
        set_strength.triggered.connect(partial(gameevents.on_strength_level,self))


        # HELP MENU
        m_help = self.menuBar().addMenu("Help")
        about = m_help.addAction("About")
        about.triggered.connect(self.show_about)
        m_help.addSeparator()
        # self.connect(action2, QtCore.SIGNAL('triggered()'), QtCore.SLOT(board.flip_board()))

        self.update_info_ok = False
        self.state_changed = False
        self.timer_update_info = QTimer()
        self.timer_update_info.timeout.connect(self.set_timer)
        self.timer_update_info.start(600)
        self.state_changed_timer = QTimer()

        self.connect(self.engine, SIGNAL("updateinfo(QString)"),partial(gameevents.receive_engine_info,self))
        self.connect(self.movesEdit, SIGNAL("statechanged()"),self.board.on_statechanged)
        self.connect(self.movesEdit, SIGNAL("statechanged()"),partial(gameevents.on_statechanged,self))
        self.connect(self.board, SIGNAL("statechanged()"),self.movesEdit.on_statechanged)
        self.connect(self.board, SIGNAL("bestmove(QString)"),partial(gameevents.on_bestmove,self))
        self.connect(self.engine, SIGNAL("bestmove(QString)"),partial(gameevents.on_bestmove,self))
        self.connect(self.board,SIGNAL("drawn"),partial(gameevents.draw_game,self))
        self.connect(self.board,SIGNAL("checkmate"),partial(gameevents.on_checkmate,self))

        #self.engine.start_engine(self.engine_fn)
        #self.engine.uci_go_infinite()

    def set_timer(self):
        #print("resetting timer")
        self.update_info_ok = True

    def set_timer2(self):
        #print("state change timer over")
        self.state_changed = False
        #self.state_changed_timer.stop()

    def set_display_info(self):
        if(self.display_info.isChecked()):
            self.gs.display_engine_info = True
        else:
            self.gs.display_engine_info = False
            self.engineOutput.setHtml("")

    def show_about(self):
        d = DialogAbout()
        d.exec_()

    def centerOnScreen (self):
        resolution = QDesktopWidget().screenGeometry()
        self.move((resolution.width() / 2) - (self.frameSize().width() / 2),
                  (resolution.height() / 2) - (self.frameSize().height()*2 / 3))

    def setLabels(self):
        game = self.gs.current.root()
        self.name.setText("<b>"+game.headers["White"]+ " - "+
                      game.headers["Black"]+"</b><br>"+
                      game.headers["Site"]+ " "+
                      game.headers["Date"])

    def closeEvent(self, event):
        self.board.engine.stop_engine()




def we_are_frozen():
    # All of the modules are built-in to the interpreter, e.g., by py2exe, py2app...
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


#gc.disable()

sys.setrecursionlimit(3000)

app = QApplication(sys.argv)
main = MainWindow()

# set app icon
app_icon = QIcon()
app_icon.addFile('res/icons_taskbar/icon16.png', QSize(16,16))
app_icon.addFile('res/icons_taskbar/icon24.png', QSize(24,24))
app_icon.addFile('res/icons_taskbar/icon32.png', QSize(32,32))
app_icon.addFile('res/icons/taskbar/icon48.png', QSize(48,48))
app_icon.addFile('res/icons_taskbar/icon256.png',QSize(256,256))
app.setWindowIcon(app_icon)


app.setActiveWindow(main)
app.aboutToQuit.connect(about_to_quit) # myExitHandler is a callable
main.show()
#main.setFocus()
sys.exit(app.exec_())
