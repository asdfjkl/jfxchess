from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from GUI.GUIPrinter import GUIPrinter
from dialogs.DialogWithListView import DialogWithListView
from dialogs.DialogWithPlaintext import DialogWithPlainText

class MovesEdit(QTextEdit):

    def __init__(self,gamestate):
        super(QTextEdit, self).__init__()
        self.gs = gamestate
        self.old_cursor_pos = -1
        self.setCursorWidth(0)
        self.viewport().setCursor(Qt.ArrowCursor)
        self.setContextMenuPolicy(Qt.CustomContextMenu)
        self.customContextMenuRequested.connect(self.context_menu)

    def context_menu(self):
        menu = QMenu(self)
        sub_move_annotation = QMenu(menu)
        sub_move_annotation.setTitle("Move Annotation")
        ann_blunder = sub_move_annotation.addAction("?? Blunder")
        ann_blunder.triggered.connect(lambda: self.move_annotation(4))
        ann_mistake = sub_move_annotation.addAction("? Mistake")
        ann_mistake.triggered.connect(lambda: self.move_annotation(2))
        ann_dubious = sub_move_annotation.addAction("?! Dubious Move")
        ann_dubious.triggered.connect(lambda: self.move_annotation(6))
        ann_interesting = sub_move_annotation.addAction("!? Interesting Move")
        ann_interesting.triggered.connect(lambda: self.move_annotation(5))
        ann_good = sub_move_annotation.addAction("! Good Move")
        ann_good.triggered.connect(lambda: self.move_annotation(1))
        ann_brilliant = sub_move_annotation.addAction("!! Brilliant Move")
        ann_brilliant.triggered.connect(lambda: self.move_annotation(3))
        ann_empty = sub_move_annotation.addAction("No Annotation")
        ann_empty.triggered.connect(lambda: self.move_annotation(None))

        sub_pos_annotation = QMenu(menu)
        sub_pos_annotation.setTitle("Position Annotation")
        pos_unclear = sub_pos_annotation.addAction("∞ Unclear")
        pos_unclear.triggered.connect(lambda: self.pos_annotation(13))
        pos_comp_w = sub_pos_annotation.addAction("=/∞ With Compensation for White")
        pos_comp_w.triggered.connect(lambda: self.pos_annotation(44))
        pos_comp_b =sub_pos_annotation.addAction("∞/= With Compensation for Black")
        pos_comp_b.triggered.connect(lambda: self.pos_annotation(45))
        pos_wsb = sub_pos_annotation.addAction("+/= White Slightly Better")
        pos_wsb.triggered.connect(lambda: self.pos_annotation(14))
        pos_bsb = sub_pos_annotation.addAction("=/+ Black Slightly Better")
        pos_bsb.triggered.connect(lambda: self.pos_annotation(15))
        pos_wb = sub_pos_annotation.addAction("+/- White Better")
        pos_wb.triggered.connect(lambda: self.pos_annotation(16))
        pos_bb = sub_pos_annotation.addAction("-/+ Black Better")
        pos_bb.triggered.connect(lambda: self.pos_annotation(17))
        pos_wmb = sub_pos_annotation.addAction("+- White Much Better")
        pos_wmb.triggered.connect(lambda: self.pos_annotation(18))
        pos_bmb = sub_pos_annotation.addAction("-+ Black Much Better")
        pos_bmb.triggered.connect(lambda: self.pos_annotation(19))
        pos_none = sub_pos_annotation.addAction("No Annotation")
        pos_none.triggered.connect(lambda: self.pos_annotation(None))

        add_comment = menu.addAction("Add/Edit Comment")
        add_comment.triggered.connect(self.add_comment)
        delete_comment = menu.addAction("Delete Comment")
        delete_comment.triggered.connect(self.delete_comment)
        menu.addMenu(sub_move_annotation)
        menu.addMenu(sub_pos_annotation)
        menu.addSeparator()
        variant_up = menu.addAction("Move Variant Up")
        variant_up.triggered.connect(self.variant_up)
        variant_down = menu.addAction("Move Variant Down")
        variant_down.triggered.connect(self.variant_down)
        delete_variant = menu.addAction("Delete Variant")
        delete_variant.triggered.connect(self.delete_variant)
        delete_here = menu.addAction("Delete From Here")
        delete_here.triggered.connect(self.delete_from_here)
        menu.addSeparator()
        delete_all_comments = menu.addAction("Delete All Comments")
        delete_all_comments.triggered.connect(self.delete_all_comments)
        delete_all_variants = menu.addAction("Delete All Variants")
        delete_all_variants.triggered.connect(self.delete_all_variants)
        menu.exec_(QCursor.pos())

    def mousePressEvent(self, mouseEvent):
        cursor = self.cursorForPosition(mouseEvent.pos())
        cursor_pos = cursor.position()
        self.go_to_pos(cursor_pos)
        self.old_cursor_pos = cursor_pos

    def move_annotation(self,nag):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            # 0...9 are move annotations according to the pgn standard
            # first remove them, then set new
            selected_state.nags = selected_state.nags - set(range(0,10))
            if(nag != None):
                selected_state.nags.add(nag)
        self.update_san()

    def pos_annotation(self, nag):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            # 10...135 are position annotations according to the
            # pgn standard. first remove them, then add the supplied one
            selected_state.nags = selected_state.nags - set(range(10,136))
            if(nag != None):
                selected_state.nags.add(nag)
        self.update_san()

    def delete_all_comments(self):
        self._rec_delete_comments(self.bv.current.root())
        self.update_san()

    def _rec_delete_comments(self,node):
        node.comment = ""
        for child in node.variations:
            self._rec_delete_comments(child)

    def delete_comment(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            selected_state.comment = ""
        self.update_san()

    def add_comment(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            dialog = DialogWithPlainText()
            dialog.setWindowTitle("Add/Edit Comment")
            dialog.plainTextEdit.setPlainText(selected_state.comment)
            answer = dialog.exec_()
            if answer == True:
                typed_text = dialog.saved_text
                selected_state.comment = typed_text
                self.update_san()


    def variant_up(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None and selected_state.parent != None):
            variations = selected_state.parent.variations
            idx = variations.index(selected_state)
            if(idx < len(variations)-1):
                temp = variations[idx-1]
                variations[idx] = temp
                variations[idx-1] = selected_state
        self.update_san()

    def variant_down(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None and selected_state.parent != None):
            variations = selected_state.parent.variations
            idx = variations.index(selected_state)
            if(idx > 0):
                temp = variations[idx+1]
                variations[idx] = temp
                variations[idx+1] = selected_state
        self.update_san()

    def delete_from_here(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            selected_state.variations = []
        self.update_san()

    def delete_variant(self):
        offset = self.old_cursor_pos
        selected_state = self._get_state_from_offset(offset)
        if(selected_state != None):
            temp = selected_state
            idx = 0
            while(temp.parent != None and len(temp.parent.variations) <= 1):
                temp = temp.parent
            if(temp.parent != None):
                temp.parent.variations.remove(temp)
        self.update_san()

    def delete_all_variants(self):
        node = self.bv.current.root()
        while(node.variations != []):
            node.variations = [node.variations[0]]
            node = node.variations[0]
        self.update_san()

    def _get_state_from_offset(self, offset):
        # next is to update to current status
        text = self.gs.printer.to_san_html(self.gs.current)
        offset_index = self.gs.printer.offset_table
        j = 0
        #for i in range(0,len(offset_index)):
        #    if(offset>= offset_index[i][0] and offset<= offset_index[i][1]):
        #        j = i
        j = 0
        while(j < len(offset_index) and offset > offset_index[j][0]):
            j += 1
        try:
            return offset_index[j-1][1]
        except IndexError:
            return None

    def go_to_pos(self,cursor_pos):
        offset = self.textCursor().position()
        if(cursor_pos > 0):
            if(offset != self.old_cursor_pos):
                self.old_cursor_pos = offset
                selected_state = self._get_state_from_offset(cursor_pos)
                self.gs.current = selected_state
                self.emit(SIGNAL("statechanged()"))
                self.setHtml(self.gs.printer.to_san_html(self.gs.current))

    def on_statechanged(self):
        self.update_san()

    def update_san(self):
        self.setHtml(self.gs.printer.to_san_html(self.gs.current))

    def keyPressEvent(self, event):
        key = event.key()
        if key == Qt.Key_Left:
            if(self.gs.current.parent):
                self.gs.current = self.gs.current.parent
            self.setHtml(self.gs.printer.to_san_html(self.gs.current))
            self.emit(SIGNAL("statechanged()"))
        elif key == Qt.Key_Right:
            variations = self.gs.current.variations
            if(len(variations) > 1):
                move_list = [ self.gs.current.board().san(x.move)
                              for x in self.gs.current.variations ]
                dialog = DialogWithListView(move_list)
                dialog.setWindowTitle("Next Move")
                dialog.listWidget.setFocus()
                answer = dialog.exec_()
                if answer == True:
                    idx = dialog.selected_idx
                    self.gs.current = self.gs.current.variation(idx)
            elif(len(variations) == 1):
                self.gs.current = self.gs.current.variation(0)
            self.setHtml(self.gs.printer.to_san_html(self.gs.current))
            self.emit(SIGNAL("statechanged()"))

