#include "profile.h"

Profile::Profile()
{

}

std::chrono::nanoseconds Profile::durationRunAll;
std::chrono::nanoseconds Profile::first_part;
std::chrono::nanoseconds Profile::pseudo_generation;
std::chrono::nanoseconds Profile::filter_pseudos;
std::chrono::nanoseconds Profile::filter_legal_check;
std::chrono::nanoseconds Profile::parse_san_fast;

