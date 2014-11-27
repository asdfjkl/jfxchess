from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogWithPlainText(QDialog):
    def __init__(self, parent=None):
        super(DialogWithPlainText, self).__init__(parent) 
        
        self.plainTextEdit = QPlainTextEdit()
        self.saved_text = ""
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        layout = QGridLayout()
        layout.addWidget(self.plainTextEdit,0,1)
        layout.addWidget(buttonBox, 3, 0, 1, 3)
        self.setLayout(layout)
        self.plainTextEdit.setPlainText("foo")
        self.saved_text = "foo"
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.plainTextEdit.textChanged.connect(self.update_text)
        self.resize(300, 150)
        
    def update_text(self):
        temp = self.plainTextEdit.toPlainText()
        self.saved_text = temp.replace('\n', ' ').replace('\r', '')
