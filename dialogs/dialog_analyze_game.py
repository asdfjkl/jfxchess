from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogAnalyzeGame(QDialog):

    def __init__(self, parent=None, gamestate = None):
        super(DialogAnalyzeGame,self).__init__(parent)


        # to limit the width of the lineEdit's
        f = self.fontMetrics()
        l = f.width("XXXXXXXX")

        self.setWindowTitle(self.trUtf8("Full Game Analysis"))

        lbl_time = QLabel(self.trUtf8("Sec(s) per move:"))
        lbl_threshold = QLabel(self.trUtf8("Threshold (in pawns):"))

        self.sb_secs = QSpinBox()
        self.sb_secs.setRange(1,30)
        self.sb_secs.setValue(3)
        self.sb_secs.setFixedWidth(l)

        self.sb_threshold = QDoubleSpinBox()
        self.sb_threshold.setRange(0.1,1.0)
        self.sb_threshold.setSingleStep(0.1)
        self.sb_threshold.setValue(0.5)
        self.sb_threshold.setFixedWidth(l)

        if(gamestate):
            self.sb_threshold.setValue(gamestate.analysis_threshold)
            self.sb_secs.setValue(gamestate.computer_think_time/1000)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)

        layout = QGridLayout()
        layout.addWidget(lbl_time,1,0)
        layout.addWidget(self.sb_secs,1,1)
        layout.addWidget(lbl_threshold,2,0)
        layout.addWidget(self.sb_threshold,2,1)

        layout1 = QVBoxLayout()
        layout1.addLayout(layout)

        layout1.addWidget(buttonBox)

        self.setLayout(layout1)

        #self.slider_elo.valueChanged.connect(self.set_lbl_elo_value)
        #self.slider_think.valueChanged.connect(self.set_lbl_think_value)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.resize(300, 150)

    """
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
    """