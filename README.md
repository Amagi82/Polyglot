# Polyglot
Manages and exports localized strings for Android and iOS

---

Polyglot makes it simple to export shared strings for Android and iOS. It'll automatically format normal localized strings, plurals, and string arrays, as well as generating type-safe R files for Swift, similar to Android, e.g.

    R.string.your_string
    R.plural.your_plural(quantity: 2, arg0: 2) 
    R.array.your_array

String formatting can be customized, but by default escapes unacceptable characters for xml on both platforms, and handles strings %s, integers %d, and floats %f.

By default it'll place these files on the desktop and open Finder, but you could easily customize it to export directly to your project. 
