import re
from chess import WHITE, BLACK
from chess import Move
#from chess import Bitboard
import copy

class EngineInfo(object):

    READYOK = re.compile('readyok')
    SCORECP = re.compile('score\scp\s-{0,1}(\d)+')
    NPS = re.compile('nps\s(\d)+')
    DEPTH = re.compile('depth\s(\d)+')
    MATE = re.compile('score\smate\s-{0,1}(\d)+')
    CURRMOVENUMBER = re.compile('currmovenumber\s(\d)+')
    CURRMOVE = re.compile('currmove\s[a-z]\d[a-z]\d[a-z]{0,1}')
    BESTMOVE = re.compile('bestmove\s[a-z]\d[a-z]\d[a-z]{0,1}')
    PV = re.compile('pv(\s[a-z]\d[a-z]\d[a-z]{0,1})+')
    POS = re.compile('position\s')
    IDNAME = re.compile('id\sname\s(\w|\s)+')
    MOVE = re.compile('\s[a-z]\d[a-z]\d([a-z]{0,1})\s')
    MOVES = re.compile('\s[a-z]\d[a-z]\d([a-z]{0,1})')

    def __init__(self):
        self.id = None
        self.score = None
        self.depth = None
        self.strength = None
        self.mate = None
        self.currmovenumber = None
        self.currmove = None
        self.no_game_halfmoves = 0
        self.nps = None
        self.pv = None
        self.flip_eval = False
        self.pv_arr = []
        self.san_arr = None
        self.turn = WHITE
        self.board = None

    def pv_to_san(self):
        if(self.san_arr == None):
            return ""
        else:
            try:
                pv_san = []
                board = Bitboard(self.san_arr[0])
                moves = self.san_arr[1]
                for uci in moves:
                    move = Move.from_uci(uci)
                    if(move in board.pseudo_legal_moves):
                        pv_san.append(board.san(move))
                        board.push(move)
                if(len(pv_san) > 0):
                    s = ""
                    white_moves = True
                    move_no = (self.no_game_halfmoves//2)+1
                    if(self.no_game_halfmoves % 2 == 1):
                        white_moves = False
                        s += str(move_no)+". ... "
                        move_no += 1
                    for san in pv_san:
                        if(white_moves):
                            s += " "+str(move_no)+". "+san
                            move_no +=1
                        else:
                            s += " "+san
                        white_moves = not white_moves
                    return s
                else:
                    return ""
            except ValueError:
                return ""
    def update_from_string(self,line,fen=None):
        if(not fen==None):
            try:
                b = Bitboard(fen)
                self.turn = b.turn
            except ValueError:
                pass
        lines = line.split("\n")
        line = ""
        start = len(lines) - 1
        for i in range(start,-1,-1):
            line += lines[i]+"\n"
        cp = self.SCORECP.search(line)
        if(cp):
            self.mate = None
            score = float(cp.group()[9:])/100.0
            if(self.turn == BLACK and score != 0):
                self.score = -score
            else:
                self.score = score
        nps = self.NPS.search(line)
        if(nps):
            knps = int(nps.group()[4:])//1000
            self.nps = knps
        depth = self.DEPTH.search(line)
        if(depth):
            d = int(depth.group()[6:])
            self.depth = d
        mate = self.MATE.search(line)
        if(mate):
            m = int(mate.group()[11:])
            self.mate = m
        cmn = self.CURRMOVENUMBER.search(line)
        if(cmn):
            n = int(cmn.group()[15])
            self.currmovenumber = n
        cm = self.CURRMOVE.search(line)
        if(cm):
            move = cm.group()[9:]
            self.currmove = move
        pv = self.PV.search(line)
        if(pv):
            moves = pv.group()[3:]
            self.pv = moves
            # if this a pv line, modify to include
            # moves numbers
            #if(self.no_game_halfmoves):
            #    self.pv = self.add_move_numbers_to_info()
            self.pv_arr = moves.split(" ")
            if(fen != None):
                self.san_arr = (fen, moves.split(" "))
        id = self.IDNAME.search(line)
        if(id):
            engine_name = id.group()[8:].split("\n")[0]
            self.id = engine_name

    def __str__(self):
        outstr = '<table width="100%"><tr>'
        if(self.id):
            if(self.strength):
                outstr += '<th colspan="3" align="left">'+self.id+" (Level "+str(self.strength)+")</th>"
            else:
                outstr += '<th colspan="3" align="left">'+self.id+"</th>"
        outstr += '</tr><tr></tr><tr><td width="33%">'
        if(self.mate != None):
            if(self.mate < 0):
                outstr += "#"+str(-self.mate)
            else:
                outstr += "#"+str(self.mate)
        elif(self.score != None):
            outstr += '%.2f' % (self.score)
        outstr += '</td><td width="36%">'
        if(self.currmovenumber and self.currmove):
            halfmoves = self.currmovenumber + self.no_game_halfmoves
            move_no = ((self.currmovenumber + (self.no_game_halfmoves-1))//2)+1
            if(halfmoves % 2 == 0):
                outstr += str(move_no)+". ..."
            else:
                outstr += str(move_no) +". "
            outstr += str(self.currmove)
        outstr += "</td><td>"
        if(self.nps):
            outstr += str(self.nps)+" kn/s"
        outstr += '</td></tr><tr></tr><tr><td colspan="3" align="left">'
        if(self.pv):
            if(not self.mate == 0):
                outstr += self.pv_to_san()
        outstr += '</td></tr></table>'
        return outstr
