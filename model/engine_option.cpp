#include "engine_option.h"
#include <QDebug>

EngineOption::EngineOption()
{
    this->name = QString("");
    this->type = EN_OPT_TYPE_SPIN;
    this->default_spin = 0;
    this->min_spin = 0;
    this->max_spin = 0;
    this->default_check = false;
    this->default_combo = QString("");
    //this->combo_options = new QList<QString>();
    this->default_string = QString("");

    this->spin_val = 0;
    this->check_val = 0;
    this->combo_val = QString("");
    this->string_val = QString("");
}

/*
EngineOption::EngineOption(EngineOption *other)
{
    this->name = other->name;
    this->type = other->type;
    this->default_spin = other->default_spin;
    this->min_spin = other->min_spin;
    this->max_spin = other->max_spin;
    this->default_check = other->default_check;
    this->default_combo = other->default_combo;
    this->combo_options = new QList<QString>();
    for(int i=0;i<other->combo_options->size();i++) {
        this->combo_options->append(other->combo_options->at(i));
    }
    this->default_string = other->default_string;

    this->spin_val = other->spin_val;
    this->check_val = other->check_val;
    this->combo_val = other->combo_val;
    this->string_val = other->string_val;
}
*/




QString EngineOption::toUciOptionString() {

    QString outstr = QString("");
    if(this->type == EN_OPT_TYPE_CHECK && (this->check_val != this->default_check)) {
        QString default_check_str = QString("false");
        if(this->default_check) {
            default_check_str = QString("true");
        }
        if(this->check_val) {
            outstr.append("option name ").append(this->name).append(" type check default ").append(default_check_str);
        } else {
            outstr.append("option name ").append(this->name).append(" type check default ").append(default_check_str);
        }
    } else if(this->type == EN_OPT_TYPE_SPIN && (this->spin_val != this->default_spin)) {
        outstr.append("option name ").append(this->name).append(" type spin");
        outstr.append(" default ").append(QString::number(this->default_spin));
        outstr.append(" min ").append(QString::number(this->min_spin));
        outstr.append(" max ").append(QString::number(this->max_spin));
    } else if(this->type == EN_OPT_TYPE_COMBO && (this->combo_val != this->default_combo)) {
        outstr.append("option name ").append(this->name);
        outstr.append(" type combo");
        outstr.append(" default ").append(this->default_combo);
        for(int i=0;i<this->combo_options.size();i++) {
            QString vari = this->combo_options.at(i);
            outstr.append(" var ").append(vari);
        }
    } else if(this->type == EN_OPT_TYPE_STRING && (this->string_val != this->default_string)) {
        outstr.append("option name ").append(this->name);
        outstr.append(" type string");
        outstr.append(" default ").append(this->default_string);
    }
    return outstr;
}

bool EngineOption::compareByName(const EngineOption &a, const EngineOption &b) {
    if(a.name < b.name) {
        return true;
    } else {
        return false;
    }
}

bool EngineOption::operator<(const EngineOption &other) const
{
    return this->name > other.name;
}




bool EngineOption::restoreFromString(const QString &optionString) {
    bool success = false;
    QString output_i = optionString;
    QRegularExpressionMatch m_opt = REG_EXP_OPTION_NAME.match(output_i);

    if(m_opt.hasMatch() && !m_opt.captured(1).isNull()) {
        QString opt_name = m_opt.captured(1);
        QRegularExpression regExpTypeSpin = QRegularExpression(".*?type spin default (\\d+) min (\\d+) max (\\d+)");
        QRegularExpressionMatch m_spin = regExpTypeSpin.match(output_i);
        if(m_spin.hasMatch()) {
            int def = m_spin.captured(1).toInt();
            int min = m_spin.captured(2).toInt();
            int max = m_spin.captured(3).toInt();
            this->default_spin = def;
            this->min_spin = min;
            this->max_spin = max;
            this->type = EN_OPT_TYPE_SPIN;
            this->name = opt_name;
            this->spin_val = this->default_spin;
            success = true;
        }
        // check option
        QRegularExpression regExpTypeCheck = QRegularExpression(".*?type check default (true|false)");
        QRegularExpressionMatch m_check = regExpTypeCheck.match(output_i);
        if(m_check.hasMatch()) {
            bool default_check = false;
            QString default_check_s = m_check.captured(1);
            if(default_check_s == QString("true")) {
                default_check = true;
            }
            this->default_check = default_check;
            this->type = EN_OPT_TYPE_CHECK;
            this->name = opt_name;
            this->check_val = this->default_check;
            success = true;
        }
        QRegularExpression regExpTypeCombo = QRegularExpression(".*?type combo default ([a-zA-Z0-9_ ]*)");
        QRegularExpressionMatch m_combo = regExpTypeCombo.match(output_i);
        if(m_combo.hasMatch() && !m_combo.captured(1).isNull()) {
            QStringList def_plus_vals = m_combo.captured(1).split(" ");
            this->combo_options.clear();
            this->default_combo = def_plus_vals.at(0);
            int size = def_plus_vals.count();
            for(int i=1;i<size;i++) {
                if(def_plus_vals.at(i) == QString("var") && i+1 < size) {
                    this->combo_options.append(def_plus_vals.at(i+1));
                }
            }
            this->type = EN_OPT_TYPE_COMBO;
            this->combo_val = this->default_combo;
            this->name = opt_name;
            success = true;
        }
        QRegularExpression regExpTypeString = QRegularExpression(".*?type string default ([a-zA-Z0-9\\.]*)");
        QRegularExpressionMatch m_string= regExpTypeString.match(output_i);
        if(m_string.hasMatch() && !m_string.captured(1).isNull()) {
            this->type = EN_OPT_TYPE_STRING;
            this->default_string = m_string.captured(1);
            this->string_val = this->default_string;
            this->name = opt_name;
            success = true;
        }
    }
    return success;
}


QString EngineOption::toUciCommandString() {

    QString outstr = QString("");
    if(this->type == EN_OPT_TYPE_CHECK && (this->check_val != this->default_check)) {
        if(this->check_val) {
            outstr.append("setoption name ").append(this->name).append(" value true");
        } else {
            outstr.append("setoption name ").append(this->name).append(" value false");
        }
    } else if(this->type == EN_OPT_TYPE_SPIN && (this->spin_val != this->default_spin)) {
        outstr.append("setoption name ").append(this->name).append(" value ").append(QString::number(this->spin_val));
    } else if(this->type == EN_OPT_TYPE_COMBO && (this->combo_val != this->default_combo)) {
        outstr.append("setoption name ").append(this->name).append(" value ").append(this->combo_val);
    } else if(this->type == EN_OPT_TYPE_STRING && (this->string_val != this->default_string)) {
        outstr.append("setoption name ").append(this->name).append(" value ").append(this->string_val);
    }
    return outstr;
}
