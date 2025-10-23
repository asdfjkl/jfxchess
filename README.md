# JFXChess - JavaFX Chess GUI

![alt text](https://raw.githubusercontent.com/asdfjkl/jerry/master/jfxchess.png)

## About
JFXChess is a cross-platform chess graphical user interface.

* based around one of the world's best chess program Stockfish
* play against the computer
* adjust strength levels to match your skill
* enter, edit and analyse games
* fully automatic game analysis
* read and save games in standard PGN format
* copy and paste FEN positions and pgn games from clipboard
* cross-platform: modern look on Linux and Windows
* handle large (i.e. 1 GB) PGN files
* easy and intuitive GUI
* free software (GNU GPL 2), no adware/spyware

## Download

Current Version: 4.5.0

* Windows
  - Exe [Installer](https://github.com/asdfjkl/jfxchess/releases)
* Linux
  - install [.deb](https://github.com/asdfjkl/jfxchess/releases) 
  - or [generic binary](https://github.com/asdfjkl/jfxchess/releases)

## Blog

Once in a while I blog about the development of JFXChess at my [blog](https://buildingjerry.wordpress.com).

## Changelog

Version 4.5.0 (October 2025)
 * new dark and light theme
 * packaged Stockfish 17.1
 * new bots to play against
 * more engine info
 * drag 'n' drop support (thanks @TTorell)
 * ability to select own opening book (can be created by e.g. [bookmaker](https://github.com/asdfjkl/bookmaker))
 * various bugfixes

Version 4.3.0 (December 2024)
 * improved drag'n'drop pieces to setup a new position
 * finer-grained centipawn threshold when analysing games
 * new opening book based on lichess master games
 * packaged stockfish 17
 * multipv up to 64 lines (for custom engines; for packaged engine 4 lines)
 * reworked the dialog to enter game information to prevent accidently entering wrong date formats
 * added ability to setup up header tags for White and Black's Elo
 * ability to setup en passant square when entering a new position
 * better word wrapping when entering text comments
 * when opening a PGN, always scroll to entry 0
 * better support for PGN files in UTF-8; removed support for legacy iso-latin-1 encodings
 * moved linux snap to core22
 * fixed a bug where pasting pgn or fen via clipboard did not work
 * fixed a bug where replacing (overwriting) a game in a PGN did not work
 * fixed a bug where the last save and open directory path where not remembered
 * fixed a bug where the game result was not stored in PGN despite being set in game information dialog

Version 4.2.1 (July 2022)
 * drag and drop when setting up a position (thx @TTorel)
 * adjust strength for any engine that supports 'uciLimitStrength'
 * flat style dialogs to better match theme
 * nicer looking opening book view
 * icon to indicate whose turn it is
 * computer's books moves are not selected randomly, but depend on chosen engine strength and opening
 * moves in bookview are shown in SAN notation instead of uci notation
 * some layout fixes
 * deleting games in database view
 * several bugfixes

Version 4.2.0 (Dec 2021)
 * opening book with statistics
 * dark mode
 * several bugfixes

Version 4.1.1 (Jan 2021)
 * fixed bug when starting a new game

Version 4.1.0 (Jan 2021)
 * several bugfixes (see issue tracker)
 * database: edit & save for large PGNs
 * colorize last move
 * drawing arrows & colorizing fields
 * keyboard shortcuts
 * fullscreen mode
 * option to hide toolbar

Version 4.0.0 (Oct 2020)
 * complete rewrite
 * position search even in very large PGN files

Version 3.2.1 (Dec 2019)
 * bug fix in displaying move and position annotations
 * ability to flip board when entering a new position

Version 3.2.0 (July 2019)
 * MultiPV up to 4 lines
 * ability to handle and search (metadata) of large (> 1 GB) PGN files
 * user defined font-sizes

## Donate

You can support the development of JFXChess by donating via PayPal.

[![paypal](https://www.paypalobjects.com/en_US/DK/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate?hosted_button_id=9K2JDF5YBDZT6)
 
## Keyboard Shortcuts

- CTRL + N : New Game
- CTRL + O : Open File (.pgn)
- CTRL + S : Save Game
- CTRL + C : Copy Game (.pgn) to clipboard
- CTRL + V : Paste Game (.pgn) or position (FEN) from clipboard
- CTRL + E : Setup new position
- CTRL + F : Flip Board
- CTRL + A : Switch to "Analysis Mode"
- CTRL + M : Switch to "Enter Moves Mode" (deactivate analysis)
- F11 : Switch to Full-Screen Mode
- CTRL + RIGHT ARROW : Load next game (when .pgn has been opened)
- CTRL + LEFT ARROW : Load previous game (when .pgn has been opened)

## Roadmap
 
various ideas for future versions:

- [DONE] opening book support
- [DONE] replace Stockfish skill level with UciLimitStrength (any engine)
- more options to edit/replace/add games in pgn files
- visual representation (i.e. graph) of game progress
- ability to set default database
- ship a default database of a few thousand interesting games 
- option to show arrow that illustrates engine top-move in analysis
- [DONE] "drag" pieces in enter-position dialog
- [DONE] show more engine info: Tablebase Hits and Hash percentage
- ship with pre-configured 4-piece (size!) tablebases
- ability to click on engine lines 
- [DONE] drag and drop for pgn files
