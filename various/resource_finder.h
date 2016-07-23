#ifndef RESOURCE_FINDER_H
#define RESOURCE_FINDER_H

// to substract /MacOS to get the "Resources"
// subfolder in MacOS X app bundles
#ifdef __APPLE__
#define SUBTRACT_DIR_PATH 5
#define RES_PATH "Resources/"
#else
#define SUBTRACT_DIR_PATH 0
#define RES_PATH ""
#endif

#include <QString>

class ResourceFinder
{
public:
    ResourceFinder();
    static QString getPath();

};

#endif // RESOURCE_FINDER_H
