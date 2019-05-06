# Using [Apache NetBeans](http://netbeans.apache.org) OQL to analyze heap dump

As part of my Charles University course [Practical Dynamic Compilation](https://d3s.mff.cuni.cz/teaching/practical_dynamic_compilation)
I want to demonstate how to access raw data structures effectively. Let's analyze a heap dump! 

## Using [VisualVM](https://visualvm.github.io)

The easiest way to look inside a heap dump is to use [VisualVM](https://visualvm.github.io). Take your `.hprof` file, 
open it and browse its content. In some sence a heap dump is an object database connecting classes, objects & co. in the 
dump with each other. As such we could use a query language to inspect the heap!

[VisualVM](https://visualvm.github.io) comes with one such language called **OQL**. Switch to *OQL Console* and execute following query:
```js
var arr = [];
heap.forEachObject(function(o) {
  if (o.length > 255) {
    arr.push(o);
  }
}, 'int[]')
arr
```
it gives you all integer arrays longer that 255 elements. **OQL** syntax is a mixture of *JavaScript* and *SQL* - however the 
above script is pure **JavaScript**. It iterates the `heap` using builtin `forEachObject` function and collects the large arrays
in a callback. Complex heap analysis has just got easy!

## Automatic Heap Processing

However we can go even further. [VisualVM](https://visualvm.github.io)'s **OQL** implementation comes from 
[Apache NetBeans](http://netbeans.apache.org) - why not use the `org-netbeans-modules-profiler-oql` library
in a headless application and query (possibly in a batch) the `.hprof` files from a command line!?

```xml
<dependencies>
  <dependency>
    <groupId>org.netbeans.modules</groupId>
    <artifactId>org-netbeans-modules-profiler-oql</artifactId>
    <version>RELEASE110</version>
  </dependency>
</dependencies>
```
Only one dependency needed in your [pom.xml](https://github.com/JaroslavTulach/heapdump/blob/bf90ab4cf7315c779f379c87528203605d5c3ec8/pom.xml)
and you can use **OQL** from your
[Main.java](https://github.com/JaroslavTulach/heapdump/blob/bf90ab4cf7315c779f379c87528203605d5c3ec8/src/main/java/org/apidesign/demo/heapdump/Main.java):
```java
Heap heap = HeapFactory.createHeap(file);
final OQLEngine eng = new OQLEngine(heap);
eng.executeQuery("var arr = [];\n" +
  "heap.forEachObject(function(o) {\n" +
  "  if (o.length > 255) {\n" +
  "    arr.push(o);\n" +
  "  }\n" +
  "}, 'int[]')\n" +
  "print('Found ' + arr.length + ' long int arrays');"
, OQLEngine.ObjectVisitor.DEFAULT);
```
Try it yourself:
```bash
$ git clone https://github.com/jaroslavtulach/heapdump
$ mvn -q -f heapdump/ package exec:exec -Dheap=/path/to/your/dump.hprof
Loading dump.hprof
Querying the heap
Found 7797 long int arrays
Round #1 took 6035 ms
Found 7797 long int arrays
Round #2 took 4309 ms
Found 7797 long int arrays
Round #3 took 3900 ms
Found 7797 long int arrays
....
Round #20 took 3444 ms
```
Heap dump processing automated with a few lines of code!

# Getting Faster with [GraalVM](http://graalvm.org)

The default 
[Main.java](https://github.com/JaroslavTulach/heapdump/blob/bf90ab4cf7315c779f379c87528203605d5c3ec8/src/main/java/org/apidesign/demo/heapdump/Main.java)
file works as a benchmark. It scans the heap multiple times and reports time of each round. The speed depends
on the used *JavaScript engine*. Nashorn, the default **JDK8** and **JDK11** was able to process my 661MB heap in 3.5 seconds.
Can we do better?

Sure we can! Download [GraalVM](http://graalvm.org) which provides its own [Graal.js](https://github.com/graalvm/graaljs)
script engine and run the benchmark again:
```bash
$ /graalvm-ee-1.0.0-rc16/bin/java -version
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) GraalVM EE 1.0.0-rc16 (build 25.202-b08-jvmci-0.59, mixed mode)
$ JAVA_HOME=/graalvm-ee-1.0.0-rc16 mvn -q -f heapdump/ package exec:exec -Dheap=dump.hprof
Loading dump.hprof
Querying the heap
Found 7797 long int arrays
Round #1 took 4008 ms
Found 7797 long int arrays
Round #2 took 1631 ms
Found 7797 long int arrays
Round #5 took 640 ms
Found 7797 long int arrays
Round #9 took 300 ms
Found 7797 long int arrays
Round #20 took 230 ms
```
Fiveteen times faster! Good result for a simple replace of one JDK by another, right?
[Apache NetBeans](http://netbeans.apache.org) gives you useful libraries.
[GraalVM](http://graalvm.org) makes them run fast!

# To Be Continued...

That is the plot. Now the we can focus on the
main question of [my course](https://d3s.mff.cuni.cz/teaching/practical_dynamic_compilation):
Can we make it even faster?
