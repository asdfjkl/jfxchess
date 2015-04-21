from PyQt4.QtGui import *
from PyQt4.QtCore import *
from PyQt4.QtSvg import *


class DialogAbout(QDialog):
    def __init__(self, parent=None):
        super(DialogAbout, self).__init__(parent)

        self.setWindowTitle("About")

        vbox = QVBoxLayout()

        #px = QPixmap("res/icons/icon_complex64.png")

        #labelImg = QLabel()
        #labelImg.setPixmap(px)
        #labelImg.setAlignment(Qt.AlignCenter)

        #vbox.addWidget(labelImg)

        label0 = QLabel(_("<b>Jerry</b><br><br>"+
                        "Version 1.02<br><br>"+
                        "Copyright © 2014, 2015 Dominik Klein<br>"+
                        "licensed under GNU GPL 3<br><br>"+
                        "<b>Credits</b><br><br>"+
                        "Stockfish Chess Engine<br>"+
                        "by the Stockfish-Team<br><br>"
                        "python-chess library<br>"+
                        "by Niklas Fiekas<br><br>"+
                        "'VARIED.BIN' opening book<br>"+
                        "by Heinz van Saanen<br><br>"+
                        "Merida piece images<br>"+
                        "from pychess/pychess-team<br><br>"+
                        "all licensed under GNU GPL 3"))
        label0.setAlignment(Qt.AlignCenter)
        vbox.addWidget(label0)

        self.setLayout(vbox)
        self.resize(300, 150)
