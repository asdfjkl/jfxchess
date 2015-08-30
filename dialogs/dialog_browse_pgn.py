from PyQt4.QtCore import *
from PyQt4.QtGui import *
from dialogs.dialog_edit_game_data import DialogEditGameData
import re
import PyQt4

class DialogBrowsePgn(QDialog):

    def get_key(self, key, dict):
        try:
            return dict[key]
        except KeyError:
            return ""

    def __init__(self, database, parent=None):
        super(QDialog, self).__init__(parent)

        self.database = database
        columns = 7
        rows = len(database.entries)
        self.setWindowTitle(database.filename)
        self.table = QTableWidget(rows,columns)
        self.table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setSelectionMode(QAbstractItemView.SingleSelection)

        horizontalHeaders = [self.trUtf8("No."),self.trUtf8("White"),self.trUtf8("Black"), \
                             self.trUtf8("Result"), self.trUtf8("Date"), self.trUtf8("ECO"), self.trUtf8("Site")]
        self.table.setHorizontalHeaderLabels(horizontalHeaders)
        self.table.verticalHeader().hide()
        self.table.setShowGrid(False)
        self.draw_all_items()
        self.table.resizeColumnsToContents()
        self.table.horizontalHeader().setStretchLastSection(True)
        if not database.index_current_game == None:
            self.table.selectRow(database.index_current_game)
        else:
            self.table.selectRow(0)

        f = self.fontMetrics()
        rec = QApplication.desktop().screenGeometry()
        self.resize(min(650,rec.width()-100),min(rows*20+130,rec.height()-200))

        #search_lbl = QLabel(self.trUtf8("Search:"))
        self.search_field = QLineEdit()
        self.button_search = QPushButton(self.trUtf8("Search"))
        self.button_reset = QPushButton(self.trUtf8("Reset"))
        #self.hlp_button = QPushButton("? ")
        hbox_lbl = QHBoxLayout()
        hbox_lbl.addWidget(self.search_field)
        hbox_lbl.addSpacerItem(QSpacerItem(20, 1))
        hbox_lbl.addWidget(self.button_search)
        hbox_lbl.addWidget(self.button_reset)
        #hbox_lbl.addWidget(self.hlp_button)

        vbox = QVBoxLayout()
        vbox.addLayout(hbox_lbl)
        vbox.addWidget(self.table)

        self.button_edit_header = QPushButton(self.trUtf8(("Edit Headers")))
        self.button_delete      = QPushButton(self.trUtf8("Delete"))
        buttonBox = QDialogButtonBox(Qt.Horizontal)
        buttonBox.addButton(self.button_edit_header, QDialogButtonBox.ActionRole)
        buttonBox.addButton(self.button_delete, QDialogButtonBox.ActionRole)
        buttonBox.addButton(QDialogButtonBox.Ok)
        buttonBox.addButton(QDialogButtonBox.Cancel)

        vbox.addWidget(buttonBox)

        self.setLayout(vbox)

        self.search_field.setFocus()

        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.button_delete.clicked.connect(self.on_delete)
        self.button_edit_header.clicked.connect(self.on_edit_headers)
        self.button_search.clicked.connect(self.on_search)
        self.button_reset.clicked.connect(self.draw_all_items)

    def keyPressEvent(self,keyEvent):
        print("captured key event")
        if(keyEvent.key() == Qt.Key_Return or keyEvent.key() == Qt.Key_Enter):
            print("return was pressed in browse dialog")
            if(self.search_field.hasFocus()):
                self.on_search()
            else:
                print("... search field has no focus")
                QDialog.keyPressEvent(self,keyEvent)
        else:
            QDialog.keyPressEvent(self,keyEvent)

    def on_edit_headers(self):
        # get selected game
        items = self.table.selectedItems()
        if(len(items) > 0):
            idx_selected_game = int(items[0].text())-1
            print("selected: "+str(idx_selected_game))
            # load selected game
            game = self.database.load_game(idx_selected_game)
            dlg = DialogEditGameData(game.root())
            answer = dlg.exec_()
            if(answer):
                game.root().headers["Event"] = dlg.ed_event.text()
                game.root().headers["Site"] = dlg.ed_site.text()
                game.root().headers["Date"] = dlg.ed_date.text()
                game.root().headers["Round"] = dlg.ed_round.text()
                game.root().headers["White"] = dlg.ed_white.text()
                game.root().headers["Black"] = dlg.ed_black.text()
                if(dlg.rb_ww.isChecked()):
                    game.root().headers["Result"] = "1-0"
                elif(dlg.rb_bw.isChecked()):
                    game.root().headers["Result"] = "0-1"
                elif(dlg.rb_draw.isChecked()):
                    game.root().headers["Result"] = "1/2-1/2"
                elif(dlg.rb_unclear.isChecked()):
                    game.root().headers["Result"] = "*"
                self.database.update_game(idx_selected_game,game)
                self.table.setItem(idx_selected_game,1,QTableWidgetItem(self.get_key("White",game.root().headers)))
                self.table.setItem(idx_selected_game,2,QTableWidgetItem(self.get_key("Black",game.root().headers)))
                self.table.setItem(idx_selected_game,3,QTableWidgetItem(self.get_key("Result",game.root().headers)))
                self.table.setItem(idx_selected_game,4,QTableWidgetItem(self.get_key("Date",game.root().headers)))
                self.table.setItem(idx_selected_game,5,QTableWidgetItem(self.get_key("ECO",game.root().headers)))
                self.table.setItem(idx_selected_game,6,QTableWidgetItem(self.get_key("Site",game.root().headers)))

    def on_delete(self):
        # get selected game
        items = self.table.selectedItems()
        if(len(items) > 0):
            idx_selected_game = int(items[0].text())-1
            print("selected: "+str(idx_selected_game))

            # ask user if he is sure
            ask_sure = QMessageBox()
            ask_sure.setText(self.trUtf8("Please confirm."))
            ask_sure.setInformativeText(self.trUtf8("This will delete the selected game permanently."))
            ask_sure.setStandardButtons(QMessageBox.Ok | QMessageBox.Cancel)
            ask_sure.setDefaultButton(QMessageBox.Cancel)

            if ask_sure.exec_() == QMessageBox.Ok:
                self.table.removeRow(idx_selected_game)
                for i in range(idx_selected_game, self.table.rowCount()):
                    old_idx = int(self.table.item(i,0).text())
                    print(str(old_idx))
                    self.table.setItem(i,0,QTableWidgetItem(str(old_idx-1)))
                self.database.delete_game_at(idx_selected_game)

    def draw_all_items(self):
        self.table.clear()
        self.table.setRowCount(self.database.no_of_games())
        for row, entry in enumerate(self.database.entries):
            self.table.setItem(row,0,QTableWidgetItem(str(row+1)))
            self.table.setItem(row,1,QTableWidgetItem(self.get_key("White",entry.headers)))
            self.table.setItem(row,2,QTableWidgetItem(self.get_key("Black",entry.headers)))
            self.table.setItem(row,3,QTableWidgetItem(self.get_key("Result",entry.headers)))
            self.table.setItem(row,4,QTableWidgetItem(self.get_key("Date",entry.headers)))
            self.table.setItem(row,5,QTableWidgetItem(self.get_key("ECO",entry.headers)))
            self.table.setItem(row,6,QTableWidgetItem(self.get_key("Site",entry.headers)))

    def on_search(self):
        search_txt = self.search_field.text()
        if(not search_txt == ""):
            #mTestTable->setRowCount(0);
            self.table.clear()
            self.table.setRowCount(self.database.no_of_games())
            #for i in reversed(range(self.table.rowCount())):
            #    self.table.removeRow(i)
            search_term = re.compile(self.search_field.text(),re.IGNORECASE)
            size = self.database.no_of_games()
            pDialog = QProgressDialog(self.trUtf8("Searching..."),None,0,size,self)
            pDialog.setWindowModality(PyQt4.QtCore.Qt.WindowModal)
            pDialog.show()
            rowcount = 0
            for row, entry in enumerate(self.database.entries):
                str_white = self.get_key("White",entry.headers)
                str_black = self.get_key("Black",entry.headers)
                str_result = self.get_key("Result",entry.headers)
                str_date = self.get_key("Date",entry.headers)
                str_eco = self.get_key("ECO",entry.headers)
                str_site = self.get_key("Site",entry.headers)
                if( (not search_term.match(str_white) == None) or
                    (not search_term.match(str_black) == None) or
                    (not search_term.match(str_result) == None) or
                    (not search_term.match(str_date) == None) or
                    (not search_term.match(str_eco) == None) or
                    (not search_term.match(str_site) == None)):
                        self.table.setItem(rowcount,0,QTableWidgetItem(str(row+1)))
                        self.table.setItem(rowcount,1,QTableWidgetItem(str_white))
                        self.table.setItem(rowcount,2,QTableWidgetItem(str_black))
                        self.table.setItem(rowcount,3,QTableWidgetItem(str_result))
                        self.table.setItem(rowcount,4,QTableWidgetItem(str_date))
                        self.table.setItem(rowcount,5,QTableWidgetItem(str_eco))
                        self.table.setItem(rowcount,6,QTableWidgetItem(str_site))
                        rowcount += 1
                QApplication.processEvents()
                pDialog.setValue(row)
            self.table.setRowCount(rowcount)
            if(rowcount > 0):
                self.table.selectRow(0)
            pDialog.close()
