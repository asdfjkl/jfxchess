from model.gamestate import MODE_ENTER_MOVES,MODE_PLAY_BLACK,\
    MODE_ANALYSIS,MODE_PLAY_WHITE,MODE_PLAYOUT_POS,MODE_GAME_ANALYSIS
import chess
from util.messages import display_mbox
from dialogs.dialog_edit_game_data import DialogEditGameData
from dialogs.dialog_save_changes import DialogSaveChanges
from PyQt4.QtGui import *

class GamestateController():

    def __init__(self, mainAppWindow):
        super(GamestateController, self).__init__()
        self.mainAppWindow = mainAppWindow
        #self.uci_controller = mainAppWindow.engine_controller
        self.model = mainAppWindow.model

    def on_statechanged(self):
        #self.mainAppWindow.save.setEnabled(True)
        gs = self.mainAppWindow.model.gamestate
        engine = self.mainAppWindow.engine_controller
        if(gs.mode == MODE_ANALYSIS):
            fen, uci_string = self.mainAppWindow.model.gamestate.printer.to_uci(gs.current)
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
        self.on_enter_moves_mode()

    def draw_game(self):
        self.model.gamestate.current.root().headers["Result"] = "1/2-1/2"
        self.on_enter_moves_mode()

    def receive_engine_info(self, info_string):
        engine_info = info_string
        if(self.model.gamestate.display_engine_info):
            self.model.gamestate.score = engine_info.score
            if(engine_info.pv_arr):
                self.model.gamestate.pv = engine_info.pv_arr
            self.model.gamestate.mate_threat = engine_info.mate
            self.mainAppWindow.engineOutput.setHtml(str(info_string))

    def count_moves(self, node):
        temp = node
        i = 0
        while(temp.parent != None):
            temp = temp.parent
            i = i+1
        return i

    def handle_offered_draw(self):
        if( (not self.model.gamestate.score == None) and
            (   (self.model.gamestate.mode == MODE_PLAY_WHITE and self.model.gamestate.score >  1.1)
            or (self.model.gamestate.mode == MODE_PLAY_BLACK and self.model.gamestate.score < -1.1))
            and self.count_moves(self.model.gamestate.current) > 40):
            self.model.gamestate.current.root().headers["Result"] = "1/2-1/2"
            display_mbox("The computer accepts.","The game ends in a draw.")
            self.on_enter_moves_mode()
            self.mainAppWindow.moves_edit_view.update_san()
        else:
            display_mbox("The computer rejects your offer.","The game continues.")

    def on_player_resigns(self):
        display_mbox("The computer thanks you.","Better luck next time!")
        if(self.model.gamestate.mode == MODE_PLAY_WHITE):
            self.model.gamestate.current.root().headers["Result"] = "0-1"
        elif(self.model.gamestate.mode == MODE_PLAY_BLACK):
            self.model.gamestate.current.root().headers["Result"] = "1-0"
        self.on_enter_moves_mode()
        self.mainAppWindow.moves_edit_view.update_san()

    def on_enter_moves_mode(self):
        self.mainAppWindow.engine_controller.stop_engine()
        self.mainAppWindow.engineOutput.setHtml("")
        self.mainAppWindow.give_up.setEnabled(False)
        self.mainAppWindow.offer_draw.setEnabled(False)
        self.model.gamestate.mode = MODE_ENTER_MOVES
        self.mainAppWindow.enter_moves.setChecked(True)

    def add_variant_from_pv(self, root, uci_list):
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

    def give_up_game(self):
        if(self.model.gamestate.mode == MODE_PLAY_WHITE):
            self.model.gamestate.headers["Result"] = "1-0"
        elif(self.model.gamestate.mode == MODE_PLAY_BLACK):
            self.model.gamestate.headers["Result"] = "0-1"
        self.on_enter_moves_mode()

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


    def on_bestmove(self, move):
        gs = self.model.gamestate
        mode = self.model.gamestate.mode

        # handling a best move in playing mode (either human vs comp or comp vs comp)
        if((mode == MODE_PLAY_BLACK and gs.current.board().turn == chess.WHITE) or
           (mode == MODE_PLAY_WHITE and gs.current.board().turn == chess.BLACK) or
           (mode == MODE_PLAYOUT_POS)):
            if(self.is_lost_by_comp(gs)):
                display_mbox("The computer resigns.","Congratulations!")
                self.give_up_game()
            else:
                # continue normal play
                uci = move
                legal_moves = gs.current.board().legal_moves
                # test if engine move is actually legal (this should
                # always be true, unless there is some serious sync
                # issue between engine and views)
                if (len([x for x in legal_moves if x.uci() == uci]) > 0):
                    self.mainAppWindow.chessboard_view.executeMove(move)
                    self.mainAppWindow.chessboard_view.on_statechanged()
        # handling bestmove command if in game analysis mode
        if(mode == MODE_GAME_ANALYSIS):
            if(self.exists_better_line(gs) and
                   ((gs.game_analysis_white and gs.current.board().turn == chess.WHITE) or
                    (gs.game_analysis_black and gs.current.board().turn == chess.BLACK))):
                self.add_variant_from_pv(gs.current,gs.pv)
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

            self.mainAppWindow.chessboard_view.on_statechanged()
            self.mainAppWindow.moves_edit_view.on_statechanged()

            # continue in that mode, unless we reached the root of
            # the game, or the parent position is in the book
            if(gs.current.parent):
                if((gs.is_current_position_in_book(gs.current.parent))):
                    gs.current.parent.comment = "last book move"
                    gs.current.parent.invalidate = True
                    display_mbox(self.mainAppWindow.trUtf8("Game Analysis Finished"),\
                                 self.mainAppWindow.trUtf8("The analysis is finished."))
                    self.on_enter_moves_mode()
                else:
                    gs.current = gs.current.parent
                    # send uci best move command
                    self.on_statechanged()
            else:
                gs.mode = MODE_ENTER_MOVES
                display_mbox(self.mainAppWindow.trUtf8("Game Analysis Finished"),\
                             self.mainAppWindow.trUtf8("The analysis is finished."))
                self.on_enter_moves_mode()
                # (finished, display messagebox)
            self.mainAppWindow.moves_edit_view.update_san()
            self.mainAppWindow.update()


    def editGameData(self):
        root = self.model.gamestate.current.root()
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
            self.mainAppWindow.save.setEnabled(True)
            self.model.gamestate.unsaved_changes = True
        self.mainAppWindow.moves_edit_view.update_san()
        self.mainAppWindow.setLabels()

    def unsaved_changes(self):
        # dialog to be called to
        # check for unsaved changes to
        # the current game
        # if current game has unsaved changes
        # or is not saved in database
        # ask user if he wants save it
        #
        # returns
        # 1) QMessageBox.Discard if the user discarded
        # 2) QMessageBox.Cancel if the user canceled
        # 3) QMessageBox.Save if the user chose to save
        #              saves the game before returning in case 3)
        if(self.model.database.index_current_game == None or
               self.model.gamestate.unsaved_changes):
            dlg_changes = DialogSaveChanges()
            ret = dlg_changes.exec_()
            if(ret == QMessageBox.Save):
                # if game is not in db append
                if(self.model.database.index_current_game == None):
                    self.mainAppWindow.gameMenuController.editGameData()
                    self.model.database.append_game(self.model.gamestate.current)
                else:
                    self.model.database.update_game(self.model.database.index_current_game,self.model.gamestate.current)
                self.mainAppWindow.save.setEnabled(False)
            return ret
        else:
            return QMessageBox.Discard
