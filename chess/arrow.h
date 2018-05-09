#ifndef ARROW_H
#define ARROW_H
#include<QPoint>
#include<QColor>

namespace chess {

struct Arrow {
    QPoint from;
    QPoint to;
    QColor color;
};

}

#endif // ARROW_H
