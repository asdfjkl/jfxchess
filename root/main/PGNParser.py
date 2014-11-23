import re
from GameTree import GameTree
from GameState import State,Move,Point
from Chessnut.game import Game
from Chessnut.game import InvalidMove
from GamePrinter import GamePrinter

p_draw = re.compile("1/2-1/2..")
p_ww = re.compile("1-0..")
p_bw = re.compile("0-1..")
p_unclear = re.compile("\*..")
p_move_number = re.compile("\d+\.")
p_move = re.compile("[a-zA-Z](\S+)")
p_tag = re.compile("\[\S+")
p_quotes = re.compile('"([A-Za-z0-9_\./\\-\s\*,?]*)"')
p_takes = re.compile("x")

def parse(line, lines, gt):
    gp = GamePrinter(gt)
    print("at line "+line)
    #print("current game " + gp.to_pgn())
    if(len(line) == 0):
        print("len line is zero")
        if(len(lines) == 0):
            print("lines also, so returnging gt")
            return gt
        else:
            print("rec call")
            parse(lines[0],lines[1:],gt)
    elif(line[0] == '['):
        print("retrieving "+line)
        extract_tag(line,gt)
        parse(lines[0],lines[1:],gt)
    elif(line[0] == ' '):
        print("enc space")
        parse(line[1:], lines, gt)
    elif(p_draw.match(line)):
        gt.result = "1/2-1/2"
        return gt
    elif(p_ww.match(line)):
        gt.result = "1-0"
        return gt
    elif(p_bw.match(line)):
        gt.result = "0-1"
        return gt
    elif(p_unclear.match(line)):
        gt.result = "*"
        return gt
    elif(p_move_number.match(line)):
        res = p_move_number.match(line)
        parse(line[res.end():],lines,gt)
    elif(p_move.match(line)):
        res = p_move.match(line)
        extract_move(line,gt)
        parse(line[res.end():],lines,gt)
    elif(line[0] == ";"):
        gt.current.move.comment = line[1:]
        parse("",lines,gt)
    elif(line[0] == "\n"):
        parse(lines[0],lines[1:],gt)


def extract_tag(line,gt):
    tag_match = p_tag.match(line)
    tag = tag_match.group()[1:]
    val_match = re.search(p_quotes,line)
    print(tag)
    val = val_match.group()[1:-1]
    print(val)
    if(tag=="Event"):
        gt.event = val
    elif(tag=="Site"):
        gt.site = val
    elif(tag=="Date"):
        gt.date = val
    elif(tag=="Round"):
        gt.round = val
    elif(tag=="White"):
        gt.white_player = val
    elif(tag=="Black"):
        gt.black_player = val
    elif(tag=="ECO"):
        gt.eco = val
    elif(tag=="Result"):
        print("parsing result: "+val)
        gt.result = val

def extract_move(line,gt):
    m = p_move.match(line)
    san = m.group()
    print("parsing "+san)
    move = Move(Point(0,0),Point(0,0),"B")
    if(san[-1] == '+'):
        move.checks = True
        san = san[:-1]
        print("is a checking move")
    if(san[-1] == '#'):
        move.checkmates = True
        san = san[:-1]
        print("checkmates")
    if(san[-1].isupper() and san[-2]=='='):
        move.promotesTo = san[-1]
        san = san[:-2]
        print("promotes to "+move.promoteTo)
    if(san == "O-O"):
        move.castles_short = True
        if(gt.current.config.whiteToMove):
            print("white to move")
            move.src = Point(4,0)
            move.dst = Point(6,0)
            move.piece = "K"
            if(gt.is_valid_move(move)):
                gt.execute_move(move)
            else:
                raise ValueError("can't apply O-O here")
        else:
            print("black to move")
            move.src = Point(4,7)
            move.dst = Point(6,7)
            move.piece = "k"
            if(gt.is_valid_move(move)):
                gt.execute_move(move)
            else:
                raise ValueError("can't apply O-O here")
    elif(san == "O-O-O"):
        move.castles_short = True
        if(gt.current.config.whiteToMove):
            move.src = Point(4,0)
            move.dst = Point(2,0)
            move.piece = "K"
            if(gt.is_valid_move(move)):
                gt.execute_move(move)
            else:
                raise ValueError("can't apply O-O-O here")
        else:
            move.src = Point(4,7)
            move.dst = Point(2,7)
            move.piece = "k"
            if(gt.is_valid_move(move)):
                gt.execute_move(move)
            else:
                raise ValueError("can't apply O-O-O here")
    else:
        print("san "+san)
        dst_x = ord(san[-2])-97
        dst_y = int(san[-1])-1
        g = Game(gt.current.board.to_fen()+gt.current.config.to_fen())
        move_list = g.get_moves()
        print("Move List for move " + san + " ml: "+str(move_list))
        print("san-3" + san[-3:])
        print("ml0" + move_list[0][-3:])
        ml_dsts = [x for x in move_list if x[-2:] == san[-2:]]
        print("filtered list "+str(ml_dsts))
        # determine figure
        if(san[0].isupper()):
            if(gt.current.config.whiteToMove):
                move.piece = san[0]
            else:
                move.piece = san[0].lower()
        else:
            if(gt.current.config.whiteToMove):
                move.piece = "P"
            else:
                move.piece = "p"
        print("move piece: "+move.piece)
        # filter ml_dsts w.r.t. same piece
        ml_dsts_piece = []
        for mv in ml_dsts:
            x = ord(mv[0]) - 97
            y = int(mv[1]) -1
            if(gt.current.board.get_at(x,y) == move.piece):
                ml_dsts_piece.append(mv)
        print("filtered list wrt piece "+str(ml_dsts_piece))
        if(len(ml_dsts_piece) == 1):
            src_x = ord(ml_dsts_piece[0][0])-97
            src_y = int(ml_dsts_piece[0][1])-1
            move.src = Point(src_x,src_y)
            move.dst = Point(dst_x,dst_y)
            if(gt.is_valid_move(move)):
                gt.execute_move(move)
                print("succ applied "+san)
            else:
                raise ValueError("can't apply move")
        if(len(ml_dsts_piece) > 1):
            print("case of two options left")
            # case if piece is pawn
            if(move.piece == 'P' or move.piece == 'p'):
                print("pawn case two options")
                ls = [x for x in ml_dsts_piece if x[0] == san[0]]
                src_x = ord(ls[0][0])-97
                src_y = int(ls[0][1])-1
                move.src = Point(src_x,src_y)
                move.dst = Point(dst_x,dst_y)
                if(gt.is_valid_move(move)):
                    gt.execute_move(move)
                    print("succ applied "+san)
                else:
                    raise ValueError("can't apply move")
            else: #case if piece is full piece
                print("two options piece")
                src_marker = san[1]
                print("srcmarker "+src_marker)
                move.san_src_marker = src_marker
                if(src_marker.isalpha()):
                    ls = [x for x in ml_dsts_piece if x[0] == src_marker]
                else:
                    ls = [x for x in ml_dsts_piece if x[1] == src_marker]
                print("ls "+str(ls))
                src_x = ord(ls[0][0:1])-97
                src_y = int(ls[0][1:2])-1
                move.src = Point(src_x,src_y)
                move.dst = Point(dst_x,dst_y)
                if(gt.is_valid_move(move)):
                    gt.execute_move(move)