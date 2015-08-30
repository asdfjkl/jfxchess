__author__ = 'user'
import model.gamestate as gs
import model.user_settings as us
import util.appdirs as ad
import pickle
from model.database import Database
import os

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
        appname = 'jerry200'
        appauthor = 'dkl'
        fn = ad.user_data_dir(appname, appauthor)
        save_dir = fn
        try:
            with open(fn+"/current.raw","rb") as pgn:
                gamestate = pickle.load(pgn)
                gamestate.mode = gs.MODE_ENTER_MOVES
            pgn.close()
        except BaseException as e:
            print(e)
            pass
        try:
            with open(fn+"/settings.raw","rb") as f_setting:
                user_settings = pickle.load(f_setting)
            f_setting.close()
        except BaseException as e:
            print(e)
            pass
        try:
            with open(fn+"/db_idx.raw","rb") as f_database:
                db = pickle.load(f_database)
                print("db loaded")
                if(db.is_consistent()):
                    print("db is consistent")
                    database = db
                else:
                    print("db is not consistent")
                    db.init_from_file(parentWidget,parentWidget.trUtf8("PGN changed on disk - rescanning..."))
                    database = db
            f_database.close()
        except BaseException as e:
            print(e)
            pass

        default_db_path = fn + "/mygames.pgn"
        # if a db could be not be recovered create default one
        if(database == None):
            # if user has no database, create new
            # one. current game is saved as first entry
            database = Database(default_db_path)
            database.create_new_pgn()
            #self.database.add_game(self.gs.game.root())

        print("databse current index" + str(database.index_current_game))

        return Model(gamestate,database,user_settings, save_dir)

    # TODO: write proper serialization with JSON
    # of whole model (instead of three separate files
    def dump(self):
        try:
            if not os.path.exists(self.save_dir):
                os.makedirs(self.save_dir)
            with open(self.save_dir+"/current.raw",'wb') as f:
                pickle.dump(self.gamestate,f)
            f.close()
            with open(self.save_dir+"/settings.raw","wb") as f:
                pickle.dump(self.user_settings,f)
            f.close()
            with open(self.save_dir+"/db_idx.raw","wb") as f:
                pickle.dump(self.database,f)
            f.close()
        except BaseException as e:
            print(e)
            pass
