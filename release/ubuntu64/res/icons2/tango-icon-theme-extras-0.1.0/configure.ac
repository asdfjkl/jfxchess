# Process this file with autoconf to produce a configure script.
AC_PREREQ(2.53)

AC_INIT([tango-icon-theme-extras], [0.1.0])
AC_CONFIG_SRCDIR([svg2png.sh.in])

AM_INIT_AUTOMAKE([1.9 tar-ustar])

PKG_PROG_PKG_CONFIG([0.19])

# Workaround to make aclocal get the right flags
AC_SUBST(ACLOCAL_AMFLAGS, "\${ACLOCAL_FLAGS}")

# Define the toplevel path here
AC_SUBST(themedir, "\${datadir}/icons/Tango")

UTILS_REQUIRED=0.6.0

AC_MSG_CHECKING([icon-naming-utils >= $UTILS_REQUIRED])
PKG_CHECK_EXISTS(icon-naming-utils >= $UTILS_REQUIRED,
                 have_utils=yes, have_utils=no)
if test "x$have_utils" = "xyes"; then
   UTILS_PATH="`$PKG_CONFIG --variable=program_path icon-naming-utils`"
   ICONMAP="$UTILS_PATH/icon-name-mapping"
   AC_SUBST(ICONMAP)
   AC_MSG_RESULT([yes])
else
   AC_MSG_RESULT([no])
   AC_MSG_ERROR([icon-naming-utils >= $UTILS_REQUIRED is required to build
                 and install tango-icon-theme])
fi

# Check for convert from ImageMagick to add the 1x1 frame to 22x22 icons
AC_ARG_ENABLE(icon-framing,
   AC_HELP_STRING([--disable-icon-framing],
                   [disable creation of 24x24 icons [default=enabled]]),
    enable_framing=$enableval,
    enable_framing=yes)
AM_CONDITIONAL(ENABLE_ICON_FRAMING, test x$enable_framing = xyes)

if test "x$enable_framing" = "xyes"; then
   PKG_CHECK_EXISTS([ImageMagick >= 5.5.7],
                    [enable_framing=yes], [enable_framing=no])
   if test "x$enable_framing" = "xyes"; then
      AC_PATH_PROG(CONVERT, convert)
      if test -z "$CONVERT"; then
      	 AC_MSG_ERROR([ImageMagick is required to build tango-icon-theme])
      fi

      # Add the command line options we need here
      CONVERT="$CONVERT -bordercolor Transparent -border 1x1"
      AC_SUBST(CONVERT)
   else
      AC_MSG_ERROR([ImageMagick is required to build tango-icon-theme])
   fi
fi

# Check for rsvg or ksvgtopng for creating large scale bitmaps from svg
AC_ARG_ENABLE(png-creation,
   AC_HELP_STRING([--enable-png-creation],
                   [enable creating png images from svg [default=disabled]]),
    enable_large_bitmaps=$enableval,
    enable_large_bitmaps=no)

SVGCONVERT=""
AC_MSG_CHECKING([SVG to PNG conversion method])
if test "x$enable_large_bitmaps" = "xyes"; then
   PKG_CHECK_EXISTS([librsvg-2.0 >= 2.12.3],
                    [enable_large_bitmaps=yes], [enable_large_bitmaps=no])
   if test "x$enable_large_bitmaps" = "xyes"; then
      svgconvert_prog="rsvg"
   else
      svgconvert_prog="ksvgtopng"
   fi
   AC_MSG_RESULT([$svgconvert_prog])
   AC_PATH_PROG(SVGCONVERT, $svgconvert_prog)
   if test -z "$SVGCONVERT"; then
      AC_MSG_ERROR([You need librsvg >= 2.12.3 or ksvgtopng installed to create large scale bitmaps])
   else
      enable_large_bitmaps=yes
   fi
else
   AC_MSG_RESULT([none])
fi
AC_SUBST(SVGCONVERT)
AM_CONDITIONAL(ENABLE_LARGE_BITMAPS, test x$enable_large_bitmaps = xyes)

AC_CONFIG_FILES([
Makefile
16x16/Makefile
16x16/devices/Makefile
22x22/Makefile
22x22/devices/Makefile
scalable/Makefile
scalable/devices/Makefile
svg2png.sh
])	

AC_OUTPUT

chmod +x svg2png.sh
