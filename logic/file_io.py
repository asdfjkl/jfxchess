from PyQt4.QtGui import *
from PyQt4.QtCore import *
#import chess
from chess.polyglot import *
from dialogs.DialogBrowsePgn import DialogBrowsePgn
import os
from logic.database import Database

def print_game(gamestate):
    dialog = QPrintDialog()
    if dialog.exec_() == QDialog.Accepted:
        exporter = chess.pgn.StringExporter()
        gamestate.current.root().export(exporter, headers=True, variations=True, comments=True)
        pgn = str(exporter)
        QPgn = QPlainTextEdit(pgn)
        QPgn.print_(dialog.printer())

def print_position(q_widget):
    dialog = QPrintDialog()
    if dialog.exec_() == QDialog.Accepted:
        p = QPixmap.grabWindow(q_widget.winId())
        painter = QPainter(dialog.printer())
        dst = QRect(0,0,200,200)
        painter.drawPixmap(dst, p)
        del painter

def save_image(q_widget):
    filename = QFileDialog.getSaveFileName(q_widget, q_widget.trUtf8('Save Image'), None, 'JPG (*.jpg)', QFileDialog.DontUseNativeDialog)
    if(filename):
        p = QPixmap.grabWindow(q_widget.board.winId())
        p.save(filename,'jpg')

def save(mainWidget):
    db = mainWidget.database
    db.save(mainWidget)
    mainWidget.user_settings.active_database = db.filename
    mainWidget.save.setEnabled(False)
    mainWidget.movesEdit.setFocus()

def save_db_as_new(mainWidget):
    gs = mainWidget.gs
    db = mainWidget.database
    dialog = QFileDialog()
    if(gs.last_save_dir != None):
        dialog.setDirectory(gs.last_save_dir)
    filename = dialog.getSaveFileName(mainWidget, mainWidget.trUtf8('Save PGN'), None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
    if(filename):
        if(not filename.endswith(".pgn")):
            filename = filename + ".pgn"
        db.save_as_new(mainWidget, filename)
        mainWidget.user_settings.active_database = db.filename
        mainWidget.save.setEnabled(False)
        mainWidget.movesEdit.setFocus()
        gs.last_save_dir = QFileInfo(filename).dir().absolutePath()



def export_game(mainWidget):
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

def init_game_tree(mainWindow, root):
    gamestate = mainWindow.gs
    # ugly workaround:
    # the next lines are just to ensure that
    # the "board cache" (see doc. of python-chess lib)
    # is initialized. The call to the last board of the main
    # line ensures recursive calls to the boards up to
    # the root
    temp = gamestate.current.root()
    end = gamestate.current.end()
    mainline_nodes = [temp]
    while(not temp == end):
        temp = temp.variations[0]
        mainline_nodes.append(temp)
    cnt = len(mainline_nodes)
    pDialog = QProgressDialog(mainWindow.trUtf8("Initializing Game Tree"),None,0,cnt,mainWindow)
    pDialog.setWindowModality(Qt.WindowModal)
    pDialog.show()
    QApplication.processEvents()
    for i,n in enumerate(mainline_nodes):
        QApplication.processEvents()
        pDialog.setValue(i)
        if(i > 0 and i % 25 == 0):
            _ = n.cache_board()
    pDialog.close()

def new_database(mainWindow):
    dialog = QFileDialog()
    filename = dialog.getSaveFileName(mainWindow, mainWindow.trUtf8('Create New PGN'), None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
    if(filename):
        if(mainWindow.database.unsaved_changes):
            mainWindow.database.save(mainWindow)
        if(not filename.endswith(".pgn")):
            filename = filename + ".pgn"
        #with open(filename,'w') as pgn:
        #    print(mainWindow.gs.current.root(), file=pgn, end="\n\n")
        #    mainWindow.save_game.setEnabled(True)
        #    mainWindow.movesEdit.setFocus()
        mainWindow.gs.last_save_dir = QFileInfo(filename).dir().absolutePath()
        db = Database(filename)
        db.create_new_pgn()
        mainWindow.save.setEnabled(False)
        mainWindow.database = db
        mainWindow.user_settings.active_database = db.filename

def open_pgn(mainWindow):
    chessboardview = mainWindow.board
    gamestate = mainWindow.gs
    dialog = QFileDialog()
    if(gamestate.last_open_dir != None):
        dialog.setDirectory(gamestate.last_open_dir)
    filename = dialog.getOpenFileName(chessboardview, mainWindow.trUtf8('Open PGN'), None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
    if filename:
        db = Database(filename)
        db.init_from_file(mainWindow)
        mainWindow.database = db
        mainWindow.user_settings.active_database = db.filename
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
            mainWindow.movesEdit.setFocus()
            gamestate.last_open_dir = QFileInfo(filename).dir().absolutePath()
            init_game_tree(mainWindow,gamestate.current.root())

                #if(dlg.table.hasS)
                #print(str(dlg.table.selectedIndexes()))


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

def browse_database(mainWindow):
    selectedGame = 0
    db = mainWindow.database
    gs = mainWindow.gs
    cbv = mainWindow.board
    dlg = DialogBrowsePgn(db)
    if dlg.exec_() == QDialog.Accepted:
        items = dlg.table.selectedItems()
        selectedGame = int(items[0].text())-1
        loaded_game = db.load_game(selectedGame)
        #offset, headers = db.offset_headers[idx]
        #pgn.seek(offset)
        #first_game = chess.pgn.read_game(pgn)
        print("loaded game: "+str(loaded_game))
        if(not loaded_game == None):
            gs.current = loaded_game
            cbv.update()
            cbv.emit(SIGNAL("statechanged()"))
            mainWindow.setLabels()
            mainWindow.movesEdit.setFocus()
            init_game_tree(mainWindow,gs.current.root())


def is_position_in_book(node):
    with open_reader("./books/varied.bin") as reader:
        entries = reader.get_entries_for_position(node.board())
        moves = []
        for entry in entries:
            move = entry.move().uci()
            moves.append(move)
        l = len(moves)
        if(l > 0):
            return True
        else:
            return False