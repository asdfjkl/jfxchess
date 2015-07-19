from util.crc32 import crc32_from_file
from PyQt4.QtGui import QApplication, QProgressDialog
import PyQt4
import os
import chess.pgn
import shutil
import pickle
import mmap as mm

class Entry():
    def __init__(self, _pgn_offset, _headers):
        super(Entry, self).__init__()
        self.pgn_offset = _pgn_offset
        self.headers = _headers

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
        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))
        f = open(filename, 'wb')
        #print(" ",file=f)
        f.close()
        self.checksum = crc32_from_file(self.filename)
        self.filename = filename

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
                self.entries.append(Entry(offset,headers))
            pDialog.close()
        self.checksum = crc32_from_file(self.filename)

    def init_from_cache(self):
        filename = self.filename[:-4] + ".idx"
        with open(filename,"rb") as f:
            self.index_current_game, self.checksum, self.entries = pickle.load(f)

    def get_end_offset(self):
        with open(self.filename,'r') as pgn:
            pgn.seek(0,os.SEEK_END)
            end_offset = pgn.tell()
        return end_offset

    def delete_game_at(self,idx):
        start_offset = self.entries[idx].pgn_offset

        # if the last game is to be deleted, first get
        # the offset at the very end of the file
        stop_offset = None
        if(idx == len(self.entries) -1):
            stop_offset = self.get_end_offset()
        else: # just take the start of the next game as end
            stop_offset = self.entries[idx+1].pgn_offset

        length = stop_offset - start_offset

        pgn = open(self.filename, 'r+')
        m=mm.mmap(pgn.fileno(),0)
        size = len(m)
        new_size = size - length
        m.move(start_offset,stop_offset,size-stop_offset)
        m.flush()
        m.close()
        pgn.truncate(new_size)
        pgn.close()

    def append_game(self, game_tree):
        # first remember the last position
        # of the current file
        # this is going to be the offfset for
        # the game that is written
        start_offset = self.get_end_offset()

        dos_newlines = False
        if "\r\n".encode() in open(self.filename,"rb").read():
            dos_newlines = True

        # append the game
        with open(self.filename, 'a') as pgn:
            if(dos_newlines):
                print('\r\n\r\n', file=pgn)
                print(game_tree.root(), file=pgn, end='\r\n\r\n')
            else:
                print("\n\n", file=pgn)
                print(game_tree.root(), file=pgn, end="\n\n")
        # create new entry
        self.entries.append(Entry(start_offset,game_tree.root().headers))
        self.index_current_game = len(self.entries) - 1

    def update_game(self, idx, game_tree):
        self.delete_game_at(idx)

        dos_newlines = False
        if "\r\n".encode() in open(self.filename,"rb").read():
            dos_newlines = True

        if(dos_newlines):
            game_str = (str(game_tree.root())).replace('\n','\r\n')
            game_str = (game_str + '\r\n\r\n').encode('utf-8')
        else:
            game_str = (str(game_tree.root())+"\n\n").encode('utf-8')
        length = len(game_str)

        offset = self.entries[idx].pgn_offset

        pgn = open(self.filename, 'r+')
        m=mm.mmap(pgn.fileno(),0)
        size = len(m)
        new_size = size + length
        m.flush()
        m.close()
        pgn.seek(size)
        pgn.write('A'*length)
        pgn.flush()
        m=mm.mmap(pgn.fileno(),0)
        m.move(offset+length,offset,size-offset)
        m.seek(offset)
        m.write(game_str)
        m.flush()
        m.close()
        pgn.close()

    def no_of_games(self):
        return len(self.entries)

    def load_game(self, index):
        if index >= 0 and index < len(self.entries):
            entry = self.entries[index]
            with open(self.filename) as pgn:
                offset = entry.pgn_offset
                pgn.seek(offset)
                game = chess.pgn.read_game(pgn)
                self.index_current_game = index
            return game
        else:
            raise ValueError("no game for supplied index in database")

    def is_consistent(self):
        checksum = crc32_from_file(self.filename)
        if not checksum == self.checksum:
            return False
        else:
            return True


import sys
entries = []
app = PyQt4.QtGui.QApplication(sys.argv)
w = PyQt4.QtGui.QWidget()
db = Database("/Users/user/Desktop/middleg.pgn")
db.init_from_file(w)
game = db.load_game(0)
db.update_game(1,game)
#db.delete_game_at(1)

#with open("/Users/user/Desktop/foobar.pgn", 'w') as pgn:
#    print("\n\n", file=pgn)
#    print(game.root(), file=pgn, end="\n\n")
