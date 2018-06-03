#ifndef ENGINE_OPTION_H
#define ENGINE_OPTION_H
#include <QString>
#include <QList>
#include <QRegularExpression>

const int EN_OPT_TYPE_SPIN = 0;
const int EN_OPT_TYPE_CHECK = 1;
const int EN_OPT_TYPE_COMBO = 2;
const int EN_OPT_TYPE_STRING = 3;

const QRegularExpression REG_EXP_OPTION_NAME = QRegularExpression("option name (.*?) type");

class EngineOption
{
public:
    EngineOption();
    //EngineOption(EngineOption *other);
    //EngineOption(const EngineOption &other);
    QString name;
    int type;
    int default_spin;
    int min_spin;
    int max_spin;
    bool default_check;
    QString default_combo;
    QVector<QString> combo_options;
    QString default_string;
    QString toUciOptionString();
    QString toUciCommandString();

    int spin_val;
    bool check_val;
    QString combo_val;
    QString string_val;

    bool restoreFromString(const QString &optionString);
    static bool compareByName(const EngineOption &a, const EngineOption &b);


private:
    bool operator<(const EngineOption &other) const;

};

#endif // ENGINE_OPTION_H

