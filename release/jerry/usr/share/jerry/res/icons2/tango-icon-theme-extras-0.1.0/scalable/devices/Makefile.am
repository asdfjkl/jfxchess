size = scalable
context = devices

iconsdir = $(themedir)/$(size)/$(context)

icons_DATA =						\
	multimedia-player-dell-dj-pocket.svg		\
	multimedia-player-ipod-mini-blue.svg		\
	multimedia-player-ipod-mini-gold.svg		\
	multimedia-player-ipod-mini-green.svg		\
	multimedia-player-ipod-mini-pink.svg		\
	multimedia-player-ipod-mini-silver.svg		\
	multimedia-player-ipod-nano-black.svg		\
	multimedia-player-ipod-nano-white.svg		\
	multimedia-player-ipod-shuffle.svg		\
	multimedia-player-ipod-standard-color.svg	\
	multimedia-player-ipod-standard-monochrome.svg	\
	multimedia-player-ipod-U2-color.svg		\
	multimedia-player-ipod-U2-monochrome.svg	\
	multimedia-player-ipod-video-black.svg		\
	multimedia-player-ipod-video-white.svg		\
	multimedia-player-motorola-rokr.svg

EXTRA_DIST =						\
	$(icons_DATA)

MAINTAINERCLEANFILES =	\
	Makefile.in

install-data-local: install-iconsDATA
	(cd $(DESTDIR)$(themedir)/$(size) && $(ICONMAP) -c $(context))
if ENABLE_LARGE_BITMAPS
	for i in 32 48 64 72 96 128; do \
		pngdir="$(DESTDIR)$(themedir)/$${i}x$${i}/$(context)"; \
		$(mkinstalldirs) $$pngdir; \
		for icon in $(icons_DATA); do \
			$(top_builddir)/svg2png.sh $$i $$pngdir $$icon; \
		done; \
		(cd $(DESTDIR)$(themedir)/$${i}x$${i} && $(ICONMAP) -c $(context)); \
	done
endif

