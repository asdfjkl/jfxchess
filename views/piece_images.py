from PyQt4.QtSvg import *
from  PyQt4.QtGui import *

class PieceImages:
    def __init__(self):
        self.pieces = {'P': {} , 'R':{},'N':{},'B':{},'Q':{},'K':{},
                       'p':{},'r':{},'n':{},'b':{},'q':{},'k':{}}
        self.rens = {}
        wb = ["w","b"]
        pcs = ["p","r","b","n","q","k"]
        for fst in wb:
            for snd in pcs:
                ren = QSvgRenderer()
                QSvgRenderer.load(ren,"./res/pieces/"+fst+snd+".svg")
                if fst == "w":
                    self.rens[snd.upper()] = ren
                else:
                    self.rens[snd] = ren

    def getWp(self, piece, size):
        imgs = self.pieces[piece]
        if size in imgs:
            return imgs[size]
        else:
            img = QImage(int(size), int(size), QImage.Format_ARGB32) 
            img.fill(QColor(1,1,1,1))
            painter = QPainter()
            painter.begin(img)
            self.rens[piece].render(painter)
            painter.end()
            imgs[size] = img
            return img
