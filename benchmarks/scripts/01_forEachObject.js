var count = 0;
heap.forEachObject(function (it) {
    if (it.clazz.name.startsWith('benchmark.problem')) {
        count += 1;
    }
});
print("Counted instances: "+count);