from gui.GUIPrinter import GUIPrinter
import chess
import time
from uci.engine_info import EngineInfo
from PyQt4.QtCore import QTimer

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
