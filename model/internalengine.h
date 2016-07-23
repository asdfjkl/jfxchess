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

#ifndef INTERNALENGINE_H
#define INTERNALENGINE_H
#include "engine.h"


#ifdef Q_OS_WIN32
#define INT_ENGINE_FN "stockfish_win.exe"
#endif

#ifdef Q_OS_WIN64
#define INT_ENGINE_FN "stockfish_win.exe"
#endif

#ifdef Q_OS_LINUX
#ifdef __i386__
#define INT_ENGINE_FN "stockfish_linux_32"
#endif
#ifdef __x86_64__
#define INT_ENGINE_FN "stockfish_linux_64"
#endif
#endif

#ifdef __APPLE__
#define INT_ENGINE_FN "stockfish_darwin_64"
#endif



class InternalEngine : public Engine
{
public:
    InternalEngine();
};

#endif // INTERNALENGINE_H
