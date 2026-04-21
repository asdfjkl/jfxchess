#!/bin/sh
# new build script

# remember current dir
BUILD_DIR=$(pwd)
echo $BUILD_DIR

# clean old build remains
cd build_jar
rm *.tar.gz
cd jfxchess
rm *.jar

# build fat jar including deps
# go to source dir which is parent
# of directory of this helper script
cd $BUILD_DIR
cd ..
mvn clean package
# copy fat jar and platform dependent jfx modules to build dir
cp target/jfxchess-5.0-jar-with-dependencies.jar build/build_jar/jfxchess
# assemble with jpackage
cd $BUILD_DIR
cd build_jar
# package into tar.gz
tar -zcvf jfxchess.tar.gz jfxchess
