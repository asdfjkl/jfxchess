# _move this file two level up (dir's) before executing _
# adjust: any source file
# contents of src.pro
# version name in dir's (jerry-3.2.0)
#
rm -r -f jerry
git clone https://github.com/asdfjkl/jerry.git
cd jerry_build
cp ../jerry/debian_package_files/make_tgz.sh .
cp ../jerry/debian_package_files/clean.sh .
rm -r -f jerry-3.2.0
mkdir jerry-3.2.0
cd jerry-3.2.0
mkdir src
mkdir debian
cp ../../jerry/debian_package_files/COPYING ./COPYING
cp ../../jerry/debian_package_files/jerry.desktop ./jerry.desktop
cp ../../jerry/debian_package_files/README ./README
cp ../../jerry/debian_package_files/jerry.pro ./jerry.pro
cp ../../jerry/debian_package_files/src.pro ./src/src.pro
cp -R ../../jerry/debian_package_files/debian/* ./debian
cd src
mkdir books
mkdir chess
mkdir controller
mkdir dialogs
mkdir model
mkdir res
mkdir uci
mkdir various
mkdir viewController
cp ~/books/varied.bin ./books
cp ../../../jerry/chess/*.* ./chess
cp ../../../jerry/controller/*.* ./controller
cp ../../../jerry/dialogs/*.* ./dialogs
cp ../../../jerry/model/*.* ./model
cp -R ../../../jerry/res ./
cp ../../../jerry/uci/*.* ./uci
cp ../../../jerry/various/*.* ./various
cp ../../../jerry/viewController/*.* ./viewController
cp ../../../jerry/*.cpp ./
cp ../../../jerry/*.h ./
cp ../../../jerry/*.qrc ./
cp ../../../jerry/debian_package_files/LICENSE.TXT ./
