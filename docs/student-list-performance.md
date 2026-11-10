# Student list performance

An isolated PostgreSQL 16 test used 30,000 synthetic active children across
100 schools, with 300 children in the selected school. The roster query selected
the visible fields and a photo-presence flag. A single warm-cache run produced:

| Plan | Shared buffers | Execution time |
| --- | ---: | ---: |
| Sequential scan without school index | 811 hits | 2.810 ms |
| Bitmap scan with partial school/grade index | 303 hits, 2 reads | 0.341 ms |

Migration V47 adds this partial index for active, visible children. These figures
are illustrative, not a production benchmark. Data distribution, storage, cache,
and write overhead can change the result; use `EXPLAIN (ANALYZE, BUFFERS)` on
representative data before drawing production conclusions.

The browser still requests each photo separately (up to four at once). Parent
lists may still read photo bytes while constructing their response. Network and
SQL measurements for those paths remain necessary before choosing caching,
batching, or an additional projection.
