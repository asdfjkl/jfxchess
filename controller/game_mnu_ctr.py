from PyQt4.QtGui import *
from PyQt4.QtCore import *

from dialogs.dialog_strength_level import DialogStrengthLevel
from dialogs.dialog_new_game import DialogNewGame
from dialogs.dialog_analyze_game import DialogAnalyzeGame
from dialogs.dialog_save_changes import DialogSaveChanges
from model.gamestate import GameState
from model.gamestate import MODE_ENTER_MOVES, \
    MODE_PLAY_WHITE, MODE_PLAY_BLACK, MODE_ANALYSIS, \
    MODE_GAME_ANALYSIS, MODE_PLAYOUT_POS
import chess
#from controller.file_menu_ctr import is_position_in_book
from util.messages import display_mbox
from dialogs.dialog_engines import DialogEngines
from uci.engine_info import EngineInfo
import controller.edit_mnu_ctr
import controller.file_mnu_ctr
from dialogs.dialog_edit_game_data import DialogEditGameData

class GameMenuController():

    def __init__(self, mainAppWindow, model):
        super(GameMenuController, self).__init__()
        self.mainAppWindow = mainAppWindow
        self.uci_controller = mainAppWindow.engine_controller
        self.model = model

    def on_newgame(self, mainWindow):
        settings = mainWindow.model.user_settings
        db = mainWindow.model.database
        ret = self.unsaved_changes(mainWindow)
        if not ret == QMessageBox.Cancel:
            dialog = DialogNewGame(gamestate=mainWindow.gs,user_settings=settings)
            movesEdit = mainWindow.moves_edit_view
            if dialog.exec_() == QDialog.Accepted:
                mainWindow.model.gamestate = GameState()
                mainWindow.chessboard_view.gs = mainWindow.model.gamestate
                movesEdit.gs = mainWindow.gs
                movesEdit.update()
                # strength is only changed if internal engine is used
                # otherwise the dialog is meaningless
                if(settings.active_engine == settings.engines[0]):
                    mainWindow.model.gamestate.strength_level = dialog.slider_elo.value()
                mainWindow.model.gamestate.computer_think_time = dialog.think_ms
                #print("think time: "+str(mainWindow.gs.computer_think_time))
                movesEdit.on_statechanged()
                mainWindow.model.gamestate.initialize_headers()
                #mainWindow.save.setEnabled(False)
                # add current game to database, but don't save it
                mainWindow.model.database.index_current_game = None
                mainWindow.save.setEnabled(True)
                #mainWindow.database.add_current_game(mainWindow.gs.game.root())
                if(dialog.rb_plays_white.isChecked()):
                    mainWindow.play_white.setChecked(True)
                    mainWindow.setLabels()
                    self.on_play_as_white(mainWindow)
                else:
                    mainWindow.play_black.setChecked(True)
                    root = mainWindow.model.gamestate.current.root()
                    temp = root.headers["White"]
                    root.headers["White"] = root.headers["Black"]
                    root.headers["Black"] = temp
                    mainWindow.setLabels()
                    self.on_play_as_black(mainWindow)

    def save(self):
        mainWidget = self.mainAppWindow
        db = mainWidget.model.database
        # if the game is not in the database
        # i.e. hasn't been saved yet, then
        # calls save_as
        if(db.index_current_game == None):
            self.save_as_new()
        else:
            db.update_game(db.index_current_game,mainWidget.model.gamestate.current)
        mainWidget.save.setEnabled(False)
        mainWidget.moves_edit_view.setFocus()

    def save_as_new(self):
        mainWidget = self.mainAppWindow
        db = mainWidget.model.database
        gs = mainWidget.model.gamestate
        # let the user enter game data
        controller.edit.editGameData(mainWidget)
        db.append_game(gs.current)
        mainWidget.save.setEnabled(False)
        mainWidget.moves_edit_view.setFocus()

    def export_game(self):
        mainWidget = self.mainAppWindow
        gamestate = mainWidget.gs
        dialog = QFileDialog()
        if(gamestate.last_save_dir != None):
            dialog.setDirectory(gamestate.last_save_dir)
        filename = dialog.getSaveFileName(mainWidget, mainWidget.trUtf8('Save PGN'), None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
        if(filename):
            if(not filename.endswith(".pgn")):
                filename = filename + ".pgn"
            f = open(filename,'w')
            print(gamestate.current.root(), file=f, end="\n\n")
            gamestate.pgn_filename = filename
            #mainWidget.save_game.setEnabled(True)
            mainWidget.movesEdit.setFocus()
            f.close()
            gamestate.last_save_dir = QFileInfo(filename).dir().absolutePath()

    def editGameData(self):
        pass
        # todo call common gamestate functionality

    def on_nextgame(self, mainWindow):
        db = mainWindow.model.database
        gs = mainWindow.model.gamestate
        cbv = mainWindow.chessboard_view
        if(not db.index_current_game == None):
            if(db.index_current_game < len(db.entries)-1):
                ret = self.unsaved_changes(mainWindow)
                if not ret == QMessageBox.Cancel:
                    loaded_game = db.load_game(db.index_current_game+1)
                    gs.current = loaded_game
                    cbv.update()
                    cbv.emit(SIGNAL("statechanged()"))
                    gs.unsaved_changes = False
                    mainWindow.save.setEnabled(False)
                    mainWindow.setLabels()
                    mainWindow.moves_edit_view.setFocus()
                    #controller.file_mnu_ctr.init_game_tree(mainWindow,gs.current.root())
                    gs.init_game_tree(mainWindow)


    def on_previous_game(self, mainWindow):
        db = mainWindow.model.database
        gs = mainWindow.model.gamestate
        cbv = mainWindow.chessboard_view
        if(not db.index_current_game == None):
            if(db.index_current_game > 0):
                ret = self.unsaved_changes(mainWindow)
                if not ret == QMessageBox.Cancel:
                    loaded_game = db.load_game(db.index_current_game-1)
                    gs.current = loaded_game
                    cbv.update()
                    cbv.emit(SIGNAL("statechanged()"))
                    gs.unsaved_changes = False
                    mainWindow.save.setEnabled(False)
                    mainWindow.setLabels()
                    mainWindow.moves_edit_view.setFocus()
                    gs.init_game_tree(mainWindow)



###########







    def on_unsaved_changes(self):
        self.mainAppWindow.model.gamestate.unsaved_changes = True
        self.mainAppWindow.save.setEnabled(True)
