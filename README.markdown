## Clipping: Simple, Transparent Data Storage

Clipping is a simple, flexible framework for declaring containers whose values
should make a best effort to be persistent between runs of your
software.

Sometimes, you want a giant object serialization and persistence
framework with ACID properties, the ability to stream data, and
versioning. If you want those things, Clipping is not for you.

Clipping does not enforce any explicit semantics on the strategy for
storage. Currently, its implementations use standard Java
serialization strategies. Clipping's architecture is such that it is trivial
to switch between various storage mediums, and to provide any sort of
(simple) persistence semantics you might deem fit.

BankSimple uses Clipping to manage monotonically increasing date-based
cursors on streaming data which may contain repetition.

### Using A Clipping

First, declare a clipping instance. In this case, we're using a reentrant
ConcurrentDiskVar:

    val decisionStore = new ConcurrentDiskVar[Boolean](false, "ex1", "/store")

You may then use Clipping's read or apply method to read the
value. The `decisionStore` will attempt to read the value from
storage; if it fails, it will provide the default. In this case, the
following code:

    if( decisionStore() ) { println("yes") } else { println("no") }

attempts to read the file "/store/ex1.clipping" and obtain a
boolean value. If that operation succeeds, its value will be
returned. If that operation fails, the default value will be used.

Once the clipping instance has consulted its backing store, it will
not consult it on reads during its lifetime. To write a new value to
a clipping, use either the `write` or `<<` methods, which are
identical in functionality:

    decisionStore << true
    if( decisionStore() ) { println("yes") } else { println("no") }

This code will always print "yes" because the value cached in the
`decisionStore` will immediately update, assuming there are no other
threads with references to `decisionStore` altering its value.

Finally, sometimes it is useful to predicate a value update operation
on a logical expression. Clipping supports a `writeIf` method that allows one to
predicate a write in such a fashion:

    val dateStore = new SynchronizedDiskVar[DateTime](DateTime.now, "ex2")
    val timeToWrite = DateTime.now
    dateStore.writeIf((d) => d.isBefore(timeToWrite)) { timeToWrite }

In this case, the second argument to `writeIf` is only evaluated and
stored only if `timeToWrite` occurs after the currently stored
value.

### Current Implementations

Clipping's implementation separates the actual encoding and storage (a
PersistingStrategy) of values from the strategy used to order and
execute the writes and reads (a ManagementStrategy). Currently the
following complete implementations exist:

1. *SynchronizedDiskVar*: This implementation writes values to disk
   using standard Java serialization. When writes occur, they occur
   within the calling thread. This strategy is reentrant and has a
   slightly lower overhead than *ConcurrentDiskVar*, but if many threads are
   writing to the variable it will cause unnecessary lock contention.
2. *ConcurrentDiskVar*: Like *SynchronizedDiskVar*, it writes values to disk using
   Java serialization. Unlike SynchronizedDiskVar, it uses a
   ReadWriteLock and tasks a separate thread with writes. This is
   probably the preferred implementation for most cases.

Both strategies use OnDiskPersistingStrategy, which in turn uses the standard
write-then-move technique for writes.

### Future Work

1. We'd like more storage strategies. Redis seems like a likely candidate.
2. We'd like more serialization methods.
3. We'd also like a pony.
