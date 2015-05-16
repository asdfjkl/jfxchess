import os,sys

class Settings():
    def __init__(self):
        super(Settings, self).__init__()

        self.engines = []
        self.active_engine = None

class Engine():
    def __init__(self):
        super(Engine, self).__init__()
        self.name = None
        self.path = None
        self.options = []

class InternalEngine(Engine):
    def __init__(self):
        super(InternalEngine, self).__init__()
        self.name = "Stockfish (Internal)"
        # path
        self.path = os.path.dirname(os.path.realpath(sys.argv[0]))
        # get filename of engine depending on os
        if sys.platform == 'win32':
            self.path += "/engine/stockfish.exe"
        elif 'linux' in  sys.platform:
            self.path += "/engine/stockfish_linux"
        elif sys.platform == 'darwin':
            self.path += '/engine/stockfish_osx'
        self.path = '"'+self.path+'"'
        self.options = []