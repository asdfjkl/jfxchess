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


#include <QApplication>
#include <QObject>
#include <QList>
#include <iostream>
#include "chess/board.h"
#include "chess/move.h"
#include <bitset>
#include "funct.h"
#include "main_window.h"
#include <ctime>
#include <QDebug>
#include <QStyleFactory>

using namespace std;

int main(int argc, char *argv[])
{
    srand(time(NULL));
    chess::FuncT *p = new chess::FuncT();
    //p->run_zobrist_test();
    //p->run_pgnt();
    //p.run_pgn_scant();
    //p.run_sant();
    // p->run_ucit();
    //p->run_pertf();

    QApplication::setAttribute(Qt::AA_EnableHighDpiScaling); // DPI support
    QApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
    // from Qt 5.6 onwards

    QApplication app(argc, argv);
    app.setOrganizationName("dkl");
    app.setApplicationName("Jerry");

    // set application icon
    QIcon *app_icon = new QIcon();
    app_icon->addFile("res/icons/icon16.png", QSize(16,16));
    app_icon->addFile("res/icons//icon24.png", QSize(24,24));
    app_icon->addFile("res/icons//icon32.png", QSize(32,32));
    app_icon->addFile("res/icons/icon48.png", QSize(48,48));
    app_icon->addFile("res/icons/icon256.png",QSize(256,256));
    app.setWindowIcon(*app_icon);

    //app.setStyle(QStyleFactory::create("Windows"));

    MainWindow mainWin;

    QObject::connect(&app, &QApplication::aboutToQuit, &mainWin, &MainWindow::aboutToQuit);

    mainWin.centerAndResize();
    mainWin.show();

    return app.exec();

}
