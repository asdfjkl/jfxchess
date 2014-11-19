from GameState import *
from GamePrinter import *
import time

class GameTree():
    def __init__(self):
        self.root = State()
        self.current = self.root
        #self.printer = GamePrinter(self)
        self.event = ""
        self.site = ""
        self.date = time.strftime("%Y.%m.%d")
        self.round = ""
        self.white_player = "N.N."
        self.black_player = "Jerry (PC)"
        self.eco = ""
        self.result = "*"
    
    #checks if applying the move in
    #current state is valid
    def is_valid_move(self,move):
        if(self.current.is_valid_move(move)):
            return True
        else:
            return False
        
    def is_valid_and_promotes(self,move):
        if(self.current.is_valid_and_promotes(move)):
            return True
        else:
            return False
    
    def exist_move(self,move):
        for state in self.current.childs:
            mv = state.move
            if(move == mv):
                return self.current.childs.index(state)
        return None
    
    def execute_move(self,move):
        idx = self.exist_move(move)
        if(idx != None):
            self.next(idx)
        else:
            c = self.current.deep_copy()
            c.execute_move(move)
            c.move = move
            self.current.childs.append(c)
            c.parent = self.current
            self.current = c
        
    def prev(self):
        if(self.current.parent != None):
            self.current = self.current.parent
            print(self.current.board.to_fen())
            
    def next(self, idx = None):
        if(idx != None and idx < len(self.current.childs)):
            self.current = self.current.childs[idx]
        elif(len(self.current.childs)>0):
            self.current = self.current.childs[0]
            
    def move_list(self):
        mvs = []
        for ch in self.current.childs:
            mvs.append(ch.move.to_str())
        return mvs
    
    def exist_variants(self):
        return len(self.current.childs) > 1
        
    def to_san_plain(self, node = None, moveNo = None, depth = True, offset = None):
        return self.printer.to_san_plain(node, moveNo, depth)

    def get_offset_table(self):
        return self.printer.offset_table
    
    def get_state_from_offset(self, offset, printer):
        # next is to update to current status
        text = printer.to_san_html()
        # print(str(self.printer.offset_table))
        offset_index = printer.offset_table
        j = 0
        for i in range(0,len(offset_index)):
            if(offset>= offset_index[i][0] and offset<= offset_index[i][1]):
                j = i
        try:
            return offset_index[j][2]
        except IndexError:
            return None
    
    def delete_variant(self, state):
        variant_root = state
        while(variant_root.parent != None and variant_root.parent.childs[0] == variant_root):
            variant_root = variant_root.parent
        parent = variant_root.parent
        if(parent != None):
            idx = parent.childs.index(variant_root)
            del(parent.childs[idx])
            self.current = parent
    
    def delete_all_comments(self, state = None):
        temp = state
        if(temp == None):
            temp = self.root
        for i in range(0,len(temp.childs)):
            temp.childs[i].move.comment = ""
            self.delete_all_comments(temp.childs[i])
            
    def delete_all_variants(self, state):
        # get root and check if we are currently
        # on a variant
        variant_root = state
        while(variant_root.parent != None and variant_root.parent.childs[0] == variant_root):
            variant_root = variant_root.parent
        parent = variant_root.parent
        if(parent != None):
            idx = parent.childs.index(variant_root)
            if(idx > 0):
                # we are on a variant
                self.current = parent
        # now delete
        temp = self.root
        while(temp.childs != []):
            new_childs = []
            new_childs.append(temp.childs[0])
            temp.childs = new_childs
            temp = temp.childs[0]
    
    def delete_from_here(self, state):
        if(state.parent == None):
            self.current = self.root
            self.root.childs = []
        else:
            parent = state.parent
            self.current = parent
            idx = parent.childs.index(state)
            del(parent.childs[idx])
    
    def variant_down(self, state):
        variant_root = state
        while(variant_root.parent != None and variant_root.parent.childs[0] == variant_root):
            variant_root = variant_root.parent
        parent = variant_root.parent
        if(parent != None):
            idx = parent.childs.index(variant_root)
            if(idx < len(parent.childs) -1):
                temp = parent.childs[idx +1]
                parent.childs[idx+1] = parent.childs[idx]
                parent.childs[idx] = temp    
    
    
    def variant_up(self, state):
        variant_root = state
        while(variant_root.parent != None and variant_root.parent.childs[0] == variant_root):
            variant_root = variant_root.parent
        parent = variant_root.parent
        if(parent != None):
            idx = parent.childs.index(variant_root)
            if(idx > 0):
                temp = parent.childs[idx -1]
                parent.childs[idx-1] = parent.childs[idx]
                parent.childs[idx] = temp    
    
    def print_formatted(self, node):
        string = ""
        if(self.current == node.state):
            string = string + '<span style="color:darkgoldenrod">'
            string = string + node.move.to_san()
            string = string + '</span>'
        else:
            string = string + node.move.to_san()
        return string

    def to_str(self):
        game = ""
        tmp = self.root
        while(tmp.childs != []):
            game = game + " " + tmp.childs[0].move.to_str()
            tmp = tmp.childs[0].state
        print(game)