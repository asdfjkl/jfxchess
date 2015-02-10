class BoardCache():
    def __init__(self):
        self.d = dict()

    def get(self,node):
        try:
            return self.d[node]
        except KeyError:
            board = node.board()
            self.d.update(node,board)
            return board

    def reset(self):
        self.d.clear()