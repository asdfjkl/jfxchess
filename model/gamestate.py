from views.game_gui_printer import GUIPrinter
import chess
import chess.polyglot
import time
from uci.engine_info import EngineInfo
from PyQt4.QtCore import Qt
from PyQt4.QtGui import QProgressDialog, QApplication

MODE_ENTER_MOVES = 0
MODE_PLAY_BLACK = 1
MODE_PLAY_WHITE = 2
MODE_ANALYSIS = 3
MODE_PLAYOUT_POS = 4
MODE_GAME_ANALYSIS = 5

class GameState():

    def __init__(self):
        super(GameState, self).__init__()
        #self.game = chess.pgn.Game()
        #self.root = self.game
        #self.current = self.game
        self.current = chess.pgn.Game()
        self.mode = MODE_ENTER_MOVES
        self.printer = GUIPrinter()
        self.computer_think_time = 3000
        self.display_engine_info = True
        self.score = 0
        self.position_bad = 0
        self.position_draw = 0
        self.pv = []
        self.analysis_threshold = 0.5

        self.engine_info = EngineInfo()

        self.mate_threat = None
        self.next_mate_threat = None

        self.best_score = None
        self.best_pv = []

        self.timed_game = False
        self.time_white = 0
        self.time_black = 0
        self.add_secs_per_move = 0
        self.strength_level = 3
        self.initialize_headers()

        self.pgn_filename = None

        self.last_save_dir = None
        self.last_open_dir = None

        self.unsaved_changes = False
        self.game_analysis_white = False
        self.game_analysis_black = False

    def half_moves(self):
        halfmoves = 0
        temp = self.current
        while(temp.parent):
            temp = temp.parent
            halfmoves += 1
        return halfmoves

    def initialize_headers(self):
        game = self.current.root()
        game.headers["Event"] = ""
        game.headers["Site"] = "MyTown"
        game.headers["Date"] = time.strftime("%Y.%m.%d")
        game.headers["Black"] = "Jerry (Level "+str(1200 + 100*self.strength_level)+")"
        game.headers["White"] = "N.N."
        game.headers["Round"] = ""
        game.headers["Result"] = "*"

    def init_game_tree(self, mainAppWindow = None):
        # ugly workaround:
        # the next lines are just to ensure that
        # the "board cache" (see doc. of python-chess lib)
        # is initialized. The call to the last board of the main
        # line ensures recursive calls to the boards up to the root
        #
        # if a mainAppWindow is passed, a progress bar
        # will be displayed while the game tree is initialized
        # the app might "freeze" otherwise, as longer games
        # i.e. (> 70 moves) can take some time to initialize
        temp = self.current.root()
        end = self.current.end()
        mainline_nodes = [temp]
        while(not temp == end):
            temp = temp.variations[0]
            mainline_nodes.append(temp)
        cnt = len(mainline_nodes)
        """
        if(not mainAppWindow == None):
            pDialog = QProgressDialog(mainAppWindow.trUtf8("Initializing Game Tree"),None,0,cnt,mainAppWindow)
            pDialog.setWindowModality(Qt.WindowModal)
            pDialog.show()
            QApplication.processEvents()
            for i,n in enumerate(mainline_nodes):
                QApplication.processEvents()
                pDialog.setValue(i)
                if(i > 0 and i % 25 == 0):
                    _ = n.cache_board()
            pDialog.close()
        else:
            QApplication.processEvents()
            for i,n in enumerate(mainline_nodes):
                if(i > 0 and i % 25 == 0):
                    _ = n.cache_board()
        """

    def is_current_position_in_book(self,node=None):
        if node == None:
            node = self.current
        with chess.polyglot.open_reader("./books/varied.bin") as reader:
            entries = reader.find_all(node.board())
            moves = []
            for entry in entries:
                move = entry.move().uci()
                moves.append(move)
            l = len(moves)
            if(l > 0):
                return True
            else:
                return False