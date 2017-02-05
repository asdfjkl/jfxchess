#include "helper.h"
#include <QPixmap>
#include <QSvgRenderer>
#include <QPainter>

Helper::Helper()
{

}


QPixmap* Helper::fromSvgToPixmap(const QSize &ImageSize, const QString &SvgFile, qreal devicePixelRatio)
{
 QSvgRenderer svgRenderer(SvgFile);
 QPixmap *img = new QPixmap(ImageSize*devicePixelRatio);
 QPainter Painter;

 img->fill(Qt::transparent);

 Painter.begin(img);
 svgRenderer.render(&Painter);
 Painter.end();

 img->setDevicePixelRatio(devicePixelRatio);

 return img;
}
