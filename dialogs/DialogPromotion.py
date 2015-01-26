from gui.PieceImages import PieceImages
from PyQt4.QtGui import *
from PyQt4.QtCore import *

class DialogPromotion(QDialog):
 
    def __init__(self, whitePromotes, parent=None):
        super(DialogPromotion, self).__init__(parent) 

        self.setWindowTitle("Promotion")

        self.border = 10
        h = self.size().height() - (2*self.border)
        w = (self.size().width()-(2*self.border))/4
        # piece size
        self.ps = min(h,w)
        self.resize(300, 90)
        self.whitePromotes = whitePromotes
        self.final_piece = "Q"
        self.sel_idx = 0
        self.pieceImages = PieceImages()

    def paintEvent(self, event):
        qp = QPainter()
        qp.begin(self)
        # draw images
        h = self.size().height() - (2*self.border)
        w = (self.size().width()-(2*self.border))/4
        self.ps = min(h,w)
        s = self.ps

        lightBlue2 = QColor(166,188,231)
        qp.setBrush(lightBlue2)
        for i in range(0,4):
            if self.sel_idx == i:
                qp.drawRect(self.border+i*s,self.border,s,s)
            qp.drawImage(self.border+i*s,self.border,self.pieceImages.getWp(self.piece_by_idx(i),s))

        qp.end()
    
    def piece_by_idx(self,idx):
        if(self.whitePromotes):
            if(idx == 0):
                return "Q"
            elif(idx == 1):
                return "R"
            elif(idx == 2):
                return "B"
            elif(idx == 3):
                return "N"
        if(not self.whitePromotes):
            if(idx == 0):
                return "q"
            elif(idx == 1):
                return "r"
            elif(idx == 2):
                return "b"
            elif(idx == 3):
                return "n"    
        
    def mousePressEvent(self, mouseEvent):
        sel_idx = mouseEvent.x() // self.ps
        self.sel_idx = sel_idx
        self.update()
    
    def mouseReleaseEvent(self, mouseEvent):
        self.final_piece = self.piece_by_idx(self.sel_idx);
        self.done(True)
                
    def keyPressEvent(self, event):
        key = event.key()
        if key == Qt.Key_Left:
            self.sel_idx = max(self.sel_idx-1,0)
            self.update()
        elif key == Qt.Key_Right:
            self.sel_idx = min(self.sel_idx+1,3)
            self.update()
        elif key == Qt.Key_Return :
            self.final_piece = self.piece_by_idx(self.sel_idx);
            self.done(True)
