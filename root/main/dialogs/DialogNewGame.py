from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogNewGame(QDialog):

    def __init__(self, parent=None):
        super(DialogNewGame,self).__init__(parent)
        self.setWindowTitle("New Game")

        self.rb_plays_white = QRadioButton("White")
        self.rb_plays_black = QRadioButton("Black")
        self.rb_plays_white.setChecked(True)

        lbl_choose_side = QLabel("Choose your side:")
        btnGroupSide = QButtonGroup()
        btnGroupSide.addButton(self.rb_plays_white)
        btnGroupSide.addButton(self.rb_plays_black)
        #lbl_choose_side.setBuddy(self.rb_plays_white)
        lbl_choose_side.setAlignment(Qt.AlignBottom)
        hboxSide = QHBoxLayout()
        hboxSide.addWidget(self.rb_plays_white)
        hboxSide.addWidget(self.rb_plays_black)
        hboxSide.setAlignment(Qt.AlignLeft)

        lbl_elo = QLabel("Computer Strength")
        lbl_elo.setAlignment(Qt.AlignBottom)
        self.lbl_slider_value = QLabel("1200")
        hboxSlider = QHBoxLayout()
        self.slider = QSlider(Qt.Horizontal, self)
        self.slider.setRange(0,19)
        self.slider.setTickInterval(1)
        self.slider.setTickPosition(2)
        hboxSlider.addWidget(self.slider)
        hboxSlider.addWidget(self.lbl_slider_value)

        hboxFixedTime = QHBoxLayout()
        lbl_fixed = QLabel("Fixed Time per Move")
        lbl_fixed.setAlignment(Qt.AlignBottom)
        rb_untimed = QRadioButton("Computer thinks for")
        think_secs = QSpinBox()
        think_secs.setSuffix("secs")
        think_secs.setMaximum(99)
        hboxFixedTime.addWidget(rb_untimed)
        hboxFixedTime.addWidget(think_secs)
        hboxFixedTime.addStretch(1)

        lbl_blitz = QLabel("Timed Game")
        lbl_blitz.setAlignment(Qt.AlignBottom)
        blitzGrid = QGridLayout()
        rb_blitz1 = QRadioButton("1 min")
        rb_blitz2_12 = QRadioButton("2 min + 12s inc")
        rb_blitz3_5 = QRadioButton("3 min + 5s inc")
        rb_blitz5 = QRadioButton("5 min")
        rb_blitz15 = QRadioButton("15 min")
        rb_blitz30 = QRadioButton("30 min")
        blitzGrid.addWidget(rb_blitz1,1,1)
        blitzGrid.addWidget(rb_blitz2_12,2,1)
        blitzGrid.addWidget(rb_blitz3_5,3,1)
        blitzGrid.addWidget(rb_blitz5,1,2)
        blitzGrid.addWidget(rb_blitz15,2,2)
        blitzGrid.addWidget(rb_blitz30,3,2)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)

        layout = QVBoxLayout()
        layout.addWidget(lbl_choose_side)
        layout.addLayout(hboxSide)
        layout.addSpacing(20)
        layout.addWidget(lbl_elo)
        layout.addLayout(hboxSlider)
        layout.addSpacing(20)
        layout.addWidget(lbl_fixed)
        layout.addLayout(hboxFixedTime)
        layout.addSpacing(20)
        layout.addWidget(lbl_blitz)
        layout.addLayout(blitzGrid)

        layout.addWidget(buttonBox)
        self.setLayout(layout)

        self.slider.valueChanged.connect(self.set_lbl_value)

        self.resize(370, 150)

    def set_lbl_value(self,val):
        self.lbl_slider_value.setNum(1200+(val*100))