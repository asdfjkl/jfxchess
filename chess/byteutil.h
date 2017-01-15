#ifndef BYTEUTIL_H
#define BYTEUTIL_H

#include <QByteArray>

namespace chess {

class ByteUtil
{
public:
    ByteUtil();
    static void append_as_uint8(QByteArray* ba, quint8 val);
    static void append_as_uint16(QByteArray* ba, quint16 val);
    static void append_as_uint32(QByteArray* ba, quint32 val);
    static void append_as_uint64(QByteArray* ba, quint64 val);

    static void prepend_as_uint8(QByteArray* ba, quint8 val);
    static void prepend_as_uint16(QByteArray* ba, quint16 val);
    static void prepend_as_uint32(QByteArray* ba, quint32 val);
    static void prepend_as_uint64(QByteArray* ba, quint64 val);
};

}

#endif // BYTEUTIL_H
