'''
Created on 31.07.2014

@author: user
'''

from Chessnut.game import Game
from Chessnut.game import InvalidMove
#from Chessnut import InvalidMove

class State():
    
    # PBNRKQ pbnrkq
    def __init__(self): 
        self.board = [['e' for x in range(8)] for x in range(8)]
        self.castleWhiteShort = True
        self.castleBlackShort = True
        self.castleWhiteLong = True
        self.castleBlackLong = True
        self.whiteInCheck = False
        self.blackInCheck = False
        self.whiteToMove = True
        self.chessnutGame = Game()
        self.setInitPos()
        #self.chessnutGame.set_fen(self.toFen())
    
    def get(self,x,y):
        return self.board[x][y]
    
    def set(self,x,y,piece):
        self.board[x][y] = piece
        
    def xyToStr(self,x,y):
        """
        Convert a board index to algebraic notation.
        """
        return chr(97 + x % 8) + str(y+1)    
    
    def toFen(self):
        fen =""
        print("4 1 "+ self.board[4][1])
        for y in range(7,-1,-1):
            cnt = 0
            for x in range(0,8):
                piece = self.board[x][y]
                if(piece != 'e'):
                    if(cnt != 0):
                        fen = fen + str(cnt)
                    fen = fen + piece
                    cnt = 0
                else:
                    cnt = cnt + 1
                    if(x==7):
                        fen = fen + str(cnt)
            if(y>0):
                fen = fen+"/"
        if(self.whiteToMove):
            fen = fen + " w"
        else:
            fen = fen + " b"
        
        #castling
        if(not (self.castleWhiteShort or self.castleBlackShort or
           self.castleWhiteLong or self.castleBlackLong)):
            fen = fen + " -"
        else:
            fen = fen + " "
            if(self.castleWhiteShort):
                fen = fen + "K"
            if(self.castleWhiteLong):
                fen = fen + "Q"
            if(self.castleBlackShort):
                fen = fen + "k"
            if(self.castleBlackLong):
                fen = fen + "q"
        
        #todo en passent
        fen = fen + " -"
        
        #todo half+fullmoves
        fen = fen + " 0 1" 
        print(fen)
        return fen           
            
                
    def validMove(self, src, dst, piece):
        for mv in self.chessnutGame.get_moves():
            print(str(mv))
        srcStr = self.xyToStr(src[0],src[1])
        dstStr = self.xyToStr(dst[0],dst[1])
        print(srcStr+dstStr)
        try:
            self.chessnutGame.apply_move(srcStr+dstStr)
            return True
        #except Exception as e: 
        except InvalidMove:
            return False
        
    def test(self):
       chessgame = Game()
       print(chessgame)  # 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
       print(chessgame.get_moves())
       chessgame.apply_move('e2e4')  # succeeds!
       print(chessgame)  # 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1'
       chessgame.apply_move('e2e4')  # fails! (raises InvalidMove exception)  
           

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
