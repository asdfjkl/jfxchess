import os
import sys

def get_platform_wordsize():
    if sys.platform == 'win32':
        return ('win32',32)
    elif 'linux' in  sys.platform:
        wordsize = os.system("uname -m")
        if 'x86_64' in wordsize:
            return('linux',64)
        else:
            return('linux',32)
    elif sys.platform == 'darwin':
        return('darwin',64)

