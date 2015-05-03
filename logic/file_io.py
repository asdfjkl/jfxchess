from PyQt4.QtGui import *
from PyQt4.QtCore import *
#import chess
from chess.polyglot import *

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


def append_to_pgn(q_widget):
    gamestate = q_widget.gs
    filename = QFileDialog.getSaveFileName(q_widget, q_widget.tr('Append to PGN'), None,
                                           'PGN (*.pgn)', QFileDialog.DontUseNativeDialog | QFileDialog.DontConfirmOverwrite)
    if(filename):
        f = open(filename,'a')
        print("\n",file=f)
        print(gamestate.current.root(), file=f, end="\n\n")
        q_widget.movesEdit.setFocus()
        f.close()

def save_to_pgn(mainWidget):
    gamestate = mainWidget.gs
    if(gamestate.pgn_filename != None):
        try:
            f = open(gamestate.pgn_filename,'w')
            print(gamestate.current.root(), file=f, end="\n\n")
            mainWidget.movesEdit.setFocus()
            f.close()
        except (OSError, IOError) as e:
            save_as_to_pgn(mainWidget)


def save_as_to_pgn(mainWidget):
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
        mainWidget.save_game.setEnabled(True)
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
    pDialog = QProgressDialog(mainWindow.self.trUtf8("Initializing Game Tree"),None,0,cnt,mainWindow)
    pDialog.setWindowModality(Qt.WindowModal)
    pDialog.show()
    QApplication.processEvents()
    for i,n in enumerate(mainline_nodes):
        QApplication.processEvents()
        pDialog.setValue(i)
        if(i > 0 and i % 25 == 0):
            _ = n.cache_board()
    pDialog.close()

def open_pgn(mainWindow):
    chessboardview = mainWindow.board
    gamestate = mainWindow.gs
    dialog = QFileDialog()
    if(gamestate.last_open_dir != None):
        dialog.setDirectory(gamestate.last_open_dir)
    filename = dialog.getOpenFileName(chessboardview, mainWindow.self.trUtf8('Open PGN'), None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
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