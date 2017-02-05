#ifndef HELPER_H
#define HELPER_H

#include <QPixmap>

class Helper
{
public:
    Helper();
    static QPixmap* fromSvgToPixmap(const QSize &ImageSize, const QString &SvgFile, qreal devicePixelRatio);
};

#endif // HELPER_H
