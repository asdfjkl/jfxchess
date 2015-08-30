from PyQt4.QtGui import *
from PyQt4.QtCore import *
from chess.polyglot import *
from dialogs.dialog_browse_pgn import DialogBrowsePgn
from model.database import Database
import model.gamestate
from dialogs.dialog_save_changes import DialogSaveChanges

class FileMenuController():

    def __init__(self, mainAppWindow):
        super(FileMenuController, self).__init__()
        self.mainAppWindow = mainAppWindow
        self.model = mainAppWindow.model

    def new_database(self):
        ret = self.mainAppWindow.gamestateController.unsaved_changes()
        if(not ret == QMessageBox.Cancel):
            file_dialog = QFileDialog()
            filename = file_dialog.getSaveFileName(self.mainAppWindow, self.mainAppWindow.trUtf8('Create New PGN'), \
                                                   None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
            if(filename):
                if(not filename.endswith(".pgn")):
                    filename = filename + ".pgn"
                self.model.gamestate.last_save_dir = QFileInfo(filename).dir().absolutePath()
                db = Database(filename)
                db.create_new_pgn()
                self.mainAppWindow.save.setEnabled(False)
                self.model.database = db
                self.model.user_settings.active_database = db.filename
        self.mainAppWindow.moves_edit_view.setFocus()


    def open_database(self):
        dialog = QFileDialog()
        if(self.model.gamestate.last_open_dir != None):
            dialog.setDirectory(self.model.gamestate.last_open_dir)
        filename = dialog.getOpenFileName(self.mainAppWindow.chessboard_view, self.mainAppWindow.trUtf8('Open PGN'), \
                                          None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
        if filename:
            db = Database(filename)
            db.init_from_file(self.mainAppWindow,self.mainAppWindow.trUtf8("Scanning PGN File..."))
            self.model.database = db
            self.model.user_settings.active_database = db.filename
            selectedGame = 0
            if(db.no_of_games() > 1):
                dlg = DialogBrowsePgn(db)
                if dlg.exec_() == QDialog.Accepted:
                    items = dlg.table.selectedItems()
                    selectedGame = int(items[0].text())-1
                else:
                    selectedGame = None
            if(not selectedGame == None and db.no_of_games() > 0):
                loaded_game = db.load_game(selectedGame)
                self.model.gamestate.current = loaded_game
                self.mainAppWindow.chessboard_view.update()
                self.mainAppWindow.chessboard_view.emit(SIGNAL("statechanged()"))
                self.mainAppWindow.save.setEnabled(False)
                self.mainAppWindow.setLabels()
                self.mainAppWindow.moves_edit_view.setFocus()
                self.model.gamestate.last_open_dir = QFileInfo(filename).dir().absolutePath()
                self.model.gamestate.init_game_tree(self.mainAppWindow)
        self.mainAppWindow.moves_edit_view.setFocus()


    def browse_games(self):
        mainWindow = self.mainAppWindow
        selectedGame = 0
        mainWindow.model.gamestate.mode = model.gamestate.MODE_ENTER_MOVES
        dlg = DialogBrowsePgn(self.model.database)
        if dlg.exec_() == QDialog.Accepted:
            items = dlg.table.selectedItems()
            selectedGame = int(items[0].text())-1
            loaded_game = self.model.database.load_game(selectedGame)
            #print("loaded game: "+str(loaded_game))
            if(not loaded_game == None):
                # if the user wants to load a game, but the current open
                # game has still unsaved changes or hasn't been saved at all,
                # ask user what to do
                cancel = False
                if(self.model.database.index_current_game == None or self.model.gamestate.unsaved_changes):
                    changes_dialog = DialogSaveChanges()
                    ret = changes_dialog.exec_()
                    if(ret == QMessageBox.Save):
                        if(self.model.database.index_current_game == None):
                            self.gs_ctr.editGameData()
                            self.model.database.append_game(self.model.gamestate.current)
                        else:
                            print(self.model.gamestate.current)
                            print(self.model.database.index_current_game)
                            self.model.database.update_game(self.model.database.index_current_game,\
                                                    self.model.gamestate.current)
                        self.mainAppWindow.save.setEnabled(False)
                    if(ret == QMessageBox.Cancel):
                        cancel = True
                if(not cancel):
                    self.model.gamestate.current = loaded_game
                    self.mainAppWindow.chessboard_view.update()
                    self.mainAppWindow.chessboard_view.emit(SIGNAL("statechanged()"))
                    self.model.gamestate.unsaved_changes = False
                    self.mainAppWindow.save.setEnabled(False)
                    self.mainAppWindow.setLabels()
                    self.mainAppWindow.moves_edit_view.setFocus()
                    self.model.gamestate.init_game_tree(self.mainAppWindow)
        self.mainAppWindow.moves_edit_view.setFocus()


    def save_image(self):
        q_widget = self.mainAppWindow
        filename = QFileDialog.getSaveFileName(q_widget, q_widget.trUtf8('Save Image'), None, \
                                               'JPG (*.jpg)', QFileDialog.DontUseNativeDialog)
        if(filename):
            p = QPixmap.grabWindow(q_widget.chessboard_view.winId())
            p.save(filename,'jpg')

    def print_game(self):
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            exporter = chess.pgn.StringExporter()
            self.model.gamestate.current.root().export(exporter, headers=True, variations=True, comments=True)
            pgn = str(exporter)
            QPgn = QPlainTextEdit(pgn)
            QPgn.print_(dialog.printer())

    def print_position(self):
        q_widget = self.mainAppWindow
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            p = QPixmap.grabWindow(q_widget.chessboard_view.winId())
            painter = QPainter(dialog.printer())
            dst = QRect(0,0,200,200)
            painter.drawPixmap(dst, p)
            del painter
