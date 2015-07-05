from util.crc32 import crc32_from_file
from PyQt4.QtGui import QApplication, QProgressDialog
import PyQt4
import os
import chess
import shutil
import pickle

class Entry():
    def __init__(self):
        super(Entry, self).__init__()

    @property
    def headers(self):
        return None

class EntryOnDisk(Entry):
    def __init__(self,_pgn_offset,_headers):
        super(EntryOnDisk, self).__init__()
        self.pgn_offset = _pgn_offset
        self._headers = _headers

    @property
    def headers(self):
        return self._headers

class EntryInMemory(Entry):
    def __init__(self,game_root):
        super(EntryInMemory, self).__init__()
        self.game_root = game_root

    @property
    def headers(self):
        return self.game_root.headers


class Database():

    def __init__(self, filename):
        super(Database, self).__init__()
        self.filename = filename
        self.entries = []
        self.checksum = None
        self.index_current_game = None
        self.unsaved_changes = False

    def create_new_pgn(self):
        filename = self.filename
        f = open(filename, 'w')
        f.close()
        self.checksum = crc32_from_file(self.filename)
        self.filename = filename

    def add_game(self,game_root):
        self.entries.append(EntryInMemory(game_root))
        self.unsaved_changes = True

    def add_current_game(self,game_root):
        self.entries.append(EntryInMemory(game_root))
        self.index_current_game = len(self.entries) - 1
        self.unsaved_changes = True

    def save_as_new(self,mainWindow, new_filename):
        if(new_filename == self.filename):
            self.save(mainWindow)
        else:
            shutil.copy(self.filename,new_filename)
            self.filename = new_filename
            self.save(mainWindow)


    def save(self, mainWindow):
        # create a new temp file
        if not self.is_consistent():
            raise IOError("database file has changed on disk - index is inconsistent")
        pDialog = QProgressDialog(mainWindow.trUtf8("Saving PGN File"),None,0,len(self.entries),mainWindow)
        pDialog.show()
        pDialog.setWindowModality(PyQt4.QtCore.Qt.WindowModal)
        QApplication.processEvents()
        with open(self.filename,'r') as pgn:
            with open(self.filename+"tmp",'w') as new_pgn:
                for idx, entry in enumerate(self.entries):
                    QApplication.processEvents()
                    if(type(entry)==EntryOnDisk):
                        pgn.seek(entry.pgn_offset)
                        game = chess.pgn.read_game(pgn)
                        print(game.root(), file=new_pgn, end="\n\n")
                    else:
                        print(entry.game_root.root(), file=new_pgn, end="\n\n")
                    pDialog.setValue(idx)
        shutil.copy(self.filename+"tmp",self.filename)
        # reparse the written file
        with open(self.filename) as pgn:
            size = os.path.getsize(self.filename)
            self.entries = []
            QApplication.processEvents()
            idx = 0
            for offset, headers in chess.pgn.scan_headers(pgn):
                pDialog.setValue(idx)
                QApplication.processEvents()
                self.entries.append(EntryOnDisk(offset,headers))
                idx += 1
            pDialog.close()
        # the last index is the current open game
        # hence we keep a reference to the current
        # gamestate, instead of reloading from file
        current = EntryInMemory(mainWindow.gs.game)
        self.entries[-1] = current
        self.checksum = crc32_from_file(self.filename)

    def init_from_file(self, mainWindow):
        with open(self.filename) as pgn:
            size = os.path.getsize(self.filename)
            self.entries = []
            pDialog = QProgressDialog(mainWindow.trUtf8("Scanning PGN File"),None,0,size,mainWindow)
            pDialog.show()
            pDialog.setWindowModality(PyQt4.QtCore.Qt.WindowModal)
            QApplication.processEvents()
            for offset, headers in chess.pgn.scan_headers(pgn):
                QApplication.processEvents()
                pDialog.setValue(offset)
                self.entries.append(EntryOnDisk(offset,headers))
            pDialog.close()
        self.checksum = crc32_from_file(self.filename)

    def init_from_cache(self):
        filename = self.filename[:-4] + ".idx"
        with open(filename,"rb") as f:
            self.index_current_game, self.checksum, self.entries = pickle.load(f)

    def no_of_games(self):
        return len(self.entries)

    def load_game(self, index):
        print("index: "+str(index))
        #print("open game index: "+str(self.game_open_idx))
        if not self.is_consistent():
            raise IOError("database file has changed on disk - index is inconsistent")
        if index >= 0 and index < len(self.entries):
            entry = self.entries[index]
            if(type(entry)==EntryOnDisk):
                with open(self.filename) as pgn:
                    offset = entry.pgn_offset
                    pgn.seek(offset)
                    game = chess.pgn.read_game(pgn)
                    print(str(game))
                self.index_current_game = index
                loaded = EntryInMemory(game)
                self.entries[index] = loaded
                return game
            else: # entry is game in memory
                self.index_current_game = index
                return entry.game_root
        else:
            raise ValueError("no game for supplied index in database")

    def is_consistent(self):
        checksum = crc32_from_file(self.filename)
        if not checksum == self.checksum:
            return False
        else:
            return True

