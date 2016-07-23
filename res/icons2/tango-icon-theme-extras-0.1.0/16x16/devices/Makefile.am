size = 16x16
context = devices

iconsdir = $(themedir)/$(size)/$(context)

icons_DATA =						\
	multimedia-player-dell-dj-pocket.png		\
	multimedia-player-ipod-mini-blue.png		\
	multimedia-player-ipod-mini-gold.png		\
	multimedia-player-ipod-mini-green.png		\
	multimedia-player-ipod-mini-pink.png		\
	multimedia-player-ipod-mini-silver.png		\
	multimedia-player-ipod-nano-black.png		\
	multimedia-player-ipod-nano-white.png		\
	multimedia-player-ipod-shuffle.png		\
	multimedia-player-ipod-standard-color.png	\
	multimedia-player-ipod-standard-monochrome.png	\
	multimedia-player-ipod-U2-color.png		\
	multimedia-player-ipod-U2-monochrome.png	\
	multimedia-player-ipod-video-black.png		\
	multimedia-player-ipod-video-white.png		\
	multimedia-player-motorola-rokr.png

EXTRA_DIST =						\
	$(icons_DATA)

MAINTAINERCLEANFILES =	\
	Makefile.in

install-data-local: install-iconsDATA
	(cd $(DESTDIR)$(themedir)/$(size) && $(ICONMAP) -c $(context))

