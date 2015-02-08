from dialogs.DialogStrengthLevel import DialogStrengthLevel
from dialogs.DialogNewGame import DialogNewGame
from dialogs.DialogAnalyzeGame import DialogAnalyzeGame
from PyQt4.QtGui import QDialog, QMessageBox
from logic.gamestate import GameState
from logic.gamestate import MODE_ENTER_MOVES, \
    MODE_PLAY_WHITE, MODE_PLAY_BLACK, MODE_ANALYSIS, \
    MODE_GAME_ANALYSIS, MODE_PLAYOUT_POS
import chess
from logic.file_io import is_position_in_book

def on_strength_level(mainWindow):
    gamestate = mainWindow.gs
    engine = mainWindow.engine
    dialog = DialogStrengthLevel(gamestate=gamestate)
    if dialog.exec_() == QDialog.Accepted:
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
    if(not gamestate.mode == MODE_ENTER_MOVES):
        engine.uci_strength(gamestate.strength_level)


def on_newgame(mainWindow):
    dialog = DialogNewGame(gamestate=mainWindow.gs)
    movesEdit = mainWindow.movesEdit
    if dialog.exec_() == QDialog.Accepted:
        mainWindow.gs = GameState()
        mainWindow.board.gs = mainWindow.gs
        movesEdit.gs = mainWindow.gs
        movesEdit.update()
        mainWindow.gs.strength_level = dialog.strength
        mainWindow.gs.computer_think_time = dialog.think_ms
        movesEdit.on_statechanged()
        mainWindow.gs.initialize_headers()
        if(dialog.rb_plays_white.isChecked()):
            mainWindow.play_white.setChecked(True)
            mainWindow.setLabels()
            on_play_as_white(mainWindow)
        else:
            mainWindow.play_black.setChecked(True)
            root = mainWindow.gs.current.root()
            temp = root.headers["White"]
            root.headers["White"] = root.headers["Black"]
            root.headers["Black"] = temp
            mainWindow.setLabels()
            on_play_as_black(mainWindow)

def on_play_as_black(mainWindow):
        mainWindow.board.flippedBoard = True
        mainWindow.board.on_statechanged()
        mainWindow.gs.mode = MODE_PLAY_BLACK
        mainWindow.engineOutput.setHtml("")
        reset_engine(mainWindow.engine,mainWindow.engine_fn)
        mainWindow.give_up.setEnabled(True)
        mainWindow.offer_draw.setEnabled(True)
        mainWindow.engine.uci_strength(mainWindow.gs.strength_level)
        if(mainWindow.gs.current.board().turn == chess.WHITE):
            uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
            mainWindow.engine.uci_send_position(uci_string)
            mainWindow.engine.uci_go_movetime(mainWindow.gs.computer_think_time)

def on_play_as_white(mainWindow):
    mainWindow.board.flippedBoard = False
    mainWindow.board.on_statechanged()
    mainWindow.gs.mode = MODE_PLAY_WHITE
    reset_engine(mainWindow.engine,mainWindow.engine_fn)
    mainWindow.engineOutput.setHtml("")
    mainWindow.give_up.setEnabled(True)
    mainWindow.offer_draw.setEnabled(True)
    mainWindow.engine.uci_strength(mainWindow.gs.strength_level)
    if(mainWindow.gs.current.board().turn == chess.BLACK):
        uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
        mainWindow.engine.uci_send_position(uci_string)
        mainWindow.engine.uci_go_movetime(mainWindow.gs.computer_think_time)

def on_statechanged(mainWindow):
    gs = mainWindow.gs
    engine = mainWindow.engine
    if(gs.mode == MODE_ANALYSIS):
        uci_string = mainWindow.gs.printer.to_uci(gs.current)
        engine.uci_send_position(uci_string)
        engine.uci_go_infinite()
    if((gs.mode == MODE_PLAY_WHITE and gs.current.board().turn == chess.BLACK) or
           (gs.mode == MODE_PLAY_BLACK and gs.current.board().turn == chess.WHITE) or
           (gs.mode == MODE_GAME_ANALYSIS)):
        uci_string = gs.printer.to_uci(gs.current)
        engine.uci_send_position(uci_string)
        engine.uci_go_movetime(gs.computer_think_time)

def on_analysis_mode(mainWindow):
    mainWindow.display_info.setChecked(True)
    mainWindow.set_display_info()
    reset_engine(mainWindow.engine,mainWindow.engine_fn)
    mainWindow.give_up.setEnabled(False)
    mainWindow.offer_draw.setEnabled(False)
    uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
    mainWindow.engine.uci_send_position(uci_string)
    mainWindow.engine.uci_go_infinite()
    mainWindow.gs.mode = MODE_ANALYSIS

