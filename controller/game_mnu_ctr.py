from PyQt4.QtGui import *
from PyQt4.QtCore import *

from dialogs.dialog_new_game import DialogNewGame
from model.gamestate import GameState

class GameMenuController():

    def __init__(self, mainAppWindow):
        super(GameMenuController, self).__init__()
        self.mainAppWindow = mainAppWindow
        self.model = mainAppWindow.model

    def on_newgame(self):
        ret = self.mainAppWindow.gamestateController.unsaved_changes()
        if not ret == QMessageBox.Cancel:
            dialog = DialogNewGame(gamestate=self.model.gamestate,user_settings=self.model.user_settings)
            if dialog.exec_() == QDialog.Accepted:
                self.model.gamestate = GameState()
                self.mainAppWindow.chessboard_view.gs = self.model.gamestate
                self.mainAppWindow.moves_edit_view.gs = self.model.gamestate
                self.mainAppWindow.moves_edit_view.update()
                # strength is only changed if internal engine is used
                # otherwise the dialog is meaningless
                if(self.model.user_settings.active_engine == self.model.user_settings.engines[0]):
                    self.model.gamestate.strength_level = dialog.slider_elo.value()
                self.model.gamestate.computer_think_time = dialog.think_ms
                self.mainAppWindow.moves_edit_view.on_statechanged()
                self.model.gamestate.initialize_headers()
                self.model.database.index_current_game = None
                self.mainAppWindow.save.setEnabled(True)
                if(dialog.rb_plays_white.isChecked()):
                    self.mainAppWindow.play_white.setChecked(True)
                    self.mainAppWindow.setLabels()
                    self.mainAppWindow.modeMenuController.on_play_as_white()
                else:
                    self.mainAppWindow.play_black.setChecked(True)
                    root = self.model.gamestate.current.root()
                    temp = root.headers["White"]
                    root.headers["White"] = root.headers["Black"]
                    root.headers["Black"] = temp
                    self.mainAppWindow.setLabels()
                    self.mainAppWindow.modeMenuController.on_play_as_black()

    def save(self):
        self.model.database.reload_if_necessary(self.mainAppWindow)
        # if the game is not in the database
        # i.e. hasn't been saved yet, then
        # calls save_as
        if(self.model.database.index_current_game == None):
            self.save_as_new()
        else:
            self.model.database.update_game(self.model.database.index_current_game,self.model.gamestate.current)
        self.mainAppWindow.save.setEnabled(False)
        self.mainAppWindow.moves_edit_view.setFocus()

    def save_as_new(self):
        self.model.database.reload_if_necessary(self.mainAppWindow)
        # let the user enter game data
        self.mainAppWindow.gamestateController.editGameData()
        # then save to db
        self.model.database.append_game(self.model.gamestate.current)
        self.mainAppWindow.save.setEnabled(False)
        self.model.gamestate.unsaved_changes = False
        self.mainAppWindow.moves_edit_view.setFocus()

    def export_game(self):
        dialog = QFileDialog()
        if(self.model.gamestate.last_save_dir != None):
            dialog.setDirectory(self.model.gamestate.last_save_dir)
        filename = dialog.getSaveFileName(self.mainAppWindow, self.mainAppWindow.trUtf8('Save PGN'), \
                                          None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
        if(filename):
            if(not filename.endswith(".pgn")):
                filename = filename + ".pgn"
            f = open(filename,'w')
            print(self.model.gamestate.current.root(), file=f, end="\n\n")
            self.model.gamestate.pgn_filename = filename
            self.mainAppWindow.movesEdit.setFocus()
            f.close()
            self.model.gamestate.last_save_dir = QFileInfo(filename).dir().absolutePath()

    def editGameData(self):
        # just pass directly to gamestate controller
        self.mainAppWindow.gamestateController.editGameData()

    def on_nextgame(self):
        if(not self.model.database.index_current_game == None):
            if(self.model.database.index_current_game < len(self.model.database.entries)-1):
                ret = self.mainAppWindow.gamestateController.unsaved_changes()
                #ret = QMessageBox.Ok
                if not ret == QMessageBox.Cancel:
                    loaded_game = self.model.database.load_game(self.model.database.index_current_game+1)
                    self.model.gamestate.current = loaded_game
                    self.mainAppWindow.chessboard_view.update()
                    self.mainAppWindow.chessboard_view.emit(SIGNAL("statechanged()"))
                    self.model.gamestate.unsaved_changes = False
                    self.mainAppWindow.save.setEnabled(False)
                    self.mainAppWindow.setLabels()
                    self.mainAppWindow.moves_edit_view.setFocus()
                    self.model.gamestate.init_game_tree(self.mainAppWindow)

    def on_previous_game(self):
        if(not self.model.database.index_current_game == None):
            if(self.model.database.index_current_game > 0):
                ret = self.mainAppWindow.gamestateController.unsaved_changes()
                #ret = QMessageBox.Ok
                if not ret == QMessageBox.Cancel:
                    loaded_game = self.model.database.load_game(self.model.database.index_current_game-1)
                    self.model.gamestate.current = loaded_game
                    self.mainAppWindow.chessboard_view.update()
                    self.mainAppWindow.chessboard_view.emit(SIGNAL("statechanged()"))
                    self.model.gamestate.unsaved_changes = False
                    self.mainAppWindow.save.setEnabled(False)
                    self.mainAppWindow.setLabels()
                    self.mainAppWindow.moves_edit_view.setFocus()
                    self.model.gamestate.init_game_tree(self.mainAppWindow)

###########

    def on_unsaved_changes(self):
        print("received unsaved changes event")
        self.mainAppWindow.model.gamestate.unsaved_changes = True
        self.mainAppWindow.save.setEnabled(True)
