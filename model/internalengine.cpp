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

#include "internalengine.h"
#include <QCoreApplication>
#include <QDir>
#include "various/resource_finder.h"
#include <QDebug>

#include <iostream>

InternalEngine::InternalEngine() : Engine()
{    
    this->name = QString("Stockfish");

#ifdef __linux__
    QString path = QString("/usr/games/stockfish");
    this->setPath(path);
#endif
    
#ifdef _WIN32
    QString path = ResourceFinder::getPath().append("/engine/");
    path = path.append(QString("stockfish.exe"));
    path = QString('"').append(path).append('"');
    path = QDir::toNativeSeparators(QDir::cleanPath(path));
    this->setPath(path);
#endif

#ifdef __APPLE__
    QString path = ResourceFinder::getPath().append("/engine/");
    path = path.append(QString("stockfish"));
    this->setPath(path);
#endif
    //qDebug() << this->getPath();
    this->isInternal = true;   
}
