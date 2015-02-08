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
        p = QPixmap.grabWindow(q_widget.winId())
        p.save(filename,'jpg')


def append_to_pgn(q_widget):
    filename = QFileDialog.getSaveFileName(q_widget, 'Append to PGN', None,
                                           'PGN (*.pgn)', QFileDialog.DontConfirmOverwrite)

def save_to_pgn(q_widget,gamestate):
    filename = QFileDialog.getSaveFileName(q_widget, 'Save PGN', None, 'PGN (*.pgn)', QFileDialog.DontUseNativeDialog)
    if(filename):
        f = open(filename,'w')
        print(gamestate.current.root(), file=f, end="\n\n")

def open_pgn(chessboardview,gamestate):
    filename = QFileDialog.getOpenFileName(chessboardview, 'Open PGN', None, 'PGN (*.pgn)',QFileDialog.DontUseNativeDialog)
    if(filename):
        pgn = open(filename)
        first_game = chess.pgn.read_game(pgn)
        gamestate.current = first_game
        chessboardview.update()
        chessboardview.emit(SIGNAL("statechanged()"))
        #self.movesEdit.bv = self

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