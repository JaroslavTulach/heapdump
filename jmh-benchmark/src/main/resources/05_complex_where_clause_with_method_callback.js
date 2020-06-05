select map(referrers(s.values), 'objectid(it)') from benchmark.problem.BooleanNetwork$State s where !contains(s.values, function(it) { return it == 'false'; })
/*
    The point of this benchmark is to test whether repeatedly executing a function with the came callback is
    performed efficiently (here, contains is called >1M times, every time with the same callback function).

    The map/referrers/object_id is executed only once so is probably not that relevant for performance.
*/