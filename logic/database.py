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

        # check for dos-style newlines
        dos_newlines = False
        if "\r\n".encode() in open(self.filename,"rb").read():
            dos_newlines = True

        print("dos newlines:"+str(dos_newlines))

        # if the last game is to be deleted, first get
        # the offset at the very end of the file
        stop_offset = None
        if(idx == len(self.entries) -1):
            stop_offset = self.get_end_offset()
        else: # just take the start of the next game as end
            stop_offset = self.entries[idx+1].pgn_offset
        # now overwrite the deleted game with
        # blank spaces + \n
        length = stop_offset - start_offset
        pgn = os.open(self.filename, os.O_RDWR)
        m=mm.mmap(pgn,0)
        if(dos_newlines):
            empty_lines = ' '* (length-2) + '\r\n'
        else:
            empty_lines = ' '* (length-1) + '\n'
        m[start_offset:stop_offset] = empty_lines.encode('utf-8')
        m.flush()
        os.close(pgn)

        # remove the header from index
        del(self.entries[idx])

    def append_game(self, game_tree):
        # first remember the last position
        # of the current file
        # this is going to be the offfset for
        # the game that is written
        start_offset = self.get_end_offset()
        # append the game
        with open(self.filename, 'a') as pgn:
            print("\n\n", file=pgn)
            print(game_tree.root(), file=pgn, end="\n\n")
        # create new entry
        self.entries.append(Entry(start_offset,game_tree.root().headers))
        self.index_current_game = len(self.entries) - 1

    def update_game(self, idx, game_tree):
        self.delete_game_at(idx)
        self.append_game(game_tree)

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

