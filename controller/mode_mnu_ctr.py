from model.gamestate import MODE_GAME_ANALYSIS, MODE_PLAYOUT_POS, \
    MODE_PLAY_WHITE, MODE_ANALYSIS, MODE_PLAY_BLACK, MODE_ENTER_MOVES
from dialogs.dialog_analyze_game import DialogAnalyzeGame
from dialogs.dialog_strength_level import DialogStrengthLevel
from dialogs.dialog_engines import DialogEngines
from uci.engine_info import EngineInfo
from  PyQt4.QtGui import QDialog
import chess

class ModeMenuController():

    def __init__(self, mainWindow, model):
        super(ModeMenuController, self).__init__()
        self.model = model
        self.mainAppWindow = mainWindow
        self.uci_controller = mainWindow.engine_controller

    def on_analysis_mode(self):
        self.mainAppWindow.display_info.setChecked(True)
        self.mainAppWindow.set_display_info()
        self.mainAppWindow.engine_controller.reset_engine(self.mainAppWindow.model.user_settings.active_engine)
        self.mainAppWindow.give_up.setEnabled(False)
        self.mainAppWindow.offer_draw.setEnabled(False)
        fen, uci_string = self.mainAppWindow.model.gamestate.printer.to_uci(self.mainAppWindow.model.gamestate.current)
        self.mainAppWindow.engine_controller.send_engine_options(self.mainAppWindow.model.user_settings.active_engine.options)
        self.mainAppWindow.engine_controller.send_fen(fen)
        self.mainAppWindow.engine_controller.uci_send_position(uci_string)
        self.mainAppWindow.engine_controller.uci_go_infinite()
        self.mainAppWindow.model.gamestate.engine_info.strength = None
        self.mainAppWindow.model.gamestate.mode = MODE_ANALYSIS

    def on_play_as_white(self):
        mainWindow = self.mainAppWindow
        mainWindow.chessboard_view.flippedBoard = False
        mainWindow.chessboard_view.on_statechanged()
        mainWindow.model.gamestate.mode = MODE_PLAY_WHITE
        self.uci_controller.reset_engine(mainWindow.model.user_settings.active_engine)
        mainWindow.engineOutput.setHtml("")
        mainWindow.give_up.setEnabled(True)
        mainWindow.offer_draw.setEnabled(True)
        # strength is only set if internal engine is used
        # otherwise the dialog is meaningless
        if(mainWindow.model.user_settings.active_engine == mainWindow.model.user_settings.engines[0]):
            mainWindow.engine_controller.uci_strength(mainWindow.model.gamestate.strength_level)
        mainWindow.model.gamestate.engine_info.strength = str((mainWindow.model.gamestate.strength_level * 100)+1200)
        self.uci_controller.send_engine_options(mainWindow.model.user_settings.active_engine.options)
        if(mainWindow.model.gamestate.current.board().turn == chess.BLACK):
            fen, uci_string = mainWindow.model.gamestate.printer.to_uci(mainWindow.model.gamestate.current)
            mainWindow.engine_controller.send_fen(fen)
            mainWindow.engine_controller.uci_send_position(uci_string)
            mainWindow.engine_controller.uci_go_movetime(mainWindow.gs.computer_think_time)


    def on_play_as_black(self):
        mainWindow = self.mainAppWindow
        mainWindow.chessboard_view.flippedBoard = True
        mainWindow.chessboard_view.on_statechanged()
        mainWindow.model.gamestate.mode = MODE_PLAY_BLACK
        mainWindow.engineOutput.setHtml("")
        self.uci_controller.reset_engine(mainWindow.model.user_settings.active_engine)
        mainWindow.give_up.setEnabled(True)
        mainWindow.offer_draw.setEnabled(True)
        # strength is only set if internal engine is used
        # otherwise the dialog is meaningless
        if(mainWindow.model.user_settings.active_engine == mainWindow.model.user_settings.engines[0]):
            mainWindow.engine_controller.uci_strength(mainWindow.model.gamestate.strength_level)
        mainWindow.model.gamestate.engine_info.strength = str((mainWindow.model.gamestate.strength_level * 100)+1200)
        self.uci_controller.send_engine_options(mainWindow.model.user_settings.active_engine.options)
        if(mainWindow.model.gamestate.current.board().turn == chess.WHITE):
            fen, uci_string = mainWindow.model.gamestate.printer.to_uci(mainWindow.model.gamestate.current)
            mainWindow.engine_controller.send_fen(fen)
            mainWindow.engine_controller.uci_send_position(uci_string)
            mainWindow.engine_controller.uci_go_movetime(mainWindow.model.gamestate.computer_think_time)

    def on_enter_moves_mode(self):
        # stop any engine
        mainWindow = self.mainAppWindow
        mainWindow.engine_controller.stop_engine()
        mainWindow.engineOutput.setHtml("")
        mainWindow.give_up.setEnabled(False)
        mainWindow.offer_draw.setEnabled(False)
        mainWindow.model.gamestate.mode = MODE_ENTER_MOVES
        mainWindow.enter_moves.setChecked(True)


    def on_game_analysis_mode(self):
        mainWindow = self.mainAppWindow
        gs = mainWindow.model.gamestate
        dialog = DialogAnalyzeGame(gamestate=gs)
        if dialog.exec_() == QDialog.Accepted:
            gs.computer_think_time = dialog.sb_secs.value()*1000
            gs.analysis_threshold = dialog.sb_threshold.value()
            gs.engine_info.strength = None
            mainWindow.display_info.setChecked(True)
            mainWindow.set_display_info()
            self.uci_controller.reset_engine(mainWindow.model.user_settings.active_engine)
            mainWindow.give_up.setEnabled(False)
            mainWindow.offer_draw.setEnabled(False)
            mainWindow.moves_edit_view.delete_all_comments()
            mainWindow.moves_edit_view.delete_all_variants()
            gs.current = gs.current.root()
            gs.best_score = None
            gs.mate_threat = False
            while(len(gs.current.variations) > 0):
                gs.current = gs.current.variations[0]
            if(gs.current.board().is_checkmate() or gs.current.board().is_stalemate()):
                gs.current = gs.current.parent
            mainWindow.moves_edit_view.update_san()
            fen, uci_string = gs.printer.to_uci(gs.current)
            mainWindow.engine_controller.send_engine_options(mainWindow.model.user_settings.active_engine.options)
            mainWindow.engine_controller.send_fen(fen)
            mainWindow.engine_controller.uci_send_position(uci_string)
            mainWindow.engine_controller.uci_go_movetime(gs.computer_think_time)
            gs.mode = MODE_GAME_ANALYSIS
            gs.best_score = None
        else:
            self.on_enter_moves_mode(mainWindow)


    def on_playout_pos(self):
        mainWindow = self.mainAppWindow
        mainWindow.display_info.setChecked(True)
        mainWindow.set_display_info()
        self.uci_controller.reset_engine(mainWindow.model.user_settings.active_engine.options)
        mainWindow.give_up.setEnabled(False)
        mainWindow.offer_draw.setEnabled(False)
        fen, uci_string = mainWindow.model.gamestate.printer.to_uci(mainWindow.model.gamestate.current)
        self.uci_controller.send_engine_options(mainWindow.model.user_settings.active_engine.options)
        mainWindow.engine_controller.send_fen(fen)
        mainWindow.engine_controller.uci_send_position(uci_string)
        mainWindow.engine_controller.uci_go_movetime(mainWindow.model.gamestate.computer_think_time)
        mainWindow.model.gamestate.engine_info.strength = None
        mainWindow.model.gamestate.mode = MODE_PLAYOUT_POS
        mainWindow.enter_moves.setChecked(False)
        #self.analysis.setChecked(False)
        mainWindow.play_white.setChecked(False)
        mainWindow.play_black.setChecked(False)



    def on_strength_level(self):
        mainWindow = self.mainAppWindow
        gamestate = mainWindow.model.gamestate
        engine = mainWindow.engine_controller
        settings = mainWindow.model.user_settings
        dialog = DialogStrengthLevel(gamestate=gamestate, user_settings=mainWindow.model.user_settings)
        if dialog.exec_() == QDialog.Accepted:
            # strength is only changed if internal engine is used
            # otherwise the dialog is meaningless
            if(settings.active_engine == settings.engines[0]):
                gamestate.strength_level = dialog.slider_elo.value()
            val = dialog.slider_think.value()
            gamestate.computer_think_time = val
            if(val == 4):
                gamestate.computer_think_time = 5
            elif(val == 5):
                gamestate.computer_think_time = 10
            elif(val == 6):
                gamestate.computer_think_time = 15
            elif(val == 7):
                gamestate.computer_think_time = 30
            gamestate.computer_think_time = gamestate.computer_think_time*1000
        if(gamestate.mode == MODE_PLAY_WHITE or gamestate.mode == MODE_PLAY_BLACK):
            engine.uci_strength(gamestate.strength_level)

    def on_set_engines(self):
        mainWidget = self.mainAppWindow
        user_settings = mainWidget.model.user_settings
        dialog = DialogEngines(user_settings=user_settings)
        self.on_enter_moves_mode(mainWidget)
        if dialog.exec_() == QDialog.Accepted:
            user_settings.engines = dialog.engines
            user_settings.active_engine = dialog.active_engine
            info = EngineInfo()
            info.id = user_settings.active_engine.name
            self.receive_engine_info(mainWidget,info)
            print("active engine after dialog setting"+str(user_settings.active_engine.path))
            #todo update views, i.e. label above engine window