from PyQt4.QtCore import *
from PyQt4.QtGui import *

class DialogBrowsePgn(QDialog):

    def get_key(self, key, dict):
        try:
            return dict[key]
        except KeyError:
            return ""

    def __init__(self, offset_headers, filenName, parent=None):
        super(QDialog, self).__init__(parent)

        columns = 7
        rows = len(offset_headers)
        self.setWindowTitle(filenName)
        self.table = QTableWidget(rows,columns)
        self.table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)

        horizontalHeaders = [self.trUtf8("No."),self.trUtf8("White"),self.trUtf8("Black"), \
                             self.trUtf8("Result"), self.trUtf8("Date"), self.trUtf8("ECO"), self.trUtf8("Site")]
        for row, (offset,headers) in enumerate(offset_headers):
            self.table.setItem(row,0,QTableWidgetItem(str(row+1)))
            self.table.setItem(row,1,QTableWidgetItem(self.get_key("White",headers)))
            self.table.setItem(row,2,QTableWidgetItem(self.get_key("Black",headers)))
            self.table.setItem(row,3,QTableWidgetItem(self.get_key("Result",headers)))
            self.table.setItem(row,4,QTableWidgetItem(self.get_key("Date",headers)))
            self.table.setItem(row,5,QTableWidgetItem(self.get_key("Eco",headers)))
            self.table.setItem(row,6,QTableWidgetItem(self.get_key("Site",headers)))
        self.table.setHorizontalHeaderLabels(horizontalHeaders)
        self.table.verticalHeader().hide()
        self.table.setShowGrid(False)
        self.table.resizeColumnsToContents()
        self.table.horizontalHeader().setStretchLastSection(True)

        f = self.fontMetrics()
        rec = QApplication.desktop().screenGeometry()
        self.resize(min(650,rec.width()-100),min(rows*20+130,rec.height()-200))

        vbox = QVBoxLayout()
        vbox.addWidget(self.table)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        vbox.addWidget(buttonBox)

        self.setLayout(vbox)

        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))