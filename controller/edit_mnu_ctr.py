from PyQt4.QtGui import *
from PyQt4.QtCore import *
import io
import chess
from dialogs.dialog_edit_game_data import DialogEditGameData
from dialogs.dialog_enter_position import DialogEnterPosition
#from controller.file_menu_ctr import init_game_tree

class EditMenuController():

    def __init__(self, mainAppWindow, model):
        super(EditMenuController, self).__init__()
        self.mainAppWindow = mainAppWindow
        self.model = model

    def copy_game_to_clipboard(self):
        print("called game to clipboard")
        clipboard = QApplication.clipboard()
        exporter = chess.pgn.StringExporter()
        self.model.gamestate.current.root().export(exporter, headers=True, variations=True, comments=True)
        pgn_string = str(exporter)
        clipboard.setText(pgn_string)

    def copy_pos_to_clipboard(self):
        clipboard = QApplication.clipboard()
        clipboard.setText(self.model.gamestate.current.board().fen())

    def paste_from_clipboard(self):
        gamestate = self.model.gamestate
        boardview = self.mainAppWindow.chessboard_view
        clipboard = QApplication.clipboard()
        try:
            root = chess.pgn.Game()
            #gamestate.initialize_headers()
            root.headers["FEN"] = ""
            root.headers["SetUp"] = ""
            board = chess.Bitboard(clipboard.text())
            root.setup(board)
            if(root.board().status() == 0):
                gamestate.current = root
                gamestate.game = root
                gamestate.root = root
                self.mainAppWindow.save_game.setEnabled(False)
        except ValueError:
            pgn = io.StringIO(clipboard.text())
            first_game = chess.pgn.read_game(pgn)
            if(first_game != None):
                gamestate.current = first_game
                self.mainAppWindow.setLabels()
                self.mainAppWindow.save.setEnabled(False)
                self.model.gamestate.init_game_tree(self.mainAppWindow)
        boardview.update()
        boardview.emit(SIGNAL("statechanged()"))



    def enter_position(self):
        mainWindow = self.mainAppWindow
        dialog = DialogEnterPosition(mainWindow.model.gamestate.current.board())
        answer = dialog.exec_()
        if(answer):
            root = chess.pgn.Game()
            root.headers["FEN"] = ""
            root.headers["SetUp"] = ""
            root.setup(dialog.displayBoard.board)
            mainWindow.model.gamestate.current = root
            mainWindow.model.gamestate.initialize_headers()
            mainWindow.setLabels()
            mainWindow.chessboard_view.on_statechanged()
            mainWindow.moves_edit_view.on_statechanged()
            mainWindow.save.setEnabled(False)
            mainWindow.update()

    def reset_to_initial(self):
        pass