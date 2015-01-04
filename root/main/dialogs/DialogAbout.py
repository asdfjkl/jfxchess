from PyQt4.QtGui import *
from PyQt4.QtCore import *
from PyQt4.QtSvg import *


class DialogAbout(QDialog):
    def __init__(self, parent=None):
        super(DialogAbout, self).__init__(parent)

        self.setWindowTitle("About")

        vbox = QVBoxLayout()

        px = QPixmap("../res/icons/icon_complex64.png")

        labelImg = QLabel()
        labelImg.setPixmap(px)
        labelImg.setAlignment(Qt.AlignCenter)

        vbox.addWidget(labelImg)

        label0 = QLabel("<b>Jerry</b><br><br>"+
                        "Version 0.8<br><br>"+
                        "Copyright © 2014 Dominik Klein<br>"+
                        "licensed under GNU GPL<br><br>"+
                        "uses the python-chess library<br>"+
                        "licensed under GNU GPL<br><br>"+
                        "uses 'VARIED.BIN' opening book<br>"+
                        "by Heinz van Saanen<br><br>"+
                        "uses Merida piece images<br>"+
                        "licensed under GNU GPL")
        label0.setAlignment(Qt.AlignCenter)
        vbox.addWidget(label0)

        self.setLayout(vbox)
        self.resize(300, 150)
