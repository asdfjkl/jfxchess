import os,sys,json
from chess.uci import popen_engine
import configparser
import ast
from util.platform import get_platform_wordsize

class UserSettings():
    def __init__(self):
        super(UserSettings, self).__init__()

        self.engines = []
        self.active_engine = None
        self.active_database = None

    def save_to_file(self, absolute_filename):
        config = configparser.ConfigParser()
        config['General'] = {}
        if(not self.active_engine == None):
            config['General']['active_engine']=str(self.engines.index(self.active_engine))
        if(not self.active_database == None):
            config['General']['active_database']=str(self.active_database)
        for i, engine in enumerate(self.engines):
            tag = 'Engine'+str(i)
            config[tag] = {}
            config[tag]['Name'] = str(engine.name)
            config[tag]['Path'] = str(engine.path)
            for i,(option_name,option_type,val) in enumerate(engine.options):
                # skip first one, which is the internal engine
                if(i>0):
                    s_i = str(i).zfill(2)
                    config[tag]['option'+s_i]=str(option_name)+"\_/"+str(option_type)+"\_/"+str(val)
        with open(absolute_filename,"w") as f:
            config.write(f)

    def load_from_file(self, absolute_filename):
        config = configparser.ConfigParser()
        idx_active = 0
        self.engines = []
        self.engines.append(InternalEngine())
        try:
            config.read(absolute_filename)
        except configparser.Error as e:
            print(e)
            pass
        for section in config.sections():
            print(section)
            if section == 'General':
                print("section is general")
                print(str(config[section]))
                for key in config[section]:
                    if key == 'active_engine':
                        idx_active = int(config[section]['active_engine'])
                    if key == 'active_database':
                        self.active_database = str(config[section]['active_database'])
            if str(section).startswith('Engine'):
                print("starts with engine")
                e = Engine()
                e.name = str(config[section]['Name'])
                e.path = str(config[section]['Path'])
                for key in config[section]:
                    print(key)
                    if(str(key).startswith('option')):
                        print("key starts with option")
                        opt = config[section][key].split('\_/')
                        print(opt)
                        try:
                            if(len(opt) == 3):
                                if(opt[1] == 'spin'):
                                    e.options.append((opt[0],opt[1],int(opt[2])))
                                elif(opt[1] == 'string'):
                                    e.options.append((opt[0],opt[1],opt[2]))
                                elif(opt[1] == 'check'):
                                    print(ast.literal_eval(opt[2]))
                                    e.options.append((opt[0],opt[1],ast.literal_eval(opt[2])))
                        except BaseException as err:
                            print(err)
                            pass
                self.engines.append(e)
        if(len(self.engines) == 0):
            self.engines.append(InternalEngine())
        if(idx_active < len(self.engines)):
            self.active_engine = self.engines[idx_active]
        else:
            self.active_engine = self.engines[0]


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
        for (option_name,option_type,val) in self.options:
            if option_name == opt_name:
                return True
        return False

    def get_option_value(self,opt_name):
        for (option_name,option_type,val) in self.options:
            if option_name == opt_name:
                return val
        raise ValueError("There is no defined option for this option name!")

class InternalEngine(Engine):
    def __init__(self):
        super(InternalEngine, self).__init__()
        self.name = "Stockfish"
        # path
        self.path = os.path.dirname(os.path.realpath(sys.argv[0]))
        # get filename of engine depending on os
        platform, wordsize = get_platform_wordsize()
        if(platform == 'win32'):
            self.path += "/engine/stockfish_" + platform + "_" + str(wordsize)+".exe"
        else:
            self.path += "/engine/stockfish_" + platform + "_" + str(wordsize)
        print(self.path)
        self.options = []

        """
        eng = popen_engine(self.path)
        command = eng.uci(async_callback=True)
        command.result(timeout=1.5)
        self.name = eng.name
        """
