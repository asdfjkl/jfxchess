#include "resource_finder.h"
#include <QtGui>

ResourceFinder::ResourceFinder()
{
}

QString ResourceFinder::getPath() {
    QString appPath = QCoreApplication::applicationDirPath();
    int l = appPath.length();
    appPath = appPath.left(l - SUBTRACT_DIR_PATH);
    appPath.append(QString(RES_PATH));

    return appPath;
}
