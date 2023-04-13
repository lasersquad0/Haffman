# About

This is a project the implements different compression techniques such as:
- Huffman encoding. Two different implementations - ordinary and adaptive.
- Range (arithmetic) encoding. Four implementations - 32bit and 64 bit. Each of them can be either ordinary or adaptive.
- RLE->BWT->MTF>RLE transformations which are done before actual compression by one of the methods above.
- Several simple models (order0 fixed or adaptive and order1) that can be used with any compression method. 
- Compression/decompression can be done in several threads. 

# How to use

This is a command line compressor written in Java.
You need Java RTE installed to run this tool.

To run it use this command line:
`java -jar Huffman.jar`

This will show you list of command line parameters like below. 

```
usage: Huffman [-a <<archive> <files...>>] [-b <arg>] [-hf] [-l <archive>] [-o <arg>] [-ra] [-ra32] [-ra64] [-rabit] [-range] [-range32] [-range64]
[-sm] [-t <arg>] [-v] [-x <archive>]
-a <<archive> <files...>>     Add files to archive
-b,--block-size <arg>         Use specified block size
-hf,--huffman                 Use Huffman compression method (default)
-l <archive>                  Display content of archive
-o,--output-dir <arg>         Specifies directory where uncompressed files will be placed.
-ra,--range-adaptive          Use Adaptive Arithmetic Range compression method (default)
-ra32,--range-adaptive32      Use Adaptive Arithmetic Range compression method 32 bit
-ra64,--range-adaptive64      Use Adaptive Arithmetic Range compression method 64 bit
-rabit,--range-bit-adaptive   Use Adaptive Bit Arithmetic Range compression method
-range,--arithmetic           Use Arithmetic Range compression method (default)
-range32,--arithmetic32       Use Arithmetic Range compression method 32 bit
-range64,--arithmetic64       Use Arithmetic Range compression method 64 bit
-sm,--use-stream-mode         Stream mode will be used for files compression. Might reduce compression ratio.
-t,--threads <arg>            Use specified number of threads.
-v,--verbose                  Print more detailed (verbose) information to screen.
-x <archive>                  Extract files with full path
```

# Command line parameters
There are three commands: ` -a, -x, -l`, all the other switches are options.
All options should be specified **before** any command.

## Commands
`-a <archive> <inputfiles...>` - add files <inputfiles> to archive <archive>. 
Input files can contain paths, otherwise they are got from current directory.
Paths are not stored into archive at the moment, if two files have the same names they will be added in archive as two separate entities, but will overwrite each other during uncompression.

`-x <archive>` - uncompresses and extracts files from archive into current directory. 
Command line parameter `-o <output dir>` can be used to specify another directory for extracted files.  

`-l <archive>` - lists contents of archive. 
If option `-v` ise used together with command `-l` then list of blocks is shown on the screen in addition to list of files.  


## Switches

### Compression methods
Compression method specified in command line is applied to all files being compressed.

`-hf` and `-hfa` - Huffman compression method (either original or adaptive) will be used for compression of specified files. 
With `-hf` each file will be passed two times. First time is for collecting frequency statistics and second time for actual compression using this statistics. 
Statistics table will be stored together with compressed data.
With `-hfa` option Adaptive Huffman compression method will be used that does not require storing separate table with frequencies.
Both methods have similar compresssion ratio.

`-range`, `-range32`, `-range64` - use Range (arithmetic) encoding for compression. Algorithms are the same, they differ by bitness of calculations.
Due to this they produce different compressed output but have similar compression ratio.
`-range` is now a synonym for `-range32` (may change in future).

`-ra`, `-ra32`, `-r64` - the same as above but adaptive model of order1 is used instead of fixed order0 model. 
Adaptive order1 model provides slightly better compression ratio.

`-rabit` - implementation of Range method that processes input stream bit-by-bit. This implementation is simpler than `-ra` and `-range` but it is slower.
Also it provides slightly worse compression results.

### Other switches
`-b` - specifies block size to use. Blocks are required for BWT and MTF transformations which provide better compression. 
Each input file is divided into blocks of specified size and each block will be compressed separately.
Block size can be specified in bytes, kilobytes (K) or megabytes (M).
Examples: `-b2048`, `-b100000`, `-b10K`, `-b128K`, `-b2M`
Block size cannot be less than 1000 bytes and greater than 300 000 000 bytes.
This option is valid for compression only. Ignored during uncompresion.

`-t` - specifies number of threads to be used for doing compression or uncompression. 
Significantly speeds up the process on computers with several CPU units.
Multiple threads will be used only if value specified by `-t` option is greater than 1.
Value cannot be greater than 24.
Examples: `-t0`, `-t1`, `-t4`,`-t10`.

`-sm` - specifies to DO NOT use blocks during compression. Files will be compressed as streams of bytes without using BWT and MTF transformations. 
Compression will be faster, but compression rate much worse. 
This option is intended for experimental purposes for example for adding new compresson algorithms which do not support block mode by default. 
Option is ignored .....

`-o` - specifies path to directory where to extract files during uncompressing. 

`-v` - turns on verbose mode. Additional information will be printed to console during compression or uncompression.


Улучшения:
- не хранить bytes length [#b804d614](https://github.com/scrat98/data-compressor/commit/b804d614)
- использовать примитивные типы вместо wrapped типов. дает прирост в ~3 раза [#c7a57abc](https://github.com/scrat98/data-compressor/commit/c7a57abc)
- был написан [suffix array за O(n * log n)](https://github.com/scrat98/data-compressor/commit/f10a8cb9), но на практике дает мало преимуществ, 
так как до этого RLE сожмет повторяющиеся символы и сравнение строк будет почти за O(1) в реализации на qsort(которая в теории работает за n^2 * log n), 
так как быстро встретится первый неповторяющийся символ

Источники:
- https://www.youtube.com/watch?v=4n7NPk5lwbI
- https://www.quora.com/Algorithms/How-can-I-optimize-burrows-wheeler-transform-and-inverse-transform-to-work-in-O-n-time-O-n-space
- https://neerc.ifmo.ru/wiki/index.php?title=%D0%9F%D1%80%D0%B5%D0%BE%D0%B1%D1%80%D0%B0%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5_%D0%91%D0%B0%D1%80%D1%80%D0%BE%D1%83%D0%B7%D0%B0-%D0%A3%D0%B8%D0%BB%D0%B5%D1%80%D0%B0
- https://compression.ru/book/pdf/compression_methods_part1_5-7.pdf
- http://mf.grsu.by/UchProc/livak/po/comprsite/theory_bwt.html


Источники:
- https://habr.com/ru/post/141827/
- https://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D0%B4%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5_%D0%B4%D0%BB%D0%B8%D0%BD_%D1%81%D0%B5%D1%80%D0%B8%D0%B9

## TODO
- Заменить алгоритм RLE на LZW/LZ77, что даст прирост в качестве сжатия.
- Можно в алгоритме BWT схлопывать 4ки букв в 32bit число. или 8ки букв в 64bit число. тем самым мы ускорим сортировку. Но это только в теории, на практике непонятно.
https://www.hindawi.com/journals/js/2018/6908760/

# Performance test results
For tests we are going to use [Calgary group dataset](http://www.data-compression.info/Corpora/CalgaryCorpus/)

## Entropy for files
| File name   | H(X)        | H(X \| X)   | H(X \| XX)  | 
| ----------- | ----------- | ----------- | ----------- |
| bib         | 5.2007      | 3.3641      | 2.3075      |
| book1       | 4.5271      | 3.5845      | 2.8141      |
| book2       | 4.7926      | 3.7452      | 2.7357      |
| geo         | 5.6464      | 4.2642      | 3.4578      |
| news        | 5.1896      | 4.0919      | 2.9228      |
| obj1        | 5.9482      | 3.4636      | 1.4005      |
| obj2        | 6.2604      | 3.8704      | 2.2654      |
| paper1      | 4.9830      | 3.6461      | 2.3318      |
| paper2      | 4.6014      | 3.5224      | 2.5136      |
| pic         | 1.2102      | 0.8237      | 0.7052      |
| progc       | 5.1990      | 3.6034      | 2.1340      |
| progl       | 4.7701      | 3.2116      | 2.0435      |
| progp       | 4.8688      | 3.1875      | 1.7551      |
| trans       | 5.5328      | 3.3548      | 1.9305      |


## Overall result
| Type | Total compressed(bytes) | Total bits per byte | Total elapsed time(ms) | (raw - compressed)/elapsed time ratio |
| ----------- | ----------- | ----------- | ----------- | ----------- |
| A0 | 1713127 | 68.593 | 947 | 1508 |
| BWT <-> MTF <-> A0 | 972691 | 37.525 | 4435 | 489 |
| RLE <-> BWT <-> MTF <-> A0 | 971855 | 37.583 | 2658 | 816 |
| BWT <-> MTF <-> RLE <-> A0 | 936784 | 35.946 | 3399 | 648 |
| RLE <-> BWT <-> MTF <-> RLE <-> A0 | 937162 | 35.998 | 2919 | 755 |