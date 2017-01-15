#include "byteutil.h"

chess::ByteUtil::ByteUtil()
{
}

void chess::ByteUtil::append_as_uint8(QByteArray* ba, quint8 r) {
    ba->append(r);
}

void chess::ByteUtil::append_as_uint16(QByteArray* ba, quint16 r) {
    ba->append(quint8(r>>8));
    ba->append(quint8(r));
}

void chess::ByteUtil::append_as_uint32(QByteArray* ba, quint32 r) {
    chess::ByteUtil::append_as_uint16(ba, quint16(r>>16));
    chess::ByteUtil::append_as_uint16(ba, quint16(r));
}

void chess::ByteUtil::append_as_uint64(QByteArray* ba, quint64 r) {
    chess::ByteUtil::append_as_uint32(ba, quint32(r>>32));
    chess::ByteUtil::append_as_uint32(ba, quint32(r));
}


void chess::ByteUtil::prepend_as_uint8(QByteArray* ba, quint8 r) {
    ba->prepend(r);
}

void chess::ByteUtil::prepend_as_uint16(QByteArray* ba, quint16 r) {
    ba->prepend(quint8(r));
    ba->prepend(quint8(r>>8));
}

void chess::ByteUtil::prepend_as_uint32(QByteArray* ba, quint32 r) {
    chess::ByteUtil::prepend_as_uint16(ba, quint16(r));
    chess::ByteUtil::prepend_as_uint16(ba, quint16(r>>16));
}

void chess::ByteUtil::prepend_as_uint64(QByteArray* ba, quint64 r) {
    chess::ByteUtil::prepend_as_uint32(ba, quint32(r));
    chess::ByteUtil::prepend_as_uint32(ba, quint32(r>>32));
}