def on_game_analysis_mode(mainWindow):
    gs = mainWindow.gs
    dialog = DialogAnalyzeGame(gamestate=gs)
    if dialog.exec_() == QDialog.Accepted:
        gs.computer_think_time = dialog.sb_secs.value()
        gs.analysis_threshold = dialog.sb_threshold.value()
        print("think time: "+str(gs.computer_think_time))
        mainWindow.display_info.setChecked(True)
        mainWindow.set_display_info()
        reset_engine(mainWindow.engine,mainWindow.engine_fn)
        mainWindow.give_up.setEnabled(False)
        mainWindow.offer_draw.setEnabled(False)
        mainWindow.movesEdit.delete_all_comments()
        mainWindow.movesEdit.delete_all_variants()
        gs.best_score = None
        gs.best_pv = []
        gs.mate_threat = False
        while(len(gs.current.variations) > 0):
            gs.current = gs.current.variations[0]
        if(gs.current.board().is_checkmate() or gs.current.board().is_stalemate()):
            gs.current = gs.current.parent
        uci_string = gs.printer.to_uci(gs.current)
        mainWindow.engine.uci_send_position(uci_string)
        mainWindow.engine.uci_go_movetime(3000)
        gs.mode = MODE_GAME_ANALYSIS

def on_enter_moves_mode(mainWindow):
    # stop any engine
    mainWindow.engine.stop_engine()
    mainWindow.engineOutput.setHtml("")
    mainWindow.give_up.setEnabled(False)
    mainWindow.offer_draw.setEnabled(False)
    mainWindow.gs.mode = MODE_ENTER_MOVES

def on_playout_pos(mainWindow):
    mainWindow.display_info.setChecked(True)
    mainWindow.set_display_info()
    reset_engine(mainWindow.engine,mainWindow.engine_fn)
    mainWindow.give_up.setEnabled(False)
    mainWindow.offer_draw.setEnabled(False)
    uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
    mainWindow.engine.uci_send_position(uci_string)
    mainWindow.engine.uci_go_movetime(mainWindow.gs.computer_think_time)
    mainWindow.gs.mode = MODE_PLAYOUT_POS
    mainWindow.enter_moves.setChecked(False)
    #self.analysis.setChecked(False)
    mainWindow.play_white.setChecked(False)
    mainWindow.play_black.setChecked(False)


def reset_engine(engine,fn):
    engine.stop_engine()
    engine.start_engine(fn)
    engine.uci_ok()
    engine.uci_newgame()

def on_checkmate(mainWindow):
    root = mainWindow.gs.current.root()
    if(root.current.board().turn == chess.WHITE):
        root.headers["Result"] = "1-0"
    else:
        root.headers["Result"] = "0-1"
    mainWindow.enter_moves.setChecked(True)
    on_enter_moves_mode(mainWindow)

def draw_game(mainWindow):
    mainWindow.gs.headers["Result"] = "1/2-1/2"
    mainWindow.enter_moves.setChecked(True)
    on_enter_moves_mode(mainWindow)

def receive_engine_info(mainWindow,engine_info):
    gs = mainWindow.gs
    if(gs.display_engine_info):
        if(engine_info.score != None):
            if(engine_info.flip_eval and engine_info.score != 0.0):
                gs.score = - engine_info.score
            else:
                gs.score = engine_info.score
        if(engine_info.pv_arr):
            gs.pv = engine_info.pv_arr
        gs.mate_threat = None
        if(engine_info.mate != None):
            gs.mate_threat = engine_info.mate
        mainWindow.engineOutput.setHtml(str(engine_info))

def count_moves(node):
    temp = node
    i = 0
    while(temp.parent != None):
        print("has parent")
        temp = temp.parent
        i = i+1
    return i

def handle_offered_draw(mainWindow):
    gs = mainWindow.gs
    print("depth: "+str(count_moves(mainWindow.gs.current)))
    if((   (gs.mode == MODE_PLAY_WHITE and gs.score >  1.1)
        or (gs.mode == MODE_PLAY_BLACK and gs.score < -1.1))
        and count_moves(mainWindow.gs.current) > 40):
        gs.current.root().headers["Result"] = "1/2-1/2"
        msgBox = QMessageBox()
        msgBox.setText("The computer accepts.")
        msgBox.setInformativeText("The game ends in a draw.")
        msgBox.exec_()
        mainWindow.enter_moves.setChecked(True)
        mainWindow.on_enter_moves_mode()
    else:
        msgBox = QMessageBox()
        msgBox.setText("The computer rejects your offer.")
        msgBox.setInformativeText("The game continues.")
        msgBox.exec_()

def on_player_resigns(mainWindow):
    msgBox = QMessageBox()
    msgBox.setText("The computer thanks you.")
    msgBox.setInformativeText("Better luck next time!")
    msgBox.exec_()
    gs = mainWindow.gs
    if(gs.mode == MODE_PLAY_WHITE):
        gs.current.root().headers["Result"] = "0-1"
    elif(gs.mode == MODE_PLAY_BLACK):
        gs.current.root().headers["Result"] = "1-0"
    on_enter_moves_mode(mainWindow)


def add_variant_from_pv(root, uci_list):
    uci_move = uci_list[0]
    print("UCI MOVE:" + str(uci_move))
    root.add_variation(chess.Move.from_uci(uci_move))
    root = root.variations[1]
    for i in range(1,len(uci_list)):
        print("UCI MOVE:" + str(uci_list[i]))
        root.add_main_variation(chess.Move.from_uci(uci_list[i]))
        root = root.variations[0]

def give_up_game(mainWindow):
    gs = mainWindow.gs
    if(gs.mode == MODE_PLAY_WHITE):
        gs.headers["Result"] = "1-0"
    elif(gs.mode == MODE_PLAY_BLACK):
        gs.headers["Result"] = "0-1"
    mainWindow.enter_moves.setChecked(True)
    on_enter_moves_mode(mainWindow)

def is_lost_by_comp(gamestate):
    # if the position is played out (i.e. comp vs comp)
    # then never give up
    if(gamestate.mode == MODE_PLAYOUT_POS):
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


def exists_better_line(gs):
    # there is a better line if:
    # a) we actually have a best_score and best_pv information from
    #    analysing the next position (this is not true at the leaf node of a game tree)
    # b) either the score after the next move is worse (threshold) than the current eval
    #    or a mate was missed
    # c) there is a next move (i.e. not a leaf node) and the played move is not the
    #    start move of the best line
    #
    return gs.best_score and gs.best_pv != [] \
            and (gs.score - gs.best_score > gs.analysis_threshold
                or (gs.mate_threat == None and gs.next_mate_threat != None)) \
            and gs.current.variations != [] and gs.best_pv[0] != gs.current.variations[0].move.uci()


def on_bestmove(mainWindow,move):
    print("bestmove received: "+str(move))
    gs = mainWindow.gs
    mode = mainWindow.gs.mode

    # handling a best move in playing mode (either human vs comp or comp vs comp)
    if((mode == MODE_PLAY_BLACK and gs.current.board().turn == chess.WHITE) or
       (mode == MODE_PLAY_WHITE and gs.current.board().turn == chess.BLACK) or
       (mode == MODE_PLAYOUT_POS)):
        if(is_lost_by_comp(gs)):
            msgBox = QMessageBox()
            msgBox.setText("The computer resigns.")
            msgBox.setInformativeText("Congratulations!")
            msgBox.exec_()
            give_up_game(mainWindow)
        else:
            # continue normal play
            uci = move
            legal_moves = gs.current.board().legal_moves
            # test if engine move is actually legal (this should
            # always be true, unless there is some serious sync
            # issue between engine and gui)
            if (len([x for x in legal_moves if x.uci() == uci]) > 0):
                mainWindow.board.executeMove(move)
                mainWindow.board.on_statechanged()
    # handling bestmove command if in analysis mode
    if(mode == MODE_GAME_ANALYSIS):
        if(exists_better_line(gs)):
            add_variant_from_pv(gs.current,gs.pv)
            if(gs.next_mate_threat != None):
                gs.current.variations[0].comment = "#"+str(gs.next_mate_threat)
            else:
                gs.current.variations[0].comment = str(gs.best_score)
            gs.current.variations[1].comment = str(gs.score)
        # record current evaluations
        gs.best_score = gs.score
        gs.best_pv = gs.pv
        gs.next_mate_threat = gs.mate_threat
        gs.mate_threat = None

        mainWindow.board.on_statechanged()
        mainWindow.movesEdit.on_statechanged()

        # continue in that mode, unless we reached the root of
        # the game, or the parent position is in the book
        if(gs.current.parent):
            if(is_position_in_book(gs.current.parent)):
                gs.current.parent.comment = "last book move"
                gs.mode = MODE_ENTER_MOVES
            else:
                gs.current = gs.current.parent
                # send uci best move command
                print("analysed, sending best move")
                on_statechanged(mainWindow)
        else:
            print("finished analysing")
            gs.mode = MODE_ENTER_MOVES
            # (finished, display messagebox)
        mainWindow.movesEdit.update_san()
