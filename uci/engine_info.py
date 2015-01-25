class EngineInfo(object):
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
