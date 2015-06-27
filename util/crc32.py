import binascii
 
def crc32_from_file(filename):
    buffer = open(filename,'rb')
    buf = buffer.read()
    buf = (binascii.crc32(buf) & 0xFFFFFFFF)
    buffer.close()
    return "%08X" % buf
