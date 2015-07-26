import os,sys,json
from chess.uci import popen_engine


class UserSettings():
    def __init__(self):
        super(UserSettings, self).__init__()

        self.engines = []
        self.active_engine = None
        self.active_database = None

class Engine():
    def __init__(self):
        super(Engine, self).__init__()
        self.name = None
        self.path = None
        self.options = []

    """
    returns True if the option with
    opt_name exists among the stored
    options of the engine
    """
    def exists_option_value(self,opt_name):
        for (option,val) in self.options:
            if option.name == opt_name:
                return True
        return False

    def get_option_value(self,opt_name):
        for (option,val) in self.options:
            if option.name == opt_name:
                return val
        raise ValueError("There is no defined option for this option name!")

class InternalEngine(Engine):
    def __init__(self):
        super(InternalEngine, self).__init__()
        self.name = "Stockfish"
        # path
        self.path = os.path.dirname(os.path.realpath(sys.argv[0]))
        # get filename of engine depending on os
        if sys.platform == 'win32':
            self.path += "/engine/stockfish.exe"
        elif 'linux' in  sys.platform:
            self.path += "/engine/stockfish_linux"
        elif sys.platform == 'darwin':
            self.path += '/engine/stockfish_osx'
        #self.path = '"'+self.path+'"'
        self.options = []

        """
        eng = popen_engine(self.path)
        command = eng.uci(async_callback=True)
        command.result(timeout=1.5)
        self.name = eng.name
        """