#ifndef PROFILE_H
#define PROFILE_H

#include <chrono>

class Profile
{
public:
    Profile();
    static std::chrono::milliseconds durationRunAll;

};

#endif // PROFILE_H
