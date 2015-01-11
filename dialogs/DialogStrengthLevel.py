from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogStrengthLevel(QDialog):

    def __init__(self, parent=None, gamestate = None):
        super(DialogStrengthLevel,self).__init__(parent)
        self.setWindowTitle("Set Strength Level")

        lbl_elo = QLabel("Computer Strength")
        lbl_elo.setAlignment(Qt.AlignBottom)
        self.lbl_elo_value = QLabel("1500")
        hboxSlider_elo = QHBoxLayout()
        self.slider_elo = QSlider(Qt.Horizontal, self)
        self.slider_elo.setRange(0,19)
        self.slider_elo.setTickInterval(1)
        self.slider_elo.setTickPosition(2)
        self.slider_elo.setValue(3)
        hboxSlider_elo.addWidget(self.slider_elo)
        hboxSlider_elo.addWidget(self.lbl_elo_value)

        lbl_think_time = QLabel("Computer's Time per Move")
        lbl_think_time.setAlignment(Qt.AlignBottom)
        self.lbl_think_value = QLabel("3 sec(s)")
        f = self.fontMetrics()
        l = f.width("20 sec(s)")
        self.lbl_think_value.setFixedWidth(l)
        hboxSlider_think = QHBoxLayout()
        self.slider_think = QSlider(Qt.Horizontal, self)
        self.slider_think.setRange(1,7) # 1, 2, 3, 5, 10, 15, 30
        self.slider_think.setTickInterval(1)
        self.slider_think.setTickPosition(2)
        self.slider_think.setValue(3)
        hboxSlider_think.addWidget(self.slider_think)
        hboxSlider_think.addWidget(self.lbl_think_value)
        if(gamestate):
            think_time = gamestate.computer_think_time // 1000
            if(think_time == 30):
                think_time = 7
            elif(think_time == 15):
                think_time = 6
            elif(think_time == 10):
                think_time = 5
            elif(think_time == 5):
                think_time = 4
            strength = gamestate.strength_level
            self.slider_think.setValue(think_time)
            self.slider_elo.setValue(strength)
            self.set_lbl_elo_value(strength)
            self.set_lbl_think_value(think_time)


        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)

        layout = QVBoxLayout()
        layout.addWidget(lbl_elo)
        layout.addLayout(hboxSlider_elo)
        layout.addSpacing(20)
        layout.addWidget(lbl_think_time)
        layout.addLayout(hboxSlider_think)
        layout.addSpacing(20)

        layout.addWidget(buttonBox)
        self.setLayout(layout)

        self.slider_elo.valueChanged.connect(self.set_lbl_elo_value)
        self.slider_think.valueChanged.connect(self.set_lbl_think_value)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.resize(370, 150)

    def set_lbl_elo_value(self,val):
        self.lbl_elo_value.setNum(1200+(val*100))

    def set_lbl_think_value(self,val):
        res = val
        if(val > 3):
            if(val == 4):
                res = 5
            elif(val == 5):
                res = 10
            elif(val == 6):
                res = 15
            elif(val ==7):
                res = 30
        self.lbl_think_value.setText(str(res)+" sec(s)")