burstsort4j
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
$Id$


Using burstsort4j
----------------------------------------------------------------------
The Burstsort sorting algorithm is designed to work efficiently with
large sets of strings. It is not intended for anything less than
100,000 strings. There are some data sets for which Burstsort will
perform quite badly (see the Benchmark class), but for the most part
will will perform many times faster than just about any other sort.

Burstsort is quite memory intensive. It constructs a shallow trie
structure with buckets of string references dangling from the leaf
nodes. While the overhead is not huge, the fact that you are sorting
millions of strings means the system will need a lot of RAM, at least
1GB, preferably more.


Running the Benchmarks
----------------------------------------------------------------------
The machine should have at least 1GB of RAM for this to work well,
ideally 2GB or more.

1. Build the jar: ant dist

2. java -Xmx1024m -cp dist/burstsort4j.jar org.burstsort4j.Benchmark

To benchmark using a particular data file, add two more options. The
first option specifies the input size:

  --1 : 100,000 lines
  --2 : 1,000,000 lines
  --3 : 3,000,000 lines

Specifying --3 will also test the smaller sizes as well, and using
--2 will test the 100k and 1m sizes.

The second option is the file name (and optional path) for the test
data, which should contain a sufficient number of lines for the
given data size (i.e. 3m lines if using --3).
