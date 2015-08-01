from PyQt4.QtGui import *
from PyQt4.QtCore import *

from dialogs.dialog_strength_level import DialogStrengthLevel
from dialogs.dialog_new_game import DialogNewGame
from dialogs.dialog_analyze_game import DialogAnalyzeGame
from dialogs.dialog_save_changes import DialogSaveChanges
from model.gamestate import GameState
from model.gamestate import MODE_ENTER_MOVES, \
    MODE_PLAY_WHITE, MODE_PLAY_BLACK, MODE_ANALYSIS, \
    MODE_GAME_ANALYSIS, MODE_PLAYOUT_POS
import chess
#from controller.file_menu_ctr import is_position_in_book
from util.messages import display_mbox
from dialogs.dialog_engines import DialogEngines
from uci.engine_info import EngineInfo
import controller.edit_mnu_ctr
import controller.file_mnu_ctr
from dialogs.dialog_edit_game_data import DialogEditGameData

class GameMenuController():

    def __init__(self, mainAppWindow, model):
        super(GameMenuController, self).__init__()
        self.mainAppWindow = mainAppWindow
        self.uci_controller = mainAppWindow.engine_controller
        self.model = model

    def on_newgame(self, mainWindow):
        settings = mainWindow.model.user_settings
        db = mainWindow.model.database
        ret = self.unsaved_changes(mainWindow)
        if not ret == QMessageBox.Cancel:
            dialog = DialogNewGame(gamestate=mainWindow.gs,user_settings=settings)
            movesEdit = mainWindow.moves_edit_view
            if dialog.exec_() == QDialog.Accepted:
                mainWindow.model.gamestate = GameState()
                mainWindow.chessboard_view.gs = mainWindow.model.gamestate
                movesEdit.gs = mainWindow.gs
                movesEdit.update()
                # strength is only changed if internal engine is used
                # otherwise the dialog is meaningless
                if(settings.active_engine == settings.engines[0]):
                    mainWindow.model.gamestate.strength_level = dialog.slider_elo.value()
                mainWindow.model.gamestate.computer_think_time = dialog.think_ms
                #print("think time: "+str(mainWindow.gs.computer_think_time))
                movesEdit.on_statechanged()
                mainWindow.model.gamestate.initialize_headers()
                #mainWindow.save.setEnabled(False)
                # add current game to database, but don't save it
                mainWindow.model.database.index_current_game = None
                mainWindow.save.setEnabled(True)
                #mainWindow.database.add_current_game(mainWindow.gs.game.root())
                if(dialog.rb_plays_white.isChecked()):
                    mainWindow.play_white.setChecked(True)
                    mainWindow.setLabels()
                    self.on_play_as_white(mainWindow)
                else:
                    mainWindow.play_black.setChecked(True)
                    root = mainWindow.model.gamestate.current.root()
                    temp = root.headers["White"]
                    root.headers["White"] = root.headers["Black"]
                    root.headers["Black"] = temp
                    mainWindow.setLabels()
                    self.on_play_as_black(mainWindow)

    def save(self):
        mainWidget = self.mainAppWindow
        db = mainWidget.model.database
        # if the game is not in the database
        # i.e. hasn't been saved yet, then
        # calls save_as
        if(db.index_current_game == None):
            self.save_as_new()
        else:
            db.update_game(db.index_current_game,mainWidget.model.gamestate.current)
        mainWidget.save.setEnabled(False)
        mainWidget.moves_edit_view.setFocus()

    def save_as_new(self):
        mainWidget = self.mainAppWindow
        db = mainWidget.model.database
        gs = mainWidget.model.gamestate
        # let the user enter game data
        controller.edit.editGameData(mainWidget)
        db.append_game(gs.current)
        mainWidget.save.setEnabled(False)
        mainWidget.moves_edit_view.setFocus()

    def export_game(self):
        mainWidget = self.mainAppWindow
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

    def editGameData(self):
        mainWindow = self.mainAppWindow
        root = mainWindow.model.gamestate.current.root()
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

    def on_nextgame(self, mainWindow):
        db = mainWindow.model.database
        gs = mainWindow.model.gamestate
        cbv = mainWindow.chessboard_view
        if(not db.index_current_game == None):
            if(db.index_current_game < len(db.entries)-1):
                ret = self.unsaved_changes(mainWindow)
                if not ret == QMessageBox.Cancel:
                    loaded_game = db.load_game(db.index_current_game+1)
                    gs.current = loaded_game
                    cbv.update()
                    cbv.emit(SIGNAL("statechanged()"))
                    gs.unsaved_changes = False
                    mainWindow.save.setEnabled(False)
                    mainWindow.setLabels()
                    mainWindow.moves_edit_view.setFocus()
                    controller.file_mnu_ctr.init_game_tree(mainWindow,gs.current.root())


    def on_previous_game(self, mainWindow):
        db = mainWindow.model.database
        gs = mainWindow.model.gamestate
        cbv = mainWindow.chessboard_view
        if(not db.index_current_game == None):
            if(db.index_current_game > 0):
                ret = self.unsaved_changes(mainWindow)
                if not ret == QMessageBox.Cancel:
                    loaded_game = db.load_game(db.index_current_game-1)
                    gs.current = loaded_game
                    cbv.update()
                    cbv.emit(SIGNAL("statechanged()"))
                    gs.unsaved_changes = False
                    mainWindow.save.setEnabled(False)
                    mainWindow.setLabels()
                    mainWindow.moves_edit_view.setFocus()
                    controller.file_mnu_ctr.init_game_tree(mainWindow,gs.current.root())



