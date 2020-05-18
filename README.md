# Heap Language
A minimal implementation of a Truffle-based language for analysing heap dumps.

To tun tests, execute:

```bash
./gradlew check
```

Producing coverage reports is possible using `./gradlew jacocoTestReport` (Only on Hotspot JDKs though).

## Benchmarks

### Benchmark heap dump

To generate a test heap, execute:

```bash
./gradlew benchmarks:dump-generator:run
```

This will generate a predictable medium-sized heap dump in `./benchmarks/dumps/tumor_cell.hprof`.

### Custom benchmarks

To run a benchmark script file, execute:

```bash
./gradlew benchmarks:runner:bench -Dscript=path/to/oql.js -Dheap=path/to/heap/dump.hprof
```

Note that `heap` defaults to the benchmark heap dump specified above and that for now, the paths are relative to the
`benchmark/runner` directory, not the root of the project. 

You can choose which implementation of OQL should be used by specifying `-Dengine=original` or `-Dengine=truffle`.
Default is `truffle`.

If you are running on GraalVM, you can dump compilation graphs to IGV automatically by adding `-Digv=on`.

The project should automatically use Graal compiler for JavaScript and Heap Language on Hotspot JDKs
with version 11 and above. If you don't want this (run JS using Nashorn), specify `-Dgraal=off`.

### Prepared benchmarks

There are some prepared benchmarks in `benchmarks/scripts/`. These typically have a custom gradle task `benchXY`
associated with them where you don't have to specify the heap or the script: 

```bash
./gradlew benchmarks:runner:bench01
```

Some of these benchmarks are also re-implmeneted using fully native Java code, in which case `-Dengine=native` is
also available.