import re
from chess import WHITE, BLACK

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
        self.turn = WHITE

    def update_from_string(self,line):
        cp = self.SCORECP.search(line)
        contains_cp = False
        if(cp):
            print("contains cp:"+cp.group())
            contains_cp = True
            self.mate = None
            score = float(cp.group()[9:])/100.0
            print("score"+str(score))
            if(self.turn == BLACK and score != 0):
                self.score = -score
            else:
                self.score = score
            #print("score: "+str(score))
            emit_info = True
        nps = self.NPS.search(line)
        if(nps):
            knps = int(nps.group()[4:])//1000
            self.nps = knps
            emit_info = True
        depth = self.DEPTH.search(line)
        if(depth):
            d = int(depth.group()[6:])
            self.depth = d
            #self.mate = None
            emit_info = True
        mate = self.MATE.search(line)
        if(mate):
            #print("found mate"+str(mate.group()))
            # if score update was done
            # ma
            #if(not contains_cp):
                #print("setting mate since no cp")
            m = int(mate.group()[11:])
            self.mate = m
            emit_info = True
        cmn = self.CURRMOVENUMBER.search(line)
        if(cmn):
            n = int(cmn.group()[15])
            self.currmovenumber = n
            emit_info = True
        cm = self.CURRMOVE.search(line)
        if(cm):
            move = cm.group()[9:]
            self.currmove = move
            emit_info = True
        pv = self.PV.search(line)
        if(pv):
            moves = pv.group()[3:]
            self.pv = moves
            # if this a pv line, modify to include
            # moves numbers
            if(self.no_game_halfmoves):
                self.pv = self.add_move_numbers_to_info()
            self.pv_arr = moves.split(" ")
            emit_info = True
            #print("pv original:"+str(moves))
            #print("pv split:"+str(moves.split(" ")))
        id = self.IDNAME.search(line)
        if(id):
            engine_name = id.group()[8:].split("\n")[0]
            #print("rec: "+engine_name+"end")
            self.id = engine_name
            emit_info = True
        #if(emit_info): pass
            #print("emitting: "+str(self.info))
            #self.emit(SIGNAL("newinfo(PyQt_PyObject)"),copy.deepcopy(self.info))



    def add_move_numbers_to_info(self):
        replaced_all = False
        move_no = (self.no_game_halfmoves//2)+1
        white_moves = True
        s = ""
        if(self.no_game_halfmoves % 2 == 1):
            s += str(move_no)+". ...?"+self.pv[:]
            move_no += 1
        else:
            white_moves = False
            s += str(move_no) +"."+self.pv[:]
        while(replaced_all == False):
            mv = self.MOVE.search(s)
            if(mv):
                idx = mv.start()
                if(white_moves):
                    s = s[:(idx)] + " "+str(move_no) + "."+ s[(idx+1):]
                else:
                    move_no += 1
                    s = s[:(idx)] + "?"+\
                                           s[(idx+1):]
                white_moves = not white_moves
            else:
                replaced_all = True
        #print(s)
        s = s.replace('?',' ')
        return s



    def __str__(self):
        outstr = '<table width="100%"><tr>'
        if(self.id):
            if(self.strength):
                outstr += '<th colspan="3" align="left">'+self.id+" (Level "+self.strength+")</th>"
            else:
                outstr += '<th colspan="3" align="left">'+self.id+"</th>"
        outstr += '</tr><tr></tr><tr><td width="33%">'
        if(self.mate != None):
            if(self.mate < 0):
                outstr += "#"+str(-self.mate)
            else:
                outstr += "#"+str(self.mate)
        elif(self.score != None):
            #if(self.score != 0.0):
            outstr += '%.2f' % (-self.score)
        outstr += '</td><td width="36%">'
        if(self.currmovenumber and self.currmove):
            halfmoves = self.currmovenumber + self.no_game_halfmoves
            move_no = ((self.currmovenumber + (self.no_game_halfmoves-1))//2)+1
            if(halfmoves % 2 == 0):
                outstr += str(move_no)+". ..."
            else:
                outstr += str(move_no) +"."
            outstr += str(self.currmove)
        outstr += "</td><td>"
        if(self.nps):
            outstr += str(self.nps)+" kn/s"
        outstr += '</td></tr><tr></tr><tr><td colspan="3" align="left">'
        if(self.pv):
            outstr += self.pv
        outstr += '</td></tr></table>'
        return outstr
