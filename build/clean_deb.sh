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
