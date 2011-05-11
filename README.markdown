## Clipping: Simple, Transparent Data Storage

Sometimes, you want a giant object serialization and persistence
framework with ACID properties, the ability to stream data, and
versioning. For those people, Clipping is not the right tool. Clipping
is a simple, flexible framework for declaring containers whose values
should make a best effort to be persistent between runs of your
software.

Clipping does not enforce any explicit semantics on the strategy for
storage, and currently its only implementations use the standard Java
serialization strategies. Its architecture is such that it is trivial 
to switch between various storage mediums, and to provide any sort of
(simple) persistence semantics you might deem fit. 

BankSimple uses Clipping to manage monotonically increasing date-based
cursors on streaming data which may contain repetition.

### Using A Clipping

While the expected behavior of a clipping for the currently released
behaviors is best explained via the unit tests, their behavior is not
particularly difficult. 

Firstly, declare a clipping instance (in this case the reentrant
ConcurrentDiskVar):

    val decisionStore = new ConcurrentDiskVar[Boolean](false, "ex1", "/store")

You may then try to use clipping's read or apply method to read the
value. The `decisionStore` will attempt to read the value from
storage, and if it fails provide the default. In this case, the
following code:

    if( decisionStore() ) { println("yes") } else { println("no") }

attempts to read the file "/store/ex1.clipping" and obtain a
boolean value. If that operation succeeds, its value will be
returned. If that operation fails, the default value will be used.

Once the clipping instance has consulted its backing store, it will
not consult it on reads during its lifetime. To write a new value to 
a clipping, use the `write` or `<<` method :

    decisionStore << true
    if( decisionStore() ) { println("yes") } else { println("no") }

This code will always print "yes", because the value cached in the
`decisionStore` will immediately update (Assuming there are no other
threads with references to `decisionStore` altering its value)

Finally, sometimes it is useful to predicate a value update to a piece
of logic. Clippings support a `writeIf` method that allows one to
predicate a write in such a fashion:

    val dateStore = new SynchronizedDiskVar[DateTime](DateTime.now, "ex2")
    val timeToWrite = DateTime.now
    dateStore.writeIf((d) => d.isBefore(timeToWrite)) { timeToWrite }

In this case, the second argument to `writeIf` is only evaluated and
stored only if `timeToWrite` is strictly after the currently stored
value. 

### Current Implementations

Clipping's implementation separates the actual encoding and storage (a 
PersistingStrategy) of values from the strategy used to order and 
execute the writes and reads (a ManagementStrategy). Currently the
following complete implementations exist:

1. *SynchronizedDiskVar*: This implementation writes values to disk
   using standard Java serialization. When writes occur, they occur
   within the calling thread. This strategy is reentrant and has a
   slightly lower overhead than its peer, but if many threads are
   writing to the variable it will cause unnecessary lock contention.
2. *ConcurrentDiskVar*: Like its cousin, it writes values to disk using
   Java serialization. Unlike SynchronizedDiskVar, it uses a
   ReadWriteLock and tasks a separate thread with writes. This is
   probably the preferred implementation for most cases.

Both strategies use OnDiskPersistingStrategy, which uses the standard
write-then-move technique for writes. 

### Future Work

1. We'd like more storage strategies. Redis seems like a likely candidate.
2. We'd like more serialization methods. 
3. We'd also like a pony.

