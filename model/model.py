__author__ = 'user'
import model.gamestate as gs
import model.user_settings as us
import util.appdirs as ad
from model.database import Database
import os
import chess

appname = 'jerry200'
appauthor = 'dkl'

class Model():

    def __init__(self, gamestate, database, user_settings, save_dir):
        super(Model, self).__init__()
        self.gamestate = gamestate
        self.database = database
        self.user_settings = user_settings
        self.save_dir = save_dir

    @classmethod
    def create_on_startup(cls, parentWidget):

        # default gamestate is just entering moves
        gamestate = gs.GameState()
        gamestate.mode = gs.MODE_ENTER_MOVES

        # default active engine is the internal one (Stockfish)
        user_settings = us.UserSettings()
        user_settings.engines.append(us.InternalEngine())
        user_settings.active_engine = user_settings.engines[0]

        database = None

        # if existing, recover game state, user settings, and
        # database that user used before exiting app last time
        # (by unpickling)
        # TODO: replace by proper JSON serialization
        fn = ad.user_data_dir(appname, appauthor)
        save_dir = fn
        try:
            with open(fn+"/current.pgn") as pgn:
                game = chess.pgn.read_game(pgn)
                gamestate.current = game
                gamestate.init_game_tree(mainAppWindow=parentWidget)
            pgn.close()
        except BaseException as e:
            print(e)
            pass
        user_settings.load_from_file(fn+"/settings.ini")
        if(not user_settings.active_database == None):
            database = Database(user_settings.active_database)
        else:
            default_db_path = fn + "/mygames.pgn"
            database = Database(default_db_path)

        return Model(gamestate,database,user_settings, save_dir)

    # TODO: write proper serialization with JSON
    # of whole model (instead of three separate files
    def dump(self):
        try:
            if not os.path.exists(self.save_dir):
                os.makedirs(self.save_dir)
            with open(self.save_dir+"/current.pgn",'w') as f:
                print(self.gamestate.current.root(), file=f)
            f.close()
            self.user_settings.save_to_file(self.save_dir+"/settings.ini")
            #self.database.dump_to_file(self.save_dir+"/database.ini")
            #self.database.save_to_file(self.save_dir+"/database.idx")
        except BaseException as e:
            print(e)
            pass
