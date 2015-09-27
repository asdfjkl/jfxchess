from views.piece_images import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogSaveChanges(QMessageBox):

    def __init__(self, parent=None):
        super(DialogSaveChanges, self).__init__(parent)

        self.setText(self.trUtf8("Save changes now?"))
        self.setInformativeText(self.trUtf8("Otherwise, the changes to the current game will be lost."))
        self.setStandardButtons(QMessageBox.Save | QMessageBox.Discard | QMessageBox.Cancel)
        self.setDefaultButton(QMessageBox.Save);
