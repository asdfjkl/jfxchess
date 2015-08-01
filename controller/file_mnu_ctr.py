from PyQt4.QtGui import *
from PyQt4.QtCore import *
#import chess
from chess.polyglot import *
from dialogs.dialog_browse_pgn import DialogBrowsePgn
from model.database import Database
import controller
import model.gamestate
from dialogs.dialog_save_changes import DialogSaveChanges

class FileMenuController():

    def __init__(self, mainAppWindow, model, gamestate_controller):
        super(FileMenuController, self).__init__()
        self.mWin = mainAppWindow
        self.mdl = model
        self.gs_ctr = gamestate_controller

    def new_database(self):
        # if the game is not in database (no index), or if the gamestate
        # has unsaved changes, first ask the user if he wants to save
        # the current game. if yes, then either let the user write the game
        # headers and add the game to database,
        # or update the database entry with the current gamestate
        cancel = False
        if(self.mdl.database.index_current_game == None or self.mdl.gamestate.unsaved_changes):
            changes_dialog = DialogSaveChanges()
            ret = changes_dialog.exec_()
            if(ret == QMessageBox.Save):
                if(self.mdl.database.index_current_game == None):
                    self.gs_ctr.editGameData()
                    self.mdl.database.append_game(self.mdl.gamestate.current)
                else:
                    self.mdl.database.update_game(self.mdl.database.index_current_game,\
                                                    self.mdl.gamestate.current)
                self.mWin.save.setEnabled(False)
            if(ret == QMessageBox.Cancel):
                cancel = True
        if(not cancel):
            file_dialog = QFileDialog()
            filename = file_dialog.getSaveFileName(self.mWin, self.mWin.trUtf8('Create New PGN'), \
                                                   None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
            if(filename):
                if(not filename.endswith(".pgn")):
                    filename = filename + ".pgn"
                self.mdl.gamestate.last_save_dir = QFileInfo(filename).dir().absolutePath()
                db = Database(filename)
                db.create_new_pgn()
                self.mWin.save.setEnabled(False)
                self.mdl.database = db
                self.mdl.user_settings.active_database = db.filename

    def open_database(self):
        mainWindow = self.mWin
        chessboardview = mainWindow.chessboard_view
        gamestate = mainWindow.model.gamestate
        dialog = QFileDialog()
        if(gamestate.last_open_dir != None):
            dialog.setDirectory(gamestate.last_open_dir)
        filename = dialog.getOpenFileName(chessboardview, mainWindow.trUtf8('Open PGN'), None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
        if filename:
            db = Database(filename)
            db.init_from_file(mainWindow,mainWindow.trUtf8("Scanning PGN File..."))
            mainWindow.model.database = db
            mainWindow.model.user_settings.active_database = db.filename
            selectedGame = 0
            if(db.no_of_games() > 1):
                dlg = DialogBrowsePgn(db)
                if dlg.exec_() == QDialog.Accepted:
                    items = dlg.table.selectedItems()
                    selectedGame = int(items[0].text())-1
                else:
                    selectedGame = None
            if(not selectedGame == None):
                loaded_game = db.load_game(selectedGame)
                    #offset, headers = db.offset_headers[idx]
                    #pgn.seek(offset)
                    #first_game = chess.pgn.read_game(pgn)
                gamestate.current = loaded_game
                chessboardview.update()
                chessboardview.emit(SIGNAL("statechanged()"))
                mainWindow.save.setEnabled(False)
                mainWindow.setLabels()
                mainWindow.moves_edit_view.setFocus()
                gamestate.last_open_dir = QFileInfo(filename).dir().absolutePath()
                gamestate.init_game_tree(self.mWin)

                    #if(dlg.table.hasS)
                    #print(str(dlg.table.selectedIndexes()))
    def browse_games(self):
        mainWindow = self.mWin
        selectedGame = 0
        mainWindow.model.gamestate.mode = model.gamestate.MODE_ENTER_MOVES
        db = mainWindow.model.database
        gs = mainWindow.model.gamestate
        cbv = mainWindow.chessboard_view

        dlg = DialogBrowsePgn(db)
        #
        if dlg.exec_() == QDialog.Accepted:
            items = dlg.table.selectedItems()
            selectedGame = int(items[0].text())-1
            loaded_game = db.load_game(selectedGame)
            #offset, headers = db.offset_headers[idx]
            #pgn.seek(offset)
            #first_game = chess.pgn.read_game(pgn)
            print("loaded game: "+str(loaded_game))
            if(not loaded_game == None):
                ret = controller.gameevents.unsaved_changes(mainWindow)
                if not ret == QMessageBox.Cancel:
                    gs.current = loaded_game
                    cbv.update()
                    cbv.emit(SIGNAL("statechanged()"))
                    gs.unsaved_changes = False
                    mainWindow.save.setEnabled(False)
                    mainWindow.setLabels()
                    mainWindow.moves_edit_view.setFocus()
                    #self.init_game_tree(gs.current.root())
                    gs.init_game_tree(self.mWin)


    def save_image(self):
        q_widget = self.mWin
        filename = QFileDialog.getSaveFileName(q_widget, q_widget.trUtf8('Save Image'), None, 'JPG (*.jpg)', QFileDialog.DontUseNativeDialog)
        if(filename):
            p = QPixmap.grabWindow(q_widget.chessboard_view.winId())
            p.save(filename,'jpg')


    def print_game(self):
        gamestate = self.mdl.gamestate
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            exporter = chess.pgn.StringExporter()
            gamestate.current.root().export(exporter, headers=True, variations=True, comments=True)
            pgn = str(exporter)
            QPgn = QPlainTextEdit(pgn)
            QPgn.print_(dialog.printer())

    def print_position(self):
        q_widget = self.mWin
        dialog = QPrintDialog()
        if dialog.exec_() == QDialog.Accepted:
            p = QPixmap.grabWindow(q_widget.winId())
            painter = QPainter(dialog.printer())
            dst = QRect(0,0,200,200)
            painter.drawPixmap(dst, p)
            del painter

########################
"""
        def unsaved_changes(self, mainWindow):
            print("unsaved changes")
            print(str(mainWindow.model.gamestate.unsaved_changes))
            print(str(mainWindow.model.database.index_current_game))
            # dialog to be called to
            # check for unsaved changes to
            # the current game
            # if current game has unsaved changes
            # or is not saved in database
            # ask user if he wants save it
            if(mainWindow.model.database.index_current_game == None or
                   mainWindow.model.gamestate.unsaved_changes):
                print("inside loop")
                dlg_changes = DialogSaveChanges()
                ret = dlg_changes.exec_()
                if(ret == QMessageBox.Save):
                    # if game is not in db append
                    if(mainWindow.model.database.index_current_game == None):
                        controller.edit_mnu_ctr.editGameData(mainWindow)
                        mainWindow.model.database.append_game(mainWindow.gs.current)
                    else:
                        mainWindow.model.database.update_game(mainWindow.database.index_current_game,mainWindow.gs.current)
                    mainWindow.save.setEnabled(False)
                return ret
            else:
                return QMessageBox.Discard


"""




        """
        if(filename):
            pgn = open(filename)
            first_game = chess.pgn.read_game(pgn)
            gamestate.current = first_game
            chessboardview.update()
            chessboardview.emit(SIGNAL("statechanged()"))
            gamestate.pgn_filename = filename
            mainWindow.save_game.setEnabled(True)
            mainWindow.setLabels()
            mainWindow.movesEdit.setFocus()
            pgn.close()
            gamestate.last_open_dir = QFileInfo(filename).dir().absolutePath()
            init_game_tree(mainWindow,gamestate.current.root())
        """


