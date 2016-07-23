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

#include "engine.h"

Engine::Engine()
{
    this->name = QString("");
    this->path = QString("");
    this->uciOptions = new QList<EngineOption*>();
    this->isInternal = false;
}

// create deep copy
Engine::Engine(Engine *e) {
    this->name = QString(e->getName());
    this->path = QString(e->getPath());
    this->uciOptions = new QList<EngineOption*>();
    for(int i=0;i<e->getUciOptions()->count();i++) {
        EngineOption *copy = new EngineOption(*e->getUciOptions()->at(i));
        this->uciOptions->append(copy);
    }
}

void Engine::setAsInternalEngine(bool val) {
    this->isInternal = val;
}


QString Engine::getPath() {
    return this->path;
}

QString Engine::getName() {
    return this->name;
}

bool Engine::isInternalEngine() {
    return this->isInternal;
}

QList<EngineOption*> * Engine::getUciOptions() {
    return this->uciOptions;
}

void Engine::setPath(QString &path) {
    this->path = path;
}

void Engine::setName(QString &name) {
    this->name = name;
}

// return -1 if not found, otherwise index of list
int Engine::existsEngineOption(QString &name) {
    for(int i=0;i<this->uciOptions->count();i++) {
        EngineOption *ei = this->uciOptions->at(i);
        if(ei->name == name) {
            return i;
        }
    }
    return -1;
}
