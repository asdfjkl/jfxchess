#include "stylesheet.h"
#include "resource_finder.h"
#include <QFile>
#include <QDir>
#include <QTextStream>
#include <QDebug>

StyleSheet& StyleSheet::getInstance()
{
    static StyleSheet instance;

    return instance;
}

QString StyleSheet::read(QString &filename) {

    QFile file(filename);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        return QString("");
    } else {
        QTextStream in(&file);
        QString stylesheet = in.readAll();
        file.close();
        return stylesheet;
    }
}

QString StyleSheet::getStylesheet(QString &name) {

    if(this->styleheets.contains(name)) {
        return this->styleheets.value(name);
    } else {
        QString dirPath = ResourceFinder::getPath();
        QString filename = dirPath.append("res").append(QDir::separator());
        filename.append("stylesheets").append(QDir::separator());
        filename.append(name).append(".css");
        QString stylesheet = this->read(filename);
        if(!stylesheet.isEmpty()) {
            this->styleheets.insert(name, stylesheet);
            return stylesheet;
        } else {
            return QString("");
        }
    }
}
