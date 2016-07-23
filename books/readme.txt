1) BOOK DATA/GENERAL

Everything was done to ensure underlying db-quality. Users not interested in
details may happily skip the whole rest.

For others more interested some details. E.g. things validated for the basic
data base used for both varied/performance:

* human-human games only
* min. age for players: 16 years. So no kid/methusalem games
* max. age for players: 56 years. So no kid/methusalem games
* min. games per player: 4. So no possibly weak validated games
* min. ply: 30
* no * games
* no blind/blitz/rapid/simul games
* no twin games at all. Really meant so not only in Scid-terms

These conditions are really harsh - and lead to a most clean db missing some
more 100.000's games which are quite likely correct. But as download-size matters
too, this strategy seems a senseful compromise between size/quality. And quality
comes first...


2) VARIED.BIN

Broad, but nevertheless quality opening book with following limits:

* avg. elo: >2400 validated for each player
* year range: 1870-2011
* avg. year: 1993
* total: ~2.1 mio. games intermixed with ~233.000 high quality corr games
* player count: ~59.200 matching all conditions
* min. w-score: >=50 %
* min. b-score: >=40 %
* min. occurence: 6 positions each
* max. book-depth: 25 plies
* bin-size: 5.6 MB


3) PERFORMANCE.BIN

Deep opening book trimmed to performant lines with following limits:

* avg. elo: >2500 validated for each player
* year range: 1950-2011
* avg. year: 1993
* total: ~852.000 games intermixed with ~23.000 highest quality corr games
* player count: ~12.400 matching all conditions
* min. w-score: >=55 %
* min. b-score: >=45 %
* min. occurence: 4 positions each
* max. book-depth: 40 plies
* bin-size: 3.7 MB


4) BOOK COMPILATION

By Heinz van Saanen. Latest change: Feb 2011
