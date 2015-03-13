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
    filename = QFileDialog.getSaveFileName(q_widget, 'Save Image', None, 'JPG (*.jpg)', QFileDialog.DontUseNativeDialog)
    if(filename):
        p = QPixmap.grabWindow(q_widget.board.winId())
        p.save(filename,'jpg')


def append_to_pgn(q_widget):
    gamestate = q_widget.gs
    filename = QFileDialog.getSaveFileName(q_widget, 'Append to PGN', None,
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

    #filename = QFileDialog.getSaveFileName(mainWidget, 'Save PGN', None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
    #if(filename):
    #    f = open(filename,'w')
    #    print(gamestate.current.root(), file=f, end="\n\n")
    #    mainWidget.movesEdit.setFocus()


#QFileDialog.DontConfirmOverwrite
def save_as_to_pgn(mainWidget):
    gamestate = mainWidget.gs
    dialog = QFileDialog()
    if(gamestate.last_save_dir != None):
        dialog.setDirectory(gamestate.last_save_dir)
    filename = dialog.getSaveFileName(mainWidget, 'Save PGN', None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
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

def open_pgn(mainWindow):
    chessboardview = mainWindow.board
    gamestate = mainWindow.gs
    dialog = QFileDialog()
    if(gamestate.last_open_dir != None):
        dialog.setDirectory(gamestate.last_open_dir)
    filename = dialog.getOpenFileName(chessboardview, 'Open PGN', None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
    if(filename):
        pgn = open(filename)
        first_game = chess.pgn.read_game(pgn)
        gamestate.current = first_game
        chessboardview.update()
        chessboardview.emit(SIGNAL("statechanged()"))
        #self.movesEdit.bv = self
        gamestate.pgn_filename = filename
        mainWindow.save_game.setEnabled(True)
        mainWindow.setLabels()
        mainWindow.movesEdit.setFocus()
        pgn.close()
        gamestate.last_open_dir = QFileInfo(filename).dir().absolutePath()

        #self.movesEdit.update_san()
        #self.movesEdit.setFocus()

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