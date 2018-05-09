#ifndef COLORED_FIELD_H
#define COLORED_FIELD_H
#include <QPoint>
#include <QColor>

namespace chess {

struct ColoredField {
    QPoint field;
    QColor color;
};

}

#endif // COLORED_FIELD_H
