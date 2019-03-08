#ifndef STYLESHEET_H
#define STYLESHEET_H

#include <QMap>

class StyleSheet
{
public:
    static StyleSheet& getInstance();
    StyleSheet(StyleSheet const&)      = delete;
    void operator=(StyleSheet const&)  = delete;
    QString getStylesheet(QString &name);

private:
    StyleSheet() { }
    QMap<QString, QString> styleheets;
    QString read(QString &filename);

};

#endif // STYLESHEET_H
