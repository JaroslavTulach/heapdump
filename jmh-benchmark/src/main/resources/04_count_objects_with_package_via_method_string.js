/*
    Technically, this should be as fast as benchmark 03, but here we use string expression instead of
    function callback, which might cause problems if the expression cannot be optimized as well as the callback.
*/
count(heap.objects(), "it.clazz.name.startsWith('java.lang')")