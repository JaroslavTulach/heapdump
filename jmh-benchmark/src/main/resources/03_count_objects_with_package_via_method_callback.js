/*
    Here, compared to benchmark 02, we replace explicit counting with a utility method provided by OQL.
    This method still uses an iterator view over the objects of the heap, but the code is bit more complex
    so it can be interesting to see if the performance is as good.
*/
count(heap.objects(), function(it) { return it.clazz.name.startsWith('java.lang'); })