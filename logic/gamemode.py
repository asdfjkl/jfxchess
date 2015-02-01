from dialogs.DialogStrengthLevel import DialogStrengthLevel
from dialogs.DialogNewGame import DialogNewGame
from PyQt4.QtGui import QDialog
from logic.gamestate import GameState
from logic.gamestate import MODE_ENTER_MOVES, MODE_PLAY_WHITE, MODE_PLAY_BLACK
import chess

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
    print("exec dialog")
    print("think time of gamestate"+str(mainWindow.gs.computer_think_time))
    movesEdit = mainWindow.movesEdit
    if dialog.exec_() == QDialog.Accepted:
        mainWindow.gs = GameState()
        mainWindow.board.gs = mainWindow.gs
        movesEdit.gs = mainWindow.gs
        movesEdit.update()
        mainWindow.gs.strength_level = dialog.strength
        mainWindow.gs.computer_think_time = dialog.think_ms
        print("dialog elo:"+str(dialog.strength))
        print("think time:"+str(dialog.think_ms))
        print("calling update board")
        movesEdit.on_statechanged()
        mainWindow.gs.initialize_headers()
        print("BOARD UPDATED")
        if(dialog.rb_plays_white.isChecked()):
            print("plays white")
            mainWindow.play_white.setChecked(True)
            mainWindow.setLabels()
            on_play_as_white(mainWindow)
        else:
            print("plays black")
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
        mainWindow.engine.stop_engine()
        mainWindow.engineOutput.setHtml("")
        mainWindow.engine.start_engine(mainWindow.engine_fn)
        mainWindow.engine.uci_ok()
        mainWindow.engine.uci_newgame()
        mainWindow.give_up.setEnabled(True)
        mainWindow.offer_draw.setEnabled(True)
        mainWindow.engine.uci_strength(mainWindow.gs.strength_level)
        print("MOVE : "+str(mainWindow.gs.current.board().turn == chess.BLACK))
        if(mainWindow.gs.current.board().turn == chess.WHITE):
            print("white to move")
            uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
            mainWindow.engine.uci_send_position(uci_string)
            mainWindow.engine.uci_go_movetime(mainWindow.gs.computer_think_time)

def on_play_as_white(mainWindow):
    mainWindow.board.flippedBoard = False
    mainWindow.board.on_statechanged()
    mainWindow.gs.mode = MODE_PLAY_WHITE
    mainWindow.engine.stop_engine()
    mainWindow.engineOutput.setHtml("")
    mainWindow.engine.start_engine(mainWindow.engine_fn)
    mainWindow.engine.uci_ok()
    mainWindow.engine.uci_newgame()
    mainWindow.give_up.setEnabled(True)
    mainWindow.offer_draw.setEnabled(True)
    mainWindow.engine.uci_strength(mainWindow.gs.strength_level)
    print("MOVE : "+str(mainWindow.gs.current.board().turn))
    if(mainWindow.gs.current.board().turn == chess.BLACK):
        uci_string = mainWindow.gs.printer.to_uci(mainWindow.gs.current)
        mainWindow.engine.uci_send_position(uci_string)
        mainWindow.engine.uci_go_movetime(mainWindow.gs.computer_think_time)
