'''
Created on 31.07.2014

@author: user
'''

class State():
    
    # PBNRKQ pbnrkq
    def __init__(self): 
        self.board = [['e' for x in range(8)] for x in range(8)]
        self.castleWhiteShort = False
        self.castleBlackShort = False
        self.castleWhiteLong = False
        self.castleBlackLong = False

    def get(self,x,y):
        return self.board[x][y]

    def setInitPos(self):
        #pawns
        for x in range(0,8):
            self.board[x][1] = 'P'
            self.board[x][6] = 'p'
        #knights
        self.board[1][0] = 'N'
        self.board[6][0] = 'N'
        self.board[1][7] = 'n'
        self.board[6][7] = 'n'
        #bishops
        self.board[2][0] = 'B'
        self.board[5][0] = 'B'
        self.board[2][7] = 'b'
        self.board[5][7] = 'b'
        #rooks
        self.board[0][0] = 'R'
        self.board[7][0] = 'R'
        self.board[0][7] = 'r'
        self.board[7][7] = 'r'
        #queens
        self.board[3][0] = 'Q'
        self.board[3][7] = 'q'
        #kings
        self.board[4][0] = 'K'
        self.board[4][7] = 'k'
