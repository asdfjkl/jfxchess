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
