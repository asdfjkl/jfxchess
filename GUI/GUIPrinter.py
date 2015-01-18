from PyQt4 import QtGui
from chess.pgn import *

EXPAND = True
PRINT_NODE = True
START_VAR = True
END_VAR = True

class GUIStringExporter(object):
    """
    Allows exporting a game as a string.

    The export method of `Game` also provides options to include or exclude
    headers, variations or comments. By default everything is included.

    >>> exporter = chess.pgn.StringExporter()
    >>> game.export(exporter, headers=True, variations=True, comments=True)
    >>> pgn_string = str(exporter)

    Only `columns` characters are written per line. If `columns` is `None` then
    the entire movetext will be on a single line. This does not affect header
    tags and comments.

    There will be no newlines at the end of the string.
    """

    def __init__(self, columns=80):
        self.lines = []
        self.columns = columns
        self.current_line = ""
        self.offset_table = []

    def flush_current_line(self):
        if self.current_line:
            self.lines.append(self.current_line.rstrip())
        self.current_line = ""

    def write_token(self, token):
        if self.columns is not None and self.columns - len(self.current_line) < len(token):
            self.flush_current_line()
        self.current_line += token

    def write_line(self, line=""):
        self.flush_current_line()
        self.lines.append(line.rstrip())

    def start_game(self):
        pass

    def end_game(self):
        self.write_line()

    def start_headers(self):
        pass

    def put_header(self, tagname, tagvalue):
        self.write_line("[{0} \"{1}\"]".format(tagname, tagvalue))

    def end_headers(self):
        self.write_line()

    def start_main_variation(self):
        self.write_token('<dd><em><span style="color:gray">[ ')

    def end_main_variation(self):
        self.write_token('] </dd></em></span>')

    def start_variation(self):
        self.write_token("( ")

    def end_variation(self):
        self.write_token(") ")

    def put_starting_comment(self, comment):
        self.put_comment(comment)

    def put_comment(self, comment):
        self.write_token("{ " + comment.replace("}", "").strip() + " } ")

    def put_nags(self, nags):
        for nag in sorted(nags):
            self.put_nag(nag)

    def put_nag(self, nag):
        self.write_token("$" + str(nag) + " ")

    def put_fullmove_number(self, turn, fullmove_number, variation_start):
        if turn == chess.WHITE:
            self.write_token(str(fullmove_number) + ". ")
        elif variation_start:
            self.write_token(str(fullmove_number) + "... ")

    def put_move(self, node, board, move):
        l = len(str(self))
        self.offset_table.append((l,node))
        self.write_token(board.san(move) + " ")

    def put_result(self, result):
        self.write_token(result + " ")

    def __str__(self):
        if self.current_line:
            return "\n".join(itertools.chain(self.lines, [ self.current_line.rstrip() ] )).rstrip()
        else:
            return "\n".join(self.lines).rstrip()


