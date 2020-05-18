/*
    Super basic benchmark to just count the objects on the heap.
*/
var count = 0;
/*heap.forEachObject(function(it) {
    count += 1;
});*/
heap.forEachObject('count += 1');
print("Total objects: "+count);