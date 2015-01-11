from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogEditGameData(QDialog):

    def __init__(self, root, parent=None):
        super(DialogEditGameData,self).__init__(parent)
        self.setWindowTitle("Edit Game Data")

        # to limit the width of the lineEdit's
        f = self.fontMetrics()

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
        l = f.width("2000.00.000")
        self.ed_date.setFixedWidth(l)

        self.ed_round = QLineEdit()
        self.ed_round.setText(root.headers["Round"])
        self.lbl_round = QLabel("Round")
        self.lbl_round.setBuddy(self.ed_round)
        self.ed_round.setFixedWidth(l)

        self.ed_white = QLineEdit()
        self.ed_white.setText(root.headers["White"])
        self.lbl_white = QLabel("White")
        self.lbl_white.setBuddy(self.ed_white)

        self.ed_black = QLineEdit()
        self.ed_black.setText(root.headers["Black"])
        self.lbl_black = QLabel("Black")
        self.lbl_black.setBuddy(self.ed_black)

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

        grpBox = QButtonGroup()
        hbox = QHBoxLayout()
        lbl_res = QLabel("Result")
        hbox.addWidget(self.rb_ww)
        hbox.addWidget(self.rb_draw)
        hbox.addWidget(self.rb_bw)
        hbox.addWidget(self.rb_unclear)
        hbox.addStretch(1)
        #grpBox.setLayout(hbox)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)
        layout = QGridLayout()
        layout.addWidget(self.lbl_event,0,0)
        layout.addWidget(self.ed_event,0,1,1,1)

        layout.addWidget(self.lbl_site,1,0)
        layout.addWidget(self.ed_site,1,1,1,1)

        layout.addWidget(self.lbl_date,2,0)
        layout.addWidget(self.ed_date,2,1,1,1)

        layout.addWidget(self.lbl_round,3,0)
        layout.addWidget(self.ed_round,3,1,1,1)

        layout.addWidget(self.lbl_white,4,0)
        layout.addWidget(self.ed_white,4,1,1,1)

        layout.addWidget(self.lbl_black,5,0)
        layout.addWidget(self.ed_black,5,1,1,1)

        #layout.addWidget(grpBox,7,0,1,3)
        layout.addWidget(lbl_res,7,0,1,1)
        layout.addLayout(hbox,7,1)

        layout.addWidget(buttonBox, 8, 1, 1, 2)
        self.setLayout(layout)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.resize(370, 150)