class GUIPrinter():
    def __init__(self):
        self.current = None
        self.san_html = ""
        self.sans = []
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()
        self.cache = {}
        self.uci = ""

    def node_to_san(self,parent_board,node):
        return parent_board.san(node.move)

    def add_to_offset_table(self,node,san):
        self.qtextedit.setHtml(self.san_html)
        plain_san = self.qtextedit.toPlainText()
        offset_end = len(plain_san)
        self.qtextedit.setHtml(san)
        plain_move = self.qtextedit.toPlainText()
        offset_start = offset_end - len(plain_move) - 1
        self.offset_table.append((offset_start,offset_end,node))

    def get_board(self,node):
        if(node in self.cache):
            return self.cache[node]
        else:
            board = node.board()
            self.cache.update({node:board})
            return board

    def pgn_code_to_txt(self,nag):
        if(nag == 1):
            return "!"
        elif(nag==2):
            return "?"
        elif(nag==3):
            return "!!"
        elif(nag==4):
            return "??"
        elif(nag==5):
            return "!?"
        elif(nag==6):
            return "?!"
        elif(nag==13):
            return "∞"
        elif(nag==14):
            return "+/="
        elif(nag==15):
            return "=/+"
        elif(nag==16):
            return "+/−"
        elif(nag==17):
            return "-/+"
        elif(nag==18):
            return "+-"
        elif(nag==19):
            return "-+"
        elif(nag==44):
            return "=/∞"
        elif(nag==45):
            return "∞/="

    def print_move(self,node,child):
        move_san = ""
        board = self.get_board(node)
        if(self.current == child):
            move_san += '<span style="color:darkgoldenrod">'
            move_san += self.node_to_san(board,child)
            for nag in sorted(list(child.nags)):
                move_san += self.pgn_code_to_txt(nag)
            move_san += '</span>'
        else:
            move_san += self.node_to_san(board,child)
            for nag in sorted(list(child.nags)):
                move_san += self.pgn_code_to_txt(nag)
        if(child.comment != ""):
            move_san += '<span style="color:darkblue">'
            move_san += " "+child.comment+" "
            move_san += '</span>'
        return move_san

    def print_nodes(self,nodes):
        s  = ""
        for a,b,el in nodes:
            if(el.move):
                s += str(a) +"," +str(b) + ","+ el.move.uci() + "   "
        print(s+"\n")

    def print_san(self,node,halfmoves,inner_variant = False):

        # (expand, print, node)
        nodes = []
        nodes.append((True,True,0,False,False,node))

        while(nodes != []):
            # pop next node incl. its configuration
            (expand, print_, halfmoves, var_start, var_end, node) = nodes.pop()
            board = self.get_board(node)

            # move_no is just halfmoves/2
            move_no = halfmoves//2 + 1

            #if(var_end):
            #    self.san_html += "]</dd></em></span> "
            #if(var_start == START_VAR):
            #        self.san_html += " ("
            #if(var_start == START_VAR_LB):
            #    self.san_html += '<dd><em><span style="color:gray">['

            if(print_ and node.move):

                # if the current turn is black, the move leading
                # was by white. thus print move no.
                if(board.turn == chess.BLACK):
                    #self.san_html += str(move_no) + "."
                    print(str(move_no)+ ".")

                # if we are at a setup position where at the current
                # node white moves, i.e. a move by black leading to it
                # also print the move no plus ...
                if(node.parent == None and board.turn == chess.WHITE):
                    #self.san_html += str(move_no-1) + ". ... "
                    print(str(move_no-1) + ". ... ")

                #self.san_html += node.move.uci()+ " "
                print(node.move.uci()+ " ")

            vars = node.variations
            l = len(vars)

            #print("l: "+str(l) + "and expand: "+str(expand))
            if(l > 1 and expand):
                #print("expanding")
                temp = []
                temp.append((EXPAND,not PRINT_NODE,halfmoves+1,not START_VAR, END_VAR, vars[0]))

                #print("appending for print, non-expand: "+vars[0].move.uci())
                for i in range(1,l):
                    #print("i at"+str(i))
                    temp.append((EXPAND,PRINT_NODE,halfmoves+1, START_VAR, not END_VAR, vars[i]))
                temp.append((not EXPAND,PRINT_NODE,halfmoves+1, not START_VAR, not END_VAR, vars[0]))
                nodes = nodes + temp
            elif(l == 1 and expand):
                nodes.append((START_VAR,PRINT_NODE,halfmoves+1, not START_VAR, not END_VAR,vars[0]))
            # continue
            #self.print_san(node.variation(0),move_no, inner_variant)
            #nodes.append(node.variation(0))

    def print_san_r(self,node,move_no,inner_variant):

        board = self.get_board(node)
        # after a move by black,
        # increase move counter
        if(board.turn == chess.BLACK):
            move_no += 1

        # add space before each move
        self.san_html += " "

        if(len(node.variations) > 0):

            # if the next move is by white, print
            if(board.turn == chess.WHITE):
                self.san_html += str(move_no) + "."

            # if we are at a setup position where
            # starts moving, also print the move no plus ...
            if(node.parent == None and board.turn == chess.BLACK):
                self.san_html += str(move_no-1) + ". ... "

            # print move of main variation
            move_san = self.print_move(node,node.variation(0))
            self.san_html += move_san
            self.add_to_offset_table(node.variation(0),move_san)

            # print all other variations
            for i in range(1,len(node.variations)):

                nodei = node.variation(i)
                boardi= self.get_board(node)

                # if we are in the main variation
                if(not inner_variant):
                    self.san_html += '<dd><em><span style="color:gray">['
                else:
                    self.san_html += " ("
                if(boardi.turn == chess.WHITE):
                    self.san_html += str(move_no)+"."
                else:
                    self.san_html += str(move_no-1)+"."
                    self.san_html += " ... "
                move_san = self.print_move(node,nodei)
                self.san_html += move_san
                self.add_to_offset_table(nodei,move_san)
                self.print_san(nodei,move_no, True)

                self.san_html = self.san_html[:-1]
                if(not inner_variant):
                    self.san_html += "]</dd></em></span>"
                else:
                    self.san_html += ")"

            # if variations exist, the move was by black, and
            # the main variation itself has variations, then
            # add some spacer
            if(len(node.variations) > 1 and (board.turn == chess.WHITE) and node.variation(0).variations != None):
                self.san_html += str(move_no) + ". ..."

            # continue
            self.print_san(node.variation(0),move_no, inner_variant)

    def is_first_variant(self,node):
        if(node.parent and len(node.parent.variations) > 1 and
           node == node.parent.variations[1]):
            return True
        else:
            return False

    def to_uci(self,current):
        self.current = current
        rev_moves = []
        node = current
        while(node.parent):
            rev_moves.append(node.move.uci())
            node = node.parent
        moves = " ".join(reversed(rev_moves))
        fen = node.board().fen()
        uci = "position fen "+fen+" moves "+moves
        return uci

    def export_it(self,exporter,node,comments=True,variations=True,_board=None,_after_variation=False):
        if _board is None:
            _board = node.board()

        # The mainline move goes first.
        if node.variations:
            main_variation = node.variations[0]

            # Append fullmove number.
            exporter.put_fullmove_number(_board.turn, _board.fullmove_number, _after_variation)

            # Append SAN.
            exporter.put_move(main_variation, _board, main_variation.move)

            if comments:
                # Append NAGs.
                exporter.put_nags(main_variation.nags)

                # Append the comment.
                if main_variation.comment:
                    exporter.put_comment(main_variation.comment)

        # Then export sidelines.
        if variations:
            for variation in itertools.islice(node.variations, 1, None):

                # Start variation.
                if(self.is_first_variant(variation)):
                    exporter.start_main_variation()
                else:
                    exporter.start_variation()

                # Append starting comment.
                if comments and variation.starting_comment:
                    exporter.put_starting_comment(variation.starting_comment)

                # Append fullmove number.
                exporter.put_fullmove_number(_board.turn, _board.fullmove_number, True)

                # Append SAN.
                exporter.put_move(variation, _board, variation.move)

                if comments:
                    # Append NAGs.
                    exporter.put_nags(variation.nags)

                    # Append the comment.
                    if variation.comment:
                        exporter.put_comment(variation.comment)

                # Recursively append the next moves.
                _board.push(variation.move)
                variation.export(exporter, comments, variations, _board, False)
                _board.pop()

                # End variation.
                if(self.is_first_variant(variation)):
                    exporter.end_main_variation()
                else:
                    exporter.end_variation()

        # The mainline is continued last.
        if node.variations:
            main_variation = node.variations[0]

            # Recursively append the next moves.
            _board.push(main_variation.move)
            #main_variation.export(exporter, comments, variations, _board, variations and len(node.variations) > 1)
            self.export_it(exporter, comments, variations, _board, variations and len(node.variations) > 1)
            _board.pop()


    def export_string(self,node):
        exporter = GUIStringExporter()
        exporter.start_game()
        self.export_it(exporter, node,comments=False, variations=node.variations)
        exporter.end_game()
        self.offset_table = exporter.offset_table
        return str(exporter)

    def to_san_html(self,current):
        self.current = current
        self.offset_table = []
        self.san_html = ""
        #self.print_san(current.root(),0,False)
        #return self.san_html
        exporter = StringExporter(columns=None)
        current.root().export_html(exporter,current)
        return exporter.__str__()
        #return self.export_string(current.root())