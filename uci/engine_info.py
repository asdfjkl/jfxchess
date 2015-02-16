import re

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
        self.no_game_halfmoves = None
        self.nps = None
        self.pv = None
        self.flip_eval = False
        self.pv_arr = []

    def update_from_string(self,line):
        cp = self.SCORECP.search(line)
        if(cp):
            print(cp.group())
            score = float(cp.group()[9:])/100.0
            self.score = score
            print("score: "+str(score))
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
            self.mate = None
            emit_info = True
        mate = self.MATE.search(line)
        if(mate):
            #print(str(mate.group()))
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
            #if(self.info.no_game_halfmoves):
                #self.info.pv = self.add_move_numbers_to_info()
            self.pv_arr = moves.split(" ")
            emit_info = True
            #print("pv original:"+str(moves))
            #print("pv split:"+str(moves.split(" ")))
        id = self.IDNAME.search(line)
        if(id):
            engine_name = id.group()[8:]
            print("rec: "+engine_name)
            self.id = engine_name
            emit_info = True
        #if(emit_info): pass
            #print("emitting: "+str(self.info))
            #self.emit(SIGNAL("newinfo(PyQt_PyObject)"),copy.deepcopy(self.info))

        bm = self.BESTMOVE.search(line)
        if(bm):
            move = bm.group()[9:]
            #self.emit(SIGNAL("bestmove(QString)"),move)




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
            if(self.flip_eval and self.score != 0.0):
                outstr += '%.2f' % (-self.score)
            else:
                outstr += '%.2f' % self.score
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
