from PyQt4.QtGui import QMessageBox

def display_mbox(title,content):
    msgBox = QMessageBox()
    msgBox.setText(title)
    msgBox.setInformativeText(content)
    msgBox.exec_()