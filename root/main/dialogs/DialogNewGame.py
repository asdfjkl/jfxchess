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
        btnGroupSide = QButtonGroup(self)
        btnGroupSide.addButton(self.rb_plays_white)
        btnGroupSide.addButton(self.rb_plays_black)
        lbl_choose_side.setAlignment(Qt.AlignBottom)
        hboxSide = QHBoxLayout()
        hboxSide.addWidget(self.rb_plays_white)
        hboxSide.addWidget(self.rb_plays_black)
        hboxSide.setAlignment(Qt.AlignLeft)

        lbl_elo = QLabel("Computer Strength")
        lbl_elo.setAlignment(Qt.AlignBottom)
        self.lbl_slider_value = QLabel("1500")
        hboxSlider = QHBoxLayout()
        self.slider = QSlider(Qt.Horizontal, self)
        self.slider.setRange(0,19)
        self.slider.setTickInterval(1)
        self.slider.setTickPosition(2)
        self.slider.setValue(3)
        hboxSlider.addWidget(self.slider)
        hboxSlider.addWidget(self.lbl_slider_value)

        hboxFixedTime = QHBoxLayout()
        self.lbl_think_time = QLabel("Computer thinks for")
        self.think_secs = QSpinBox()
        self.think_secs.setSuffix(" sec(s)")
        self.think_secs.setMaximum(30)
        self.think_secs.setValue(1)
        hboxFixedTime.addWidget(self.lbl_think_time)
        hboxFixedTime.addWidget(self.think_secs)
        hboxFixedTime.addStretch(1)

        buttonBox = QDialogButtonBox(QDialogButtonBox.Ok| QDialogButtonBox.Cancel)

        layout = QVBoxLayout()
        layout.addWidget(lbl_choose_side)
        layout.addLayout(hboxSide)
        layout.addSpacing(20)
        layout.addWidget(lbl_elo)
        layout.addLayout(hboxSlider)
        layout.addSpacing(20)
        layout.addLayout(hboxFixedTime)
        layout.addSpacing(20)

        layout.addWidget(buttonBox)
        self.setLayout(layout)

        self.slider.valueChanged.connect(self.set_lbl_value)
        self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
        self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
        self.resize(370, 150)

    def set_lbl_value(self,val):
        self.lbl_slider_value.setNum(1200+(val*100))