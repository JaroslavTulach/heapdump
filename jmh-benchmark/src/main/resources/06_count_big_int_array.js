/* global heap */
var arr = [];
var count = 0;
heap.forEachObject(function(o) {
  count++;
  if (o.length > 255) {
    arr.push(o);
  }
}, 'char[]');

if (count != 305) {
    throw `Unexpected number of arrays: ${arr.length}`;
}

if (arr.length != 3) {
    throw `Unexpected number of large arrays: ${arr.length}`;
}

arr;
