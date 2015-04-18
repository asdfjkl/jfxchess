#!/bin/bash
rm -r -f release
mkdir release

mkdir release/jerry_linux_x32
cp jerry.py ./release/jerry_linux_x32/

mkdir release/jerry_linux_x32/books
cp books/*.* ./release/jerry_linux_x32/books

mkdir release/jerry_linux_x32/chess
cp chess/*.py ./release/jerry_linux_x32/chess/

mkdir release/jerry_linux_x32/dialogs
cp dialogs/*.py ./release/jerry_linux_x32/dialogs/

mkdir release/jerry_linux_x32/engine
cp engine/* ./release/jerry_linux_x32/engine/

mkdir release/jerry_linux_x32/gui
cp gui/*.py ./release/jerry_linux_x32/gui

mkdir release/jerry_linux_x32/logic
cp logic/*.py ./release/jerry_linux_x32/logic

mkdir release/jerry_linux_x32/res
cp -r res/* ./release/jerry_linux_x32/res

mkdir release/jerry_linux_x32/uci
cp uci/*.py ./release/jerry_linux_x32/uci

mkdir release/jerry_linux_x32/util
cp util/*.py ./release/jerry_linux_x32/util

mkdir release/jerry_linux_x64
cp jerry.py ./release/jerry_linux_x64/

mkdir release/jerry_linux_x64/books
cp books/*.* ./release/jerry_linux_x64/books

mkdir release/jerry_linux_x64/chess
cp chess/*.py ./release/jerry_linux_x64/chess

mkdir release/jerry_linux_x64/dialogs
cp dialogs/*.py ./release/jerry_linux_x64/dialogs

mkdir release/jerry_linux_x64/engine
cp ~/Downloads/stockfish-6-linux/Linux/stockfish_6_x64 ./release/jerry_linux_x64/engine/stockfish_linux

mkdir release/jerry_linux_x64/gui
cp gui/*.py ./release/jerry_linux_x64/gui

mkdir release/jerry_linux_x64/res
cp -r res/* ./release/jerry_linux_x64/res

mkdir release/jerry_linux_x64/uci
cp uci/*.py ./release/jerry_linux_x64/uci

mkdir release/jerry_linux_x64/util
cp util/*.py ./release/jerry_linux_x64/util

mkdir release/jerry_linux_x64/logic
cp logic/*.py ./release/jerry_linux_x64/logic

tar -zcvf ./release/jerry_linux_x86.tar.gz ./release/jerry_linux_x32
tar -zcvf ./release/jerry_linux_x64.tar.gz ./release/jerry_linux_x64

# now build the debian package
# edit the name of the control file to
# control the resulting architecture.
# here we only build for i386, since we 
# can't build for amd64 on an i386 system
# and vice-versa
mkdir release/jerry
mkdir release/jerry/DEBIAN
mkdir release/jerry/usr
mkdir release/jerry/usr/bin
mkdir release/jerry/usr/share
mkdir release/jerry/usr/share/applications
mkdir release/jerry/usr/share/jerry
cp -r release/jerry_linux_x32/* release/jerry/usr/share/jerry/
cp debian_package_files/jerry.desktop release/jerry/usr/share/applications/
cp debian_package_files/jerry release/jerry/usr/bin/
cp debian_package_files/control_i386 release/jerry/DEBIAN/control
cp debian_package_files/postrm release/jerry/DEBIAN/postrm
chmod u+x release/jerry/usr/share/jerry/jerry.py
cd release/
dpkg-deb -b jerry .
cd ..
