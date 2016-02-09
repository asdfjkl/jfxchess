from PyQt4 import QtGui
from chess.pgn import *

class GUIPrinter():
    def __init__(self):
        self.current = None
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.uci = ""

    def to_uci(self,current):
        self.current = current
        rev_moves = []
        node = current
        while(node.parent):
            rev_moves.append(node.move.uci())
            node = node.parent
        moves = " ".join(reversed(rev_moves))
        fen = node.board().fen()
        fen_current = self.current.board().fen()
        uci = "position fen "+fen+" moves "+moves
        return fen_current, uci

    def update_pos(self,current):
        return self.to_san_html(current, game=self.san_html)

    def generate_game(self, current):
        self.current = current
        self.offset_table = {}
        self.san_html = ""
        exporter = StringExporter(columns=None)
        current.root().export_html(exporter, current, self.offset_table)
        game = exporter.__str__()
        self.san_html = game
        return game

    def to_san_html(self, current, game = None):
        if not game:
            game = self.generate_game(current)

        start_idx = -1
        end_idx = - 1

        # for i in range(0, len(self.offset_table)):
        #     if self.offset_table[i][2] == current:
        #         start_idx = self.offset_table[i][0]
        #         end_idx = self.offset_table[i][1]
        # # mark the move leading to the current state
        if not start_idx == -1:
            game = game[:end_idx] + "</span>" + game[end_idx:]
            game = game[:start_idx] + ' <span style="background-color:yellow;font-family:CAChess">' + game[start_idx:]
        # do formatting of plain text with regexp here by
        # making all variations grey
        # [ ] must be then blocked for comments, otherwise
        # highlighting doesn't work
        # for highlighting current move, look up current move in offset table
        # then insert highlighting at offsets

        game = '<span style="color:black;font-size:13pt;font-family:CAChess">' + game + '</span>'
        game1 = re.sub("╔",'</span><br><br><em><span style="color:blue;font-size:11pt;font-family:CAChess">[', game)
        game2 = re.sub("╚",']<br><br></em></span> <span style="color:black;font-size:13pt;font-family:CAChess">', game1)

        try:
            game2 += " "+current.root().headers['Result']
            pass
        except KeyError:
            pass

        return game2