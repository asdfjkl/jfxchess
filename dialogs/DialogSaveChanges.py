from gui.PieceImages import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogSaveChanges(QMessageBox):

    def __init__(self, parent=None):
        super(DialogSaveChanges, self).__init__(parent)

        self.setText(self.trUtf8("The current game is not saved in the database."))
        self.setInformativeText(self.trUtf8("Would you like to add the current game?"))
        self.setStandardButtons(QMessageBox.Save | QMessageBox.Discard | QMessageBox.Cancel)
        self.setDefaultButton(QMessageBox.Save);
