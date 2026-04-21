#!/bin/sh
# new build script

# remember current dir
BUILD_DIR=$(pwd)
echo $BUILD_DIR

# clean old build remains
cd build_linux/build/jar
rm *.jar
cd $BUILD_DIR
cd build_linux/output
rm -r *

# build fat jar including deps
# go to source dir which is parent
# of directory of this helper script
cd $BUILD_DIR
cd ..
mvn clean package
# copy fat jar and platform dependent jfx modules to build dir
cp target/jfxchess-5.0-jar-with-dependencies.jar build/build_linux/build/jar
# assemble with jpackage
cd $BUILD_DIR
cd build_linux
jpackage --java-options '-splash:$APPDIR/splash.png' --type deb -n jfxchess -i build/jar --main-jar jfxchess-5.0-jar-with-dependencies.jar -d output --icon build/linux/app_icon.png --resource-dir build/linux --linux-menu-group games --linux-shortcut --license-file build/LICENSE.TXT --about-url https://github.com/asdfjkl/jfxchess --linux-app-release 5.0 --main-class org.asdfjkl.jfxchess.gui.App
