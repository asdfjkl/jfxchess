from PyQt4.QtGui import *
from PyQt4.QtCore import *
import io
import chess
from dialogs.DialogEditGameData import DialogEditGameData
from dialogs.DialogEnterPosition import DialogEnterPosition

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

def editGameData(mainWindow):
    root = mainWindow.gs.current.root()
    ed = DialogEditGameData(root)
    answer = ed.exec_()
    if(answer):
        root.headers["Event"] = ed.ed_event.text()
        root.headers["Site"] = ed.ed_site.text()
        root.headers["Date"] = ed.ed_date.text()
        root.headers["Round"] = ed.ed_round.text()
        root.headers["White"] = ed.ed_white.text()
        root.headers["Black"] = ed.ed_black.text()
        if(ed.rb_ww.isChecked()):
            root.headers["Result"] = "1-0"
        elif(ed.rb_bw.isChecked()):
            root.headers["Result"] = "0-1"
        elif(ed.rb_draw.isChecked()):
            root.headers["Result"] = "1/2-1/2"
        elif(ed.rb_unclear.isChecked()):
            root.headers["Result"] = "*"
    mainWindow.setLabels()

def enter_position(mainWindow):
    dialog = DialogEnterPosition(mainWindow.gs.current.board())
    answer = dialog.exec_()
    if(answer):
        root = chess.pgn.Game()
        root.headers["FEN"] = ""
        root.headers["SetUp"] = ""
        root.setup(dialog.displayBoard.board)
        mainWindow.gs.current = root
        mainWindow.gs.initialize_headers()
        mainWindow.setLabels()
        mainWindow.board.on_statechanged()
        mainWindow.movesEdit.on_statechanged()
        mainWindow.update()