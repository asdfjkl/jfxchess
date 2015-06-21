from util.crc32 import crc32_from_file
from PyQt4.QtGui import QApplication, QProgressDialog
import PyQt4
import os
import chess

class Database():

    def __init__(self, filename):
        super(Database, self).__init__()
        self.filename = filename
        self.offset_headers = []
        self.checksum = None
        self.game_open = False
        self.game_open_idx = None
        self.unsaved_changes = False

    def initialize(self, mainWindow):
        with open(self.filename) as pgn:
            size = os.path.getsize(self.filename)
            self.offset_headers = []
            pDialog = QProgressDialog(mainWindow.trUtf8("Scanning PGN File"),None,0,size,mainWindow)
            pDialog.show()
            pDialog.setWindowModality(PyQt4.QtCore.Qt.WindowModal)
            QApplication.processEvents()
            for offset, headers in chess.pgn.scan_headers(pgn):
                QApplication.processEvents()
                pDialog.setValue(offset)
                self.offset_headers.append((offset,headers))
            pDialog.close()
        self.checksum = crc32_from_file(self.filename)

    def no_of_games(self):
        return len(self.offset_headers)

    def load_game(self, index):
        if not self.is_consistent():
            raise IOError("database file has changed on disk - index is inconsistent")
        if index >= 0 and index < len(self.offset_headers):
            with open(self.filename) as pgn:
                offset, headers = self.offset_headers[index]
                pgn.seek(offset)
                game = chess.pgn.read_game(pgn)
                self.game_open = True
                self.game_open_idx = index
                return game
        else:
            raise ValueError("no game for supplied index in database")

    def is_consistent(self):
        checksum = crc32_from_file(self.filename)
        if not checksum == self.checksum:
            return False
        else:
            return True

