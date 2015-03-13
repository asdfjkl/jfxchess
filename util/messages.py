from PyQt4.QtGui import QMessageBox

def display_mbox(title,content):
    msgBox = QMessageBox()
    msgBox.setIcon(QMessageBox.Information)
    msgBox.setText(title+content)
    #msgBox.setInformativeText(content)
    msgBox.exec_()
