from PyQt4 import QtGui

class GamePrinter(): 
    def __init__(self, game_tree):
        self.gt = game_tree
        self.san_html = ""
        self.offset_table = []
        self.qtextedit = QtGui.QTextEdit()
        
    def variant_start(self, node, child, moveNo):    
        if(node.config.whiteToMove):
            self.san_plain = self.san_plain + str(moveNo)+"."
        else:
            self.san_plain = self.san_plain + str(moveNo-1)+"."
        if(not node.config.whiteToMove):
            self.san_plain = self.san_plain + " ... "
        self.san_plain = self.san_plain + child.move.to_san()
    
    def add_to_offset_table(self,node):
        self.qtextedit.setHtml(self.san_html)
        plain_san = self.qtextedit.toPlainText()
        offset_end = len(plain_san)
        self.qtextedit.setHtml(node.move.to_san())
        plain_move = self.qtextedit.toPlainText()
        offset_start = offset_end - len(plain_move)
        self.offset_table.append((offset_start,offset_end,node))
    
    def print_highlighted(self, node):
        string = ""
        if(self.gt.current == node):
            string = string + '<span style="color:darkgoldenrod">'
            string = string + node.move.to_san()
            string = string + '</span>'
        else:
            string = string + node.move.to_san()
        return string
    
    def variant_start_highlighted(self, node, child, moveNo):    
        if(node.config.whiteToMove):
            self.san_html = self.san_html + str(moveNo)+"."
        else:
            self.san_html = self.san_html + str(moveNo-1)+"."
        if(not node.config.whiteToMove):
            self.san_html = self.san_html + " ... "
        self.san_html = self.san_html + self.print_highlighted(child)
    
    def rec_san_html(self, node = None, moveNo = None, depth = True):
        temp = node
        if(node == None):
            temp = self.gt.root
        if(moveNo == None):
            moveNo = 1
        if(temp != None):
            if(not temp.config.whiteToMove):
                moveNo = moveNo + 1
            len_temp = len(temp.childs)
            self.san_html = self.san_html + " "
            if(len_temp > 0):
                if(temp.config.whiteToMove):
                    self.san_html = self.san_html + str(moveNo) + "."
                # print first move
                self.san_html = self.san_html + self.print_highlighted(temp.childs[0])
                self.add_to_offset_table(temp.childs[0])
                # print all alternatives
                for i in range(1,len_temp):
                    if(depth):
                        self.san_html = self.san_html + '<dd><em><span style="color:gray">'
                        self.san_html = self.san_html + "[ "
                        self.variant_start_highlighted(temp, temp.childs[i], moveNo)
                        self.add_to_offset_table(temp.childs[i])
                        self.san_html = self.san_html + '</span><span style="color:gray">'
                        self.rec_san_html(temp.childs[i],moveNo,False)
                        self.san_html = self.san_html + "]"
                        self.san_html = self.san_html + "</dd></em></span>"
                    else:
                        self.san_html = self.san_html + " ("
                        self.variant_start_highlighted(temp, temp.childs[i], moveNo)
                        self.add_to_offset_table(temp.childs[i])
                        self.rec_san_html(temp.childs[i],moveNo,False)
                        self.san_html = self.san_html + ") "
                # continue
                if(len_temp > 1 and (temp.config.whiteToMove) and temp.childs[0].childs != []):
                    self.san_html = self.san_html + str(moveNo) + ". ..."
                self.rec_san_html(temp.childs[0],moveNo, depth)
            elif(temp.childs != []):
                self.san_html = self.san_html + temp.childs[0].move.to_san()
                self.add_to_offset_table(temp.childs[0])                
                self.rec_san_html(temp.childs[0],moveNo, depth)

    
    def to_san_html(self, node = None, moveNo = None, depth = True):
        self.san_html = ""
        self.rec_san_html(node, moveNo, depth)
        return self.san_html

