/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#ifndef ENGINE_H
#define ENGINE_H
#include <QString>
#include "engine_option.h"

class Engine
{
public:
    Engine();
    //Engine(Engine *e);
    QString getPath();
    QString getName();
    void setPath(QString &path);
    void setName(QString &name);
    QVector<EngineOption> getUciOptions();
    int existsEngineOption(QString &name);
    void addEngineOption(EngineOption o);
    void removeEngineOption(int idx_option);
    void clearAllEngineOptions();
    void setUciSpinOption(int opt_idx, int value);
    void setUciComboOption(int opt_idx, QString value);
    void setUciCheckOption(int opt_idx, bool value);
    void setUciStringOption(int opt_idx, QString value);
    void setEngineOptions(QVector<EngineOption> opts);


protected:
    QString name;
    QString path;
    QVector<EngineOption> uciOptions;
    bool isInternal;
    bool isActive;
};

#endif // ENGINE_H
