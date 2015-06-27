from gui.PieceImages import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogSaveChanges(QMessageBox):

    def __init__(self, parent=None):
        super(DialogSaveChanges, self).__init__(parent)

        self.setText(self.trUtf8("The current game has unsaved changes."))
        self.setInformativeText(self.trUtf8("Do you want to save your changes?"))
        self.setStandardButtons(QMessageBox.Save | QMessageBox.Discard | QMessageBox.Cancel)
        self.setDefaultButton(QMessageBox.Save);
