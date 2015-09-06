import os,sys,json
from chess.uci import popen_engine


class UserSettings():
    def __init__(self):
        super(UserSettings, self).__init__()

        self.engines = []
        self.active_engine = None
        self.active_database = None

    def __str__(self):
        s = ""
        for engine in self.engines:
            s += str(engine)
        s += "[General]\n"
        if(not self.active_engine == None):
            s += "active_engine="+str(self.engines.index(self.active_engine))+"\n"
        if(not self.active_database == None):
            s += "active_database="+str(self.active_database)+"\n"
        return s

    def load_from_file(self, absolute_filename):
        PARSE_GENERAL = "parse_general"
        PARSE_ENGINE = "parse_engine"
        engines = []
        mode = None
        current_engine = None
        with open(absolute_filename) as f:
            for line in f:
                setting_value = line.rstrip().split('=')
                if(mode == None and setting_value[0].startswith("[General]")):
                    mode = PARSE_GENERAL
                if(mode == None and setting_value[0].startswith("[Engine]")):
                    mode = PARSE_ENGINE
                    current_engine = Engine()
                if(mode == PARSE_GENERAL):
                    if(setting_value[0].startswith("active_engine")):
                        self.active_engine = setting_value[1]
                    elif(setting_value[0].startswith("active_engine")):
                        pass


        f.close()

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

    def __str__(self):
        s = "[Engine]\n"
        s += "Name="+str(self.name)+"\n"
        s += "Path="+str(self.path)+"\n"
        for (opt,val) in self.options:
            s += str(opt)+"="+str(val)+"\n"
        print(self.options)
        return s

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