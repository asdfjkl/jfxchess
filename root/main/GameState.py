'''
Created on 31.07.2014

@author: user
'''

from Chessnut.game import Game
from Chessnut.game import InvalidMove
from GamePrinter import *

#from Chessnut import InvalidMove

def idx_to_str(x):
    return chr(97 + x % 8) 

class Point():
    def __init__(self,x,y):
        self.x = x
        self.y = y
        
    def to_str(self):
        return idx_to_str(self.x) + str(self.y+1)
    
    def __eq__(self, other):
        return self.x == other.x and self.y == other.y
    
    def __ne__(self, other):
        return not self.__eq__(other)
        
class Move():
    def __init__(self,src,dst,piece):
        self.src = src
        self.dst = dst
        self.piece = piece
        self.takes_piece = False
        self.comment = ""
        self.pos_annotation = ""
        self.en_passant = False
        self.move_annotation = ""
        self.san_src_marker = ""
    
    def __eq__(self, other):
        return self.src == other.src and self.dst == other.dst and self.piece == other.piece
    
    def __ne__(self, other):
        return not self.__eq__(other)
    
    def to_str(self):
        return self.src.to_str() + self.dst.to_str()
    
    def to_san(self):
        ret_str = ""
        if(self.piece == 'p' or self.piece == 'P'):
            ret_str = self.dst.to_str()
            if(self.takes_piece):
                ret_str = idx_to_str(self.src.x) + "x" + self.dst.to_str()
            if(self.en_passant):
                ret_str = idx_to_str(self.src.x) + "x" + self.dst.to_str()+ " e.p."
        else:
            ret_str = self.piece.upper() + self.san_src_marker
            if(self.takes_piece):
                ret_str = ret_str + "x"
            ret_str = ret_str + self.dst.to_str()
        ret_str = ret_str + self.move_annotation
        if(self.pos_annotation != ""):
            ret_str = ret_str + " " + self.pos_annotation
        if(self.comment != ""):
            ret_str = ret_str + " { " + self.comment + " } "
        return ret_str
        
class Board():
    def __init__(self):
        self.board = [['e' for x in range(8)] for x in range(8)]
        self.set_init_pos()
        
    def set(self,board):
        self.board = board
        
    def set_at(self,x,y,piece):
        self.board[x][y] = piece
    
    def get_at(self,x,y):
        return self.board[x][y]
    
    def deep_copy(self):
        board_copy = [row[:] for row in self.board]
        bc = Board()
        bc.set(board_copy)
        return bc
    
    def to_fen(self):
        fen =""
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
        return fen
    
    def set_init_pos(self):
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

class Config():
    def __init__(self):
        self.castleWhiteShort = True
        self.castleBlackShort = True
        self.castleWhiteLong = True
        self.castleBlackLong = True
        self.whiteInCheck = False
        self.blackInCheck = False
        self.whiteToMove = True
        self.blackEnPassant = None
        self.whiteEnPassant = None       
        
    def deep_copy(self):
        c = Config()
        c.castleWhiteShort = self.castleWhiteShort
        c.castleBlackShort = self.castleBlackShort
        c.castleWhiteLong = self.castleWhiteLong
        c.castleBlackLong = self.castleBlackLong
        c.whiteInCheck = self.whiteInCheck
        c.blackInCheck = self.blackInCheck
        c.whiteToMove = self.whiteToMove
        return c

    def to_fen(self):
        fen = ""
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
        fen = fen + " "
        if(self.blackEnPassant):
            fen = fen + self.blackEnPassant.to_str()
        elif(self.whiteEnPassant):
            fen = fen + self.whiteEnPassant.to_str()
        else:   
            fen = fen + "-"    
        
        #todo half+fullmoves
        fen = fen + " 0 1" 
        return fen           



class State():
    
    # PBNRKQ pbnrkq
    def __init__(self): 
        self.board = Board()
        self.config = Config()
        self.childs = []
        self.move = None
        self.parent = None
            
    def deep_copy(self):
        c = State()
        c.board = self.board.deep_copy()
        c.config = self.config.deep_copy()
        return c
    
    def to_fen(self):
        return self.board.to_fen() + self.config.to_fen()
    
    def to_console(self):
        string = ""
        for i in range(0,8):
            for j in range(0,8):
                string = string + self.board.board[j][i]
            string = string + "\n"
        return string
        
    def is_castle_white_short(self,move):
        if(move.piece == 'K' and move.src.x==4 
           and move.src.y==0 and move.dst.x==6):
            print("castle white short")
            return True
        else:
            return False

    def is_castle_white_long(self,move):
        if(move.piece == 'K' and move.src.x==4 
           and move.src.y==0 and move.dst.x==2):
            print("castle white long")
            return True
        else:
            return False

    def is_castle_black_short(self,move):
        if(move.piece == 'k' and move.src.x==4 
           and move.src.y==7 and move.dst.x==6):
            print("castle black short")
            return True
        else:
            return False
    
    def is_castle_black_long(self,move):
        if(move.piece == 'k' and move.src.x==4 
           and move.src.y==7 and move.dst.x==2):
            print("castle black long")
            return True
        else:
            return False

    def black_pawn_2_steps(self,move):
        if(move.piece == 'p' and move.src.y==6 and move.dst.y==4):
            return True
        else:
            return False
    
    def white_pawn_2_steps(self,move):
        if(move.piece == 'P' and move.src.y==1 and move.dst.y==3):
            return True
        else:
            return False
    
    def black_takes_en_passant(self,move):
        if(move.piece == 'p' and move.src.x != move.dst.x 
           and self.board.get_at(move.dst.x,move.dst.y) == 'e'):
            return True
        else:
            return False
    
    def white_takes_en_passant(self,move):
        if(move.piece == 'P' and move.src.x != move.dst.x
           and self.board.get_at(move.dst.x,move.dst.y) == 'e'):
            return True
        else:
            return False
    
    def execute_move(self,move):
        self.config.blackEnPassant = None
        self.config.whiteEnPassant = None
        if(self.white_takes_en_passant(move)):
            self.board.set_at(move.dst.x,move.dst.y-1,'e')
            move.en_passant = True
        if(self.black_takes_en_passant(move)):
            self.board.set_at(move.dst.x,move.dst.y+1,'e')
            move.en_passant = True
        #check if move takes another piece
        if(self.board.get_at(move.dst.x, move.dst.y) != 'e'):
            move.takes_piece = True
        self.board.set_at(move.src.x,move.src.y,'e')
        self.board.set_at(move.dst.x,move.dst.y,move.piece)
        if(self.config.whiteToMove):
            self.config.whiteToMove = False
        else:
            self.config.whiteToMove = True
        if(self.is_castle_white_short(move)):
            self.config.castleWhiteShort = False
            self.board.set_at(7, 0, 'e')
            self.board.set_at(5, 0, 'R')
        if(self.is_castle_white_long(move)):
            self.config.castleWhiteLong = False
            self.board.set_at(0, 0, 'e')
            self.board.set_at(3, 0, 'R')
        if(self.is_castle_black_short(move)):
            self.config.castleBlackShort = False
            self.board.set_at(7, 7, 'e')
            self.board.set_at(5, 7, 'r')
        if(self.is_castle_black_long(move)):
            self.config.castleBlackLong = False
            self.board.set_at(0, 7, 'e')
            self.board.set_at(3, 7, 'r')   
        if(self.black_pawn_2_steps(move)):
            self.config.blackEnPassant = Point(move.src.x,move.src.y-1)
            # print("ep recorded"+self.config.blackEnPassant.to_str())
        if(self.white_pawn_2_steps(move)):
            self.config.whiteEnPassant = Point(move.src.x,move.src.y+1)
            # print("ep recorded"+self.config.whiteEnPassant.to_str())
        
        
                
    def is_valid_move(self, move):
        board_copy = self.board.deep_copy()
        board_copy.set_at(move.src.x, move.src.y, move.piece)
        print("self to fen:"+board_copy.to_fen())
        g = Game(board_copy.to_fen() + self.config.to_fen())
        print("is checkmate "+str(g.is_checkmate()))
        # print("possible moves "+str(g.get_moves()))
        #for mv in g.get_moves():
        #    print(str(mv))
        # print("trying: "+move.to_str())
        try:
            g.apply_move(move.to_str())
            return True
        #except Exception as e: 
        except InvalidMove:
            # print("exception raised")
            return False
        
    def test(self):
        chessgame = Game()
        print(chessgame)  # 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
        print(chessgame.get_moves())
        chessgame.apply_move('e2e4')  # succeeds!
        print(chessgame)  # 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1'
        chessgame.apply_move('e2e4')  # fails! (raises InvalidMove exception)  

    
    
          
