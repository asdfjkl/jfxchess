from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogEditGameData(QDialog):

    def __init__(self, root, parent=None):
        super(DialogEditGameData,self).__init__(parent)
        self.setWindowTitle("Edit Game Data")

        self.ed_event = QLineEdit()
        self.ed_event.setText(root.headers["Event"])
        self.lbl_event = QLabel("Event")
        self.lbl_event.setBuddy(self.ed_event)

        self.ed_site = QLineEdit()
        self.ed_site.setText(root.headers["Site"])
        self.lbl_site = QLabel("Site")
        self.lbl_site.setBuddy(self.ed_site)

        self.ed_date = QLineEdit()
        self.ed_date.setText(root.headers["Date"])
        self.lbl_date = QLabel("Date")
        self.lbl_date.setBuddy(self.ed_date)

        self.ed_round = QLineEdit()
        self.ed_round.setText(root.headers["Round"])
        self.lbl_round = QLabel("Round")
        self.lbl_round.setBuddy(self.ed_round)

        self.ed_white = QLineEdit()
        self.ed_white.setText(root.headers["White"])
        self.lbl_white = QLabel("White")
        self.lbl_white.setBuddy(self.ed_white)

        self.ed_black = QLineEdit()
        self.ed_black.setText(root.headers["Black"])
        self.lbl_black = QLabel("Black")
        self.lbl_black.setBuddy(self.ed_black)

        #self.ed_eco = QLineEdit()
        #self.ed_eco.setText(root.headers["ECO"])
        #self.lbl_eco = QLabel("ECO")
        #self.lbl_eco.setBuddy(self.ed_eco)

        self.rb_ww = QRadioButton("1-0")
        self.rb_bw = QRadioButton("0-1")
        self.rb_draw = QRadioButton("½-½")
        self.rb_unclear = QRadioButton("*")
        self.lbl_result = QLabel("Result")

        if(root.headers["Result"]=="1-0"):
            self.rb_ww.setChecked(True)
        elif(root.headers["Result"]=="0-1"):
            self.rb_bw.setChecked(True)
        elif(root.headers["Result"]=="1/2-1/2"):
            self.rb_draw.setChecked(True)
        else:
            self.rb_unclear.setChecked(True)

        grpBox = QGroupBox("Result")
        hbox = QHBoxLayout()
        hbox.addWidget(self.rb_ww)
        hbox.addWidget(self.rb_draw)
        hbox.addWidget(self.rb_bw)
        hbox.addWidget(self.rb_unclear)
        hbox.addStretch(1)
        grpBox.setLayout(hbox)

        #self.plainTextEdit = QtGui.QPlainTextEdit()
        self.saved_text = ""
        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        layout = QGridLayout()
        layout.addWidget(self.lbl_event,0,0)
        layout.addWidget(self.ed_event,0,1,1,2)

        layout.addWidget(self.lbl_site,1,0)
        layout.addWidget(self.ed_site,1,1,1,2)

        layout.addWidget(self.lbl_date,2,0)
        layout.addWidget(self.ed_date,2,1,1,1)

        layout.addWidget(self.lbl_round,3,0)
        layout.addWidget(self.ed_round,3,1)

        layout.addWidget(self.lbl_white,4,0)
        layout.addWidget(self.ed_white,4,1,1,2)

        layout.addWidget(self.lbl_black,5,0)
        layout.addWidget(self.ed_black,5,1,1,2)

        #layout.addWidget(self.lbl_eco,6,0)
        #layout.addWidget(self.ed_eco,6,1)

        layout.addWidget(grpBox,7,0,1,3)

        layout.addWidget(buttonBox, 8, 2, 1, 1)
        self.setLayout(layout)
        #self.ed_eco.setText("D42")
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        # self.ed_eco.textChanged.connect(self.update_text)
        self.resize(370, 150)

    def update_text(self):
        print("text changed")
