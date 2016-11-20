#ifndef RESOURCE_FINDER_H
#define RESOURCE_FINDER_H

// to substract /MacOS to get the "Resources"
// subfolder in MacOS X app bundles
#ifdef __APPLE__
#ifndef SUBTRACT_DIR_PATH
#define SUBTRACT_DIR_PATH 5
#endif
#ifndef RES_PATH
#define RES_PATH "Resources/"
#endif
#else
#ifndef SUBTRACT_DIR_PATH
#define SUBTRACT_DIR_PATH 0
#endif
#ifndef RES_PATH
#define RES_PATH ""
#endif
#endif

#include <QString>

class ResourceFinder
{
public:
    ResourceFinder();
    static QString getPath();

};

#endif // RESOURCE_FINDER_H
