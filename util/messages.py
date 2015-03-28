from PyQt4.QtGui import QMessageBox

def display_mbox(title,content):
    msgBox = QMessageBox()
    msgBox.setWindowTitle("Jerry")
    msgBox.setIcon(QMessageBox.Information)
    msgBox.setText(title+"\n"+content)
    msgBox.exec_()
