#include "resource_finder.h"
#include <QtGui>

ResourceFinder::ResourceFinder()
{
}

QString ResourceFinder::getPath() {
#ifdef __linux__
    QString appPath = "/usr/share/jerry/";
    return appPath;
#else
    QString appPath = QCoreApplication::applicationDirPath();
    return appPath.append(QDir::separator());
#endif
}