###########


    def unsaved_changes(self, mainWindow):
        print("unsaved changes")
        print(str(mainWindow.model.gamestate.unsaved_changes))
        print(str(mainWindow.model.database.index_current_game))
        # dialog to be called to
        # check for unsaved changes to
        # the current game
        # if current game has unsaved changes
        # or is not saved in database
        # ask user if he wants save it
        if(mainWindow.model.database.index_current_game == None or
            mainWindow.model.gamestate.unsaved_changes):
            print("inside loop")
            dlg_changes = DialogSaveChanges()
            ret = dlg_changes.exec_()
            if(ret == QMessageBox.Save):
                # if game is not in db append
                if(mainWindow.model.database.index_current_game == None):
                    controller.edit_mnu_ctr.editGameData(mainWindow)
                    mainWindow.model.database.append_game(mainWindow.gs.current)
                else:
                    mainWindow.model.database.update_game(mainWindow.database.index_current_game,mainWindow.gs.current)
                mainWindow.save.setEnabled(False)
            return ret
        else:
            return QMessageBox.Discard







    def on_unsaved_changes(self, mainWindow):
        mainWindow.model.gamestate.unsaved_changes = True
        mainWindow.save.setEnabled(True)

    def on_statechanged(self, mainWindow):
        mainWindow.save.setEnabled(True)
        gs = mainWindow.model.gamestate
        engine = mainWindow.engine_controller
        if(gs.mode == MODE_ANALYSIS):
            fen, uci_string = mainWindow.model.gamestate.printer.to_uci(gs.current)
            engine.send_fen(fen)
            engine.uci_send_position(uci_string)
            engine.uci_go_infinite()
        if((gs.mode == MODE_PLAY_WHITE and gs.current.board().turn == chess.BLACK) or
               (gs.mode == MODE_PLAY_BLACK and gs.current.board().turn == chess.WHITE) or
               (gs.mode == MODE_GAME_ANALYSIS)):
            fen, uci_string = gs.printer.to_uci(gs.current)
            engine.send_fen(fen)
            engine.uci_send_position(uci_string)
            engine.uci_go_movetime(gs.computer_think_time)










    def on_checkmate(self, mainWindow):
        root = mainWindow.model.gamestate.current.root()
        if(root.board().turn == chess.WHITE):
            root.headers["Result"] = "1-0"
        else:
            root.headers["Result"] = "0-1"
        self.on_enter_moves_mode(mainWindow)

    def draw_game(self, mainWindow):
        mainWindow.model.gamestate.current.root().headers["Result"] = "1/2-1/2"
        self.on_enter_moves_mode(mainWindow)


    def receive_engine_info(self, mainWindow,info_string):
        gs = mainWindow.model.gamestate
        engine_info = info_string
        if(gs.display_engine_info):
            gs.score = engine_info.score
            if(engine_info.pv_arr):
                gs.pv = engine_info.pv_arr
            gs.mate_threat = engine_info.mate
            mainWindow.engineOutput.setHtml(str(info_string))

    def count_moves(self, node):
        temp = node
        i = 0
        while(temp.parent != None):
            temp = temp.parent
            i = i+1
        return i

    def handle_offered_draw(self, mainWindow):
        gs = mainWindow.model.gamestate
        if((   (gs.mode == MODE_PLAY_WHITE and gs.score >  1.1)
            or (gs.mode == MODE_PLAY_BLACK and gs.score < -1.1))
            and self.count_moves(mainWindow.gs.current) > 40):
            gs.current.root().headers["Result"] = "1/2-1/2"
            display_mbox("The computer accepts.","The game ends in a draw.")
            self.on_enter_moves_mode(mainWindow)
        else:
            display_mbox("The computer rejects your offer.","The game continues.")

    def on_player_resigns(self, mainWindow):
        display_mbox("The computer thanks you.","Better luck next time!")
        gs = mainWindow.model.gamestate
        if(gs.mode == MODE_PLAY_WHITE):
            gs.current.root().headers["Result"] = "0-1"
        elif(gs.mode == MODE_PLAY_BLACK):
            gs.current.root().headers["Result"] = "1-0"
        self.on_enter_moves_mode(mainWindow)


    def add_variant_from_pv(self, root, move, uci_list):
        uci_move = uci_list[0]
        root.add_variation(chess.Move.from_uci(uci_move))
        # enforce repainting main variation if exists
        if(len(root.variations)>0 and len(root.variations[0].variations)>0):
            root.variations[0].invalidate = True
            root.variations[0].variations[0].invalidate = True
        root = root.variations[1]
        for i in range(1,len(uci_list)):
            root.add_main_variation(chess.Move.from_uci(uci_list[i]))
            root = root.variations[0]

    def give_up_game(self, mainWindow):
        gs = mainWindow.model.gamestate
        if(gs.mode == MODE_PLAY_WHITE):
            gs.headers["Result"] = "1-0"
        elif(gs.mode == MODE_PLAY_BLACK):
            gs.headers["Result"] = "0-1"
        self.on_enter_moves_mode(mainWindow)

    def is_lost_by_comp(self, gamestate):
        # if the position is played out (i.e. comp vs comp)
        # then never give up
        if(gamestate.mode == MODE_PLAYOUT_POS):
            return False
        if(gamestate.score == None):
            return False
        # otherwise, if we are in a play-state, first update
        # the current gamestate
        if((gamestate.mode == MODE_PLAY_BLACK and gamestate.score < -7.0) or
           (gamestate.mode == MODE_PLAY_WHITE and gamestate.score > 7.0)):
            gamestate.position_bad += 1
        else:
            gamestate.position_bad = 0
        # if the human player has a position that is for five consecutive
        # moves better than 7.0 (or -7.0), then the computer gives up
        if((gamestate.mode == MODE_PLAY_BLACK or gamestate.mode == MODE_PLAY_WHITE)
           and gamestate.position_bad > 5):
            return True
        else:
            return False


    def exists_better_line(self, gs):
        # there is a better line if:
        # a) we actually have a best_score information from
        #    analysing the next position (this is not true at the leaf node of a game tree)
        #    and a pv line from the current position
        # b) either the score after the next move is worse (threshold) than the current eval
        #    or a mate was missed
        # c) there is a next move (i.e. not a leaf node) and the played move is not the
        #    start move of the best line
        #
        return gs.best_score and gs.pv != [] \
                and (abs(gs.score - gs.best_score) > gs.analysis_threshold
                    or (gs.mate_threat == None and gs.next_mate_threat != None)) \
                and gs.current.variations != [] and gs.pv[0] != gs.current.variations[0].move.uci()


    def on_bestmove(self, mainWindow,move):
        gs = mainWindow.model.gamestate
        mode = mainWindow.model.gamestate.mode

        # handling a best move in playing mode (either human vs comp or comp vs comp)
        if((mode == MODE_PLAY_BLACK and gs.current.board().turn == chess.WHITE) or
           (mode == MODE_PLAY_WHITE and gs.current.board().turn == chess.BLACK) or
           (mode == MODE_PLAYOUT_POS)):
            if(self.is_lost_by_comp(gs)):
                display_mbox("The computer resigns.","Congratulations!")
                self.give_up_game(mainWindow)
            else:
                # continue normal play
                uci = move
                legal_moves = gs.current.board().legal_moves
                # test if engine move is actually legal (this should
                # always be true, unless there is some serious sync
                # issue between engine and views)
                if (len([x for x in legal_moves if x.uci() == uci]) > 0):
                    mainWindow.chessboard_view.executeMove(move)
                    mainWindow.chessboard_view.on_statechanged()
        # handling bestmove command if in analysis mode
        if(mode == MODE_GAME_ANALYSIS):
            if(self.exists_better_line(gs)):
                self.add_variant_from_pv(gs.current,move,gs.pv)
                if(gs.next_mate_threat != None):
                    gs.current.variations[0].comment = "#"+str(gs.next_mate_threat)
                else:
                    gs.current.variations[0].comment = str(gs.best_score)
                gs.current.variations[1].comment = str(gs.score)
            # record current evaluations
            gs.best_score = gs.score
            gs.pv = []
            gs.next_mate_threat = gs.mate_threat
            gs.mate_threat = None

            mainWindow.chessboard_view.on_statechanged()
            mainWindow.moves_edit_view.on_statechanged()

            # continue in that mode, unless we reached the root of
            # the game, or the parent position is in the book
            if(gs.current.parent):
                if(self.is_position_in_book(gs.current.parent)):
                    gs.current.parent.comment = "last book move"
                    gs.current.parent.invalidate = True
                    display_mbox(mainWindow.trUtf8("Game Analysis Finished","The analysis is finished."))
                    self.on_enter_moves_mode(mainWindow)
                else:
                    gs.current = gs.current.parent
                    # send uci best move command
                    self.on_statechanged(mainWindow)
            else:
                gs.mode = MODE_ENTER_MOVES
                display_mbox(mainWindow.trUtf8("Game Analysis Finished","The analysis is finished."))
                self.on_enter_moves_mode(mainWindow)
                # (finished, display messagebox)
            mainWindow.moves_edit_view.update_san()
            mainWindow.update()














    def save_db_as_new(self):
        mainWidget = self.mainAppWindow
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





    def save_to_db(self):
        mainWindow = self.mainAppWindow
        mainWindow.database.add_current_game(mainWindow.gs.game.root())
        mainWindow.save_in_db.setEnabled(False)
