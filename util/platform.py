import os
import sys
import subprocess

def get_platform_wordsize():
    if sys.platform == 'win32':
        return ('win32',32)
    elif 'linux' in  sys.platform:
        wordsize = subprocess.check_output(["uname", "-m"]).decode("utf-8")
        if 'x86_64' in wordsize:
            return('linux',64)
        else:
            return('linux',32)
    elif sys.platform == 'darwin':
        return('darwin',64)

