## Burstsort ##

Burstsort4j contains an implementation of both the original
[Burstsort](http://en.wikipedia.org/wiki/Burstsort), which is quite fast
but not particularly memory efficient, and the engineered Burstsort, which
is extremely memory efficient and close to the speed of the original in
most cases. Burstsort typically outperforms other string sorting algorithms
for most data sets.

Both single-threaded and multi-threaded implementations are available,
where the multi-threaded method will utilize all available processor cores
using a simple thread pool executor. In this mode of operation, the buckets
to be sorted are assigned to jobs and sorted in parallel.

## Funnelsort ##

In addition to Burstsort there is an implementation of the Funnelsort
algorithm for sorting strings, which is faster than most string sorts but
not as fast as Burstsort. Funnelsort uses string comparison while Burstsort
is a form of radix sort and hence sorts only lexicographically by
individual characters. The specific implementation of Funnelsort is modeled
after the [Lazy Funnelsort](http://portal.acm.org/citation.cfm?id=1227161.1227164)
described by Brodal, Fagerberg, and Vinther. At the heart of the algorithm
is an [insertion d-way merger](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.63.3896)
as described by Brodal and Moruz. Like Burstsort, the Funnelsort algorithm
is cache- oblivious and thus typically performs well compared to algorithms
that assume a unit-cost for RAM access (e.g. Quicksort).

## Multikey Quicksort ##

Burstsort4j contains a Java implementation of the multikey quicksort
algorithm. It takes a median of three approach to find the pivot, and
delegates to insertion sort for small sublists. This sort is what Burstsort
delegates to for sorting the buckets that hang from the trie structure.
