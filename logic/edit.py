from PyQt4.QtGui import *
from PyQt4.QtCore import *
import io
import chess

def game_to_clipboard(gamestate):
    clipboard = QApplication.clipboard()
    exporter = chess.pgn.StringExporter()
    gamestate.current.root().export(exporter, headers=True, variations=True, comments=True)
    pgn_string = str(exporter)
    clipboard.setText(pgn_string)

def pos_to_clipboard(gamestate):
    clipboard = QApplication.clipboard()
    clipboard.setText(gamestate.current.board().fen())

def from_clipboard(gamestate,boardview):
    clipboard = QApplication.clipboard()
    try:
        root = chess.pgn.Game()
        boardview.setup_headers(root)
        root.headers["FEN"] = ""
        root.headers["SetUp"] = ""
        board = chess.Bitboard(clipboard.text())
        root.setup(board)
        if(root.board().status() == 0):
            gamestate.current = root
            gamestate.game = root
            gamestate.root = root
    except ValueError:
        pgn = io.StringIO(clipboard.text())
        first_game = chess.pgn.read_game(pgn)
        gamestate.current = first_game

    boardview.update()
    boardview.emit(SIGNAL("statechanged()"))
    #self.movesEdit.bv = self

    #self.movesEdit.update_san()
    #self.mainWindow.setLabels(self.gs.current)
    #self.movesEdit.setFocus()
