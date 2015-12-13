#!/usr/bin/python3

# classes from Jerry:
from views.moves_edit_view import MovesEditView
from views.chessboard_view import ChessboardView
from dialogs.dialog_about import DialogAbout
from uci.uci_controller import Uci_controller
from model.model import Model
from controller.gamestate_ctr import GamestateController
from controller.edit_mnu_ctr import EditMenuController
from controller.file_mnu_ctr import FileMenuController
from controller.game_mnu_ctr import GameMenuController
from controller.mode_mnu_ctr import ModeMenuController

# PyQt and python system functions / external libs
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *

import time

class MainWindow(QMainWindow):
    def __init__(self):
        QMainWindow.__init__(self)

        self.resize(800, 470)
        self.setWindowTitle('Jerry - Chess')
        self.centerOnScreen()

        exit = QAction(QIcon('icons/exit.png'), 'Exit', self)
        exit.setShortcut('Ctrl+Q')
        exit.setStatusTip(self.trUtf8('Exit application'))
        self.connect(exit, SIGNAL('triggered()'), SLOT('close()'))

        self.engine_controller = Uci_controller()
        """
        self.engine_fn = os.path.dirname(os.path.realpath(sys.argv[0]))
        # get filename of engine depending on os
        if sys.platform == 'win32':
            self.engine_fn += "/engine/stockfish.exe"
        elif 'linux' in  sys.platform:
            self.engine_fn += "/engine/stockfish_linux"
        elif sys.platform == 'darwin':
            self.engine_fn += '/engine/stockfish_osx'
        self.engine_fn = '"'+self.engine_fn+'"'
        """

        self.model = Model.create_on_startup(self)

        self.chessboard_view = ChessboardView(self.model.gamestate,self.engine_controller)
        self.chessboard_view.on_statechanged()

        self.moves_edit_view = MovesEditView(self.model.gamestate)
        self.moves_edit_view.setReadOnly(True)
        self.moves_edit_view.on_statechanged()


        # setup the main window
        # consisting of:
        #
        # <-------menubar---------------------------->
        # <chess-     ->   <moves_edit_view---------->
        # <boardview  ->   <engine output (Textedit)->
        #
        spLeft = QSizePolicy();
        spLeft.setHorizontalStretch(1);
        mainWidget = QWidget()
        hbox = QHBoxLayout()
        hbox.addWidget(self.chessboard_view)
        spRight = QSizePolicy()
        spRight.setHorizontalStretch(2)
        vbox = QVBoxLayout()
        self.name = QLabel()
        self.name.setAlignment(Qt.AlignCenter)
        self.name.setBuddy(self.moves_edit_view)
        vbox.addWidget(self.name)
        vbox.addWidget(self.moves_edit_view)
        self.chessboard_view.movesEdit = self.moves_edit_view
        self.engineOutput = QTextEdit()
        self.engineOutput.setReadOnly(True)
        self.engineOutput.setFocusPolicy(Qt.NoFocus)
        vbox.addWidget(self.engineOutput)
        hbox.addLayout(vbox)
        mainWidget.setLayout(hbox)
        self.setCentralWidget(mainWidget)
        statusbar = self.statusBar()
        statusbar.showMessage('Jerry v2.0')

        # set labels of game (i.e. player etc.) above move editing panel
        self.setLabels()

        # create the main menu bar, and connect slots for actions
        self.menubar = self.menuBar()

        # the gamestate controller is the controller that
        # manages all functionality w.r.t. state changes
        #
        # other controllers get a reference of this
        # controller and make user of this functionality
        self.gamestateController = GamestateController(self)

        # FILE MENU
        self.fileMenuController = FileMenuController(self)
        m_file = self.menuBar().addMenu(self.trUtf8('File '))
        new_db = m_file.addAction(self.trUtf8("New..."))
        new_db.triggered.connect(self.fileMenuController.new_database)
        open_db = m_file.addAction(self.trUtf8("Open..."))
        open_db.setShortcut(QKeySequence.Open)
        open_db.triggered.connect(self.fileMenuController.open_database)
        browse_games = m_file.addAction(self.trUtf8("Browse Games..."))
        browse_games.setShortcut('L')
        browse_games.triggered.connect(self.fileMenuController.browse_games)
        m_file.addSeparator()
        save_diag = m_file.addAction(self.trUtf8("Save Position as Image"))
        save_diag.triggered.connect(self.fileMenuController.save_image)
        m_file.addSeparator()
        print_game = m_file.addAction(self.trUtf8("Print Game"))
        print_game.triggered.connect(self.fileMenuController.print_game)
        print_game.setShortcut(QKeySequence.Print)
        print_pos = m_file.addAction(self.trUtf8("Print Position"))
        print_pos.triggered.connect(self.fileMenuController.print_position)
        m_file.addSeparator()
        exit_item = m_file.addAction(self.trUtf8("Quit"))
        exit_item.setShortcut(QKeySequence.Quit)
        exit_item.triggered.connect(self.close)

        # EDIT MENU
        self.editMenuController = EditMenuController(self)
        m_edit = self.menuBar().addMenu(self.trUtf8('Edit '))
        copy_game = m_edit.addAction(self.trUtf8("Copy Game"))
        copy_game.triggered.connect(self.editMenuController.copy_game_to_clipboard)
        copy_game.setShortcut(QKeySequence.Copy)
        copy_pos = m_edit.addAction(self.trUtf8("Copy Position"))
        copy_pos.triggered.connect(self.editMenuController.copy_pos_to_clipboard)
        paste = m_edit.addAction(self.trUtf8("Paste"))
        paste.setShortcut(QKeySequence.Paste)
        paste.triggered.connect(self.editMenuController.paste_from_clipboard)
        m_edit.addSeparator()
        enter_pos = m_edit.addAction(self.trUtf8("&Enter Position"))
        enter_pos.setShortcut('e')
        enter_pos.triggered.connect(self.editMenuController.enter_position)
        reset_pos = m_edit.addAction(self.trUtf8("Reset to Initial"))
        reset_pos.triggered.connect(self.editMenuController.reset_to_initial)
        m_edit.addSeparator()
        flip = m_edit.addAction(self.trUtf8("&Flip Board"))
        flip.setShortcut('f')
        flip.triggered.connect(self.chessboard_view.flip_board)
        self.display_info = QAction(self.trUtf8("Show Search &Info"),m_edit,checkable=True)
        self.display_info.setShortcut('i')
        m_edit.addAction(self.display_info)
        self.display_info.setChecked(True)
        self.display_info.triggered.connect(self.set_display_info)
        m_edit.addSeparator()
        self.offer_draw = m_edit.addAction(self.trUtf8("Offer Draw"))
        self.offer_draw.triggered.connect(self.gamestateController.handle_offered_draw)
        self.offer_draw.setEnabled(False)
        self.give_up = m_edit.addAction(self.trUtf8("Resign"))
        self.give_up.triggered.connect(self.gamestateController.on_player_resigns)
        self.give_up.setEnabled(False)
        m_edit.addSeparator()

        # GAME MENU
        self.gameMenuController = GameMenuController(self)
        m_game = self.menuBar().addMenu(self.trUtf8('Game '))
        new_game = m_game.addAction(self.trUtf8('New Game'))
        new_game.setShortcut(QKeySequence.New)
        new_game.triggered.connect(self.gameMenuController.on_newgame)
        self.save = m_game.addAction(self.trUtf8("Save"))
        self.save.triggered.connect(self.gameMenuController.save)
        if(not self.model.gamestate.unsaved_changes):
            self.save.setEnabled(False)
        self.save_as = m_game.addAction(self.trUtf8("Save As New..."))
        self.save_as.triggered.connect(self.gameMenuController.save_as_new)
        export_game = m_game.addAction(self.trUtf8("Export..."))
        export_game.triggered.connect(self.gameMenuController.export_game)
        m_game.addSeparator()
        edit_game_data = m_game.addAction(self.trUtf8("Edit Game Data"))
        edit_game_data.triggered.connect(self.gameMenuController.editGameData)
        m_game.addSeparator()
        next_ = m_game.addAction(self.trUtf8("Next in Database"))
        next_.triggered.connect(self.gameMenuController.on_nextgame)
        prev_ = m_game.addAction(self.trUtf8("Previous in Database"))
        prev_.triggered.connect(self.gameMenuController.on_previous_game)


        # MODE MENU
        self.modeMenuController = ModeMenuController(self)
        m_mode = self.menuBar().addMenu(self.trUtf8("Mode"))
        ag = QActionGroup(self, exclusive=True)
        analysis = QAction(self.trUtf8("&Analysis Mode"),m_mode,checkable=True)
        m_mode.addAction(ag.addAction(analysis))
        analysis.setShortcut('a')
        analysis.triggered.connect(self.modeMenuController.on_analysis_mode)
        self.play_white = QAction(self.trUtf8("Play as &White"),m_mode,checkable=True)
        self.play_white.setShortcut('w')
        m_mode.addAction(ag.addAction(self.play_white))
        self.play_white.triggered.connect(self.modeMenuController.on_play_as_white)

        self.play_black = QAction(self.trUtf8("Play as &Black"),m_mode,checkable=True)
        self.play_black.setShortcut('b')
        m_mode.addAction(ag.addAction(self.play_black))
        self.play_black.triggered.connect(self.modeMenuController.on_play_as_black)

        self.enter_moves = QAction(self.trUtf8("Enter &Moves"),m_mode,checkable=True)
        self.enter_moves.setShortcuts([Qt.Key_M,Qt.Key_Escape])
        m_mode.addAction(ag.addAction(self.enter_moves))
        self.enter_moves.triggered.connect(self.modeMenuController.on_enter_moves_mode)
        self.enter_moves.setChecked(True)
        m_mode.addSeparator()

        self.analyze_game = QAction(self.trUtf8("Full Game Analysis"),m_mode,checkable=True)
        m_mode.addAction(ag.addAction(self.analyze_game))
        self.analyze_game.triggered.connect(self.modeMenuController.on_game_analysis_mode,)
        self.play_out_pos = QAction(self.trUtf8("Play out Position"),m_mode,checkable=True)
        m_mode.addAction(ag.addAction(self.play_out_pos))
        self.play_out_pos.triggered.connect(self.modeMenuController.on_playout_pos)
        m_mode.addSeparator()

        set_strength = m_mode.addAction(self.trUtf8("Strength Level"))
        set_strength.triggered.connect(self.modeMenuController.on_strength_level)

        set_engines = m_mode.addAction(self.trUtf8("Engines..."))
        set_engines.triggered.connect(self.modeMenuController.on_set_engines)

        # HELP MENU
        m_help = self.menuBar().addMenu((self.trUtf8("Help")))
        about = m_help.addAction(self.trUtf8("About"))
        about.setMenuRole(QAction.AboutRole)
        about.triggered.connect(self.show_about)
        homepage = m_help.addAction(self.trUtf8(("Jerry-Homepage")))
        homepage.triggered.connect(self.go_to_homepage)

        self.connect(self.engine_controller, SIGNAL("updateinfo(PyQt_PyObject)"),self.gamestateController.receive_engine_info)
        self.connect(self.moves_edit_view, SIGNAL("statechanged()"),self.chessboard_view.on_statechanged)
        self.connect(self.moves_edit_view, SIGNAL("statechanged()"),self.gamestateController.on_statechanged)
        self.connect(self.moves_edit_view, SIGNAL("unsaved_changes()"),self.gameMenuController.on_unsaved_changes)
        self.connect(self.chessboard_view, SIGNAL("unsaved_changes()"),self.gameMenuController.on_unsaved_changes)
        self.connect(self.chessboard_view, SIGNAL("statechanged()"),self.moves_edit_view.on_statechanged)
        self.connect(self.chessboard_view, SIGNAL("bestmove(QString)"),self.gamestateController.on_bestmove)
        self.connect(self.engine_controller, SIGNAL("bestmove(QString)"),self.gamestateController.on_bestmove)
        self.connect(self.chessboard_view,SIGNAL("drawn"),self.gamestateController.draw_game)
        self.connect(self.chessboard_view,SIGNAL("checkmate"),self.gamestateController.on_checkmate)


    def set_display_info(self):
        if(self.display_info.isChecked()):
            self.model.gamestate.display_engine_info = True
        else:
            self.model.gamestate.display_engine_info = False
            self.engineOutput.setHtml("")

    def show_about(self):
        d = DialogAbout()
        d.exec_()

    def go_to_homepage(self):
        QDesktopServices.openUrl(QUrl("http://asdfjkl.github.io/jerry/"))

    def centerOnScreen (self):
        resolution = QDesktopWidget().screenGeometry()
        self.move((resolution.width() / 2) - (self.frameSize().width() / 2),
                  (resolution.height() / 2) - (self.frameSize().height()*2 / 3))

    def setLabels(self):
        game = self.model.gamestate.current.root()
        self.name.setText("<b>"+game.headers["White"]+ " - "+
                      game.headers["Black"]+"</b><br>"+
                      game.headers["Site"]+ " "+
                      game.headers["Date"])

    def closeEvent(self, event):
        # print("received close event")
        self.engine_controller.kill_engine()
        self.engine_controller.thread.exit()
        self.engine_controller.thread.wait()
