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

An automated PostgreSQL test with 30 synthetic children verifies that each
parent or teacher list projection runs one SQL statement and loads no child
entities. A frontend HTTP-mock test verifies that 30 photographed students cause
30 photo requests, with no more than four active at once. These tests count
requests, not end-to-end latency or SQL used by photo authorization.

The browser still requests each photo separately. Production-scale network and
SQL measurements for photo requests would be needed before choosing caching or
batching, while preserving authorization and photo invalidation.
