from util.crc32 import crc32_from_file
from PyQt4.QtGui import QApplication, QProgressDialog
import PyQt4
import os
import chess.pgn
import shutil
import pickle
import mmap as mm
import configparser
import json
import util.appdirs as ad
import codecs


appname = 'jerry200'
appauthor = 'dkl'

class Entry():
    def __init__(self, _pgn_offset, _headers):
        super(Entry, self).__init__()
        self.pgn_offset = _pgn_offset
        self.headers = _headers

    def __str__(self):
        s = "[Entry]\n"
        s += "pgn_offset="+str(self.pgn_offset)+"\n"
        for header in self.headers:
            print(header)
            s += str(header)+"="+str(self.headers[header])+"\n"
        return s

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
        f.close()
        self.checksum = crc32_from_file(self.filename)
        self.filename = filename

    def reload_if_necessary(self, mainWindow):
        if not os.path.isfile(self.filename):
            self.filename = ad.user_data_dir(appname, appauthor) + "/mygames.pgn"
            self.index_current_game = None
        # if still path doesn't exist, we have to create
        # new default pgn from scratch
        if(not os.path.isfile(self.filename)):
            self.create_new_pgn()
        else:
            crc = crc32_from_file(self.filename)
            if not crc == self.checksum:
                try:
                    self.init_from_pgn(mainWindow, mainWindow.trUtf8("Re-loading PGN File..."))
                except BaseException as e:
                    #print(e)
                    self.filename = ad.user_data_dir(appname, appauthor) + "/mygames.pgn"
                    self.index_current_game = None
                    self.create_new_pgn()
            else:
                pass

    def init_from_pgn(self, mainWindow, msg):
        with codecs.open(self.filename,"r", "iso-8859-15") as pgn:
            print(pgn)
            size = os.path.getsize(self.filename)
            self.entries = []
            pDialog = QProgressDialog(msg,None,0,size,mainWindow)
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

        # we can't mmap an empty file
        # the file is however empty, the
        # game to be deleted was the only one in the database
        # just delete it
        current_filesize = os.stat(self.filename).st_size
        if current_filesize == 0:
            pgn = open(self.filename, 'r+')
            pgn.close()
        else:
            pgn = open(self.filename, 'r+')
            m=mm.mmap(pgn.fileno(),0)
            size = len(m)
            new_size = size - length
            m.move(start_offset,stop_offset,size-stop_offset)
            m.flush()
            m.close()
            pgn.truncate(new_size)
            pgn.close()
            self.checksum = crc32_from_file(self.filename)

        for i in range(idx, len(self.entries)):
            self.entries[i].pgn_offset -= length
        del(self.entries[idx])
        # if idx was the current open game, set current_idx to None
        if(idx == self.index_current_game):
            self.index_current_game = None


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

        self.checksum = crc32_from_file(self.filename)

        # create new entry
        self.entries.append(Entry(start_offset,game_tree.root().headers))
        self.index_current_game = len(self.entries) - 1


    def delete_game_for_update_at(self,idx):
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

        return length


    def update_game(self, idx, game_tree):
        old_length = self.delete_game_for_update_at(idx)

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

        # we can't mmap an empty file
        # the file is however empty, if the current
        # game was the only one in the database
        # just append it
        current_filesize = os.stat(self.filename).st_size
        if current_filesize == 0:
            self.entries = []
            self.append_game(game_tree)
        else:
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

            for i in range(idx+1, len(self.entries)):
                self.entries[i].pgn_offset += (length-old_length)

        self.checksum = crc32_from_file(self.filename)
        self.entries[idx] = Entry(offset, game_tree.root().headers)


    def no_of_games(self):
        return len(self.entries)

    def load_game(self, index):
        if index >= 0 and index < len(self.entries):
            entry = self.entries[index]
            with codecs.open(self.filename,"r", "iso-8859-15") as pgn:

            #with open(self.filename) as pgn:
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

"""
import sys
app = PyQt4.QtGui.QApplication(sys.argv)
w = PyQt4.QtGui.QWidget()
db = Database("/Users/user/Desktop/middleg.pgn")
db.init_from_file(w)
game = db.load_game(21)
#db.update_game(1,game)
print("offset of 2: "+str(db.entries[2].pgn_offset))
print("offset of 3: "+str(db.entries[3].pgn_offset))
print("deleting game at 2")
db.update_game(2,game)
print("calculated offsets:")
print("offset of 2: "+str(db.entries[2].pgn_offset))
print("offset of 3: "+str(db.entries[3].pgn_offset))

entries = []
with open("/Users/user/Desktop/middleg.pgn","r") as pgn:
    for offset, headers in chess.pgn.scan_headers(pgn):
        entries.append((offset,headers))
print("real offsets:")
print("offset of 2: "+str(entries[2][0]))
print("offset of 3: "+str(entries[3][0]))

#with open("/Users/user/Desktop/foobar.pgn", 'w') as pgn:
#    print("\n\n", file=pgn)
#    print(game.root(), file=pgn, end="\n\n")
"""
