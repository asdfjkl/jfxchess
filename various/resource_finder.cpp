#include "resource_finder.h"
#include <QtGui>

ResourceFinder::ResourceFinder()
{
}

QString ResourceFinder::getPath() {
#ifdef __linux__
    QString appPath = "/usr/share/jerry/";
    return appPath;
#endif
#ifdef _WIN32
    QString appPath = QCoreApplication::applicationDirPath();
    return appPath.append(QDir::separator());
#endif

#ifdef __APPLE__
    QString appPath = QCoreApplication::applicationDirPath();
    int l = appPath.length();
    // 5 for "MacOS"
    appPath = appPath.left(l - 5);
    appPath.append("Resources");
    qDebug() << appPath;
    return appPath.append(QDir::separator());
#endif

}
