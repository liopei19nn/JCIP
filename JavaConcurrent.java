/*
Ch2 Fundamental => atomicity, lock, liveness and performance.

Thread safety <= Correctness

ways to avoid thread safety issue : 1. No shared state variable 2. Immutable 3. Synchronization

Stateless objects are always safe.


Race condition => correctness is dependent on timing

Atomic Operations => all or nothing
*/

/*To preserve state consistency, update related state variables in a single atomic operation.
eg: even lastNumber and lastFactors are all atomic, but we want the two operation binded together.
In this case, there is a window vulnerable for other thread to change the atomic thing and break the
bind of two realted atomic reference.*/
@NotThreadSafe
public class UnsafeCachingFactorizer implements Servlet {
     private final AtomicReference<BigInteger> lastNumber
         = new AtomicReference<BigInteger>();
     private final AtomicReference<BigInteger[]>  lastFactors
         = new AtomicReference<BigInteger[]>();
     public void service(ServletRequest req, ServletResponse resp) {
         BigInteger i = extractFromRequest(req);
         if (i.equals(lastNumber.get()))
             encodeIntoResponse(resp,  lastFactors.get() );
         else {
			BigInteger[] factors = factor(i);
			lastNumber.set(i);
			lastFactors.set(factors);
			encodeIntoResponse(resp, factors);
     } 
   }
}


/*Reentrancy means locks are acquired on a per-thread level, not per-invocation level. 
这一特性可以在java中由一个counter实现。lock存在一个counter，当其为0的时候可以被某个线程获得。然后
增加计数，之后同一线程内的访问都会增加计数，完成一个计数－1， 直到0为止*/

// there will be no deadlock bacause re-entrancy
public class Widget {
    public synchronized void doSomething() {
     ...
    }
}
public class LoggingWidget extends Widget {
    public synchronized void doSomething() {
        System.out.println(toString() + ": calling doSomething");
        super.doSomething();
    }
}


// 2.4 对于mutable variable, 如果有多个线程可以操作，那么不只是write要lock，read也要lock. 
//     for mutable variable accessed by multiple threads, lock should be implemented for both read and write,
//     not only for write.

//     Every shrad, mutable variable should be guraded by exactly one lock, which is clear for the maintainer.

//     For every invariant that involves more than one variable, all the variables involved in that invariant must
//     be guarded by the same lock.


// 2.5 Performance is also big issue. Focus only on thread safe will harm performance. 

//      Simplicity and performance can be at odds. Simplicity can be got when you synchronize the whole block,
//      the concurrency can be got when the shortest code path are in synchronized block. When implementing 
//      sync policy, consider performance first instead of simplicity.

//      Holding a lock during high load computation may lead to performance issue, suck as network and I/O

// an example considering performance

@ThreadSafe
public class CachedFactorizer implements Servlet {
    @GuardedBy("this") private BigInteger lastNumber;  // temporary cache for last queried number
    @GuardedBy("this") private BigInteger[] lastFactors; // cache for factors of last querid number
    @GuardedBy("this") private long hits;
    @GuardedBy("this") private long cacheHits;
    public synchronized long getHits() { return hits; }
    public synchronized double getCacheHitRatio() {
        return (double) cacheHits / (double) hits;
    }
    public void service(ServletRequest req, ServletResponse resp) {
        BigInteger i = extractFromRequest(req);
        BigInteger[] factors = null;
        synchronized (this) {
            ++hits;
            if (i.equals(lastNumber)) {
                ++cacheHits;
                factors = lastFactors.clone();
            }
        }
        if (factors == null) {
            factors = factor(i);
            synchronized (this)  {
                lastNumber = i;
                lastFactors = factors.clone();
            }
        }
        encodeIntoResponse(resp, factors);
    }
}

// CH3 Sharing Obj

// 3.1 Visibility : how to make other thread see the change?

// a simple wait and print number function with visibility issue
/*
  it could be possible that no the ready in thread never reach true
  or print a number either 0 or 42

  Printing 0 is a behavior called Re-ordering, which is supported by JVM
  to take advantage of hardware.

*/
public class NoVisibility {
    private static boolean ready;
    private static int number;
    private static class ReaderThread extends Thread {
        public void run() {
            while (!ready)
                Thread.yield();
            System.out.println(number);
        }
    }
    public static void main(String[] args) {
        new ReaderThread().start();
        number = 42;
        ready = true;
    } 
}


/*3.1.1 stale data : another thread may see the stale data, or worse, half-stale data (some data are up-to-date, while others not)

3.1.2 Non-atomic 64-bid operations : 
      out-of-thin-air-safety : When a thread reads a variable without sync, it may see a stale value, but at least the value is placed by one thread, not a random one.
      Exception is 64-bid numeric value (long and double). They are built with 2 seperate 32-bid operations, so a non-volatile long or double's two seperate 32-bid value may be from different threads.


3.1.3 Locking for visibility : For thread A and B have common sync locking, everything A did in or prior to a sync block is visible to B when it executes a sync block guarded by same lock.

3.1.4 using volatile : volatile variable will not be saved in cache or register, but directly to memory. It is a light weight style sync method. It promise visibility, but not atomicity, which means for read-update-write method, it could perform incorrectly. 

Rules for using volatile : Use volatile variables only when they simplify implementing and verifying your synchronization policy; avoid using volatile variables when verifying correctness would require subtle reasoning about visibility. Good uses of volatile variables include ensuring the visibility of their own state, that of the object they refer to, or indicating that an important lifecycle event (such as initialization or shutdown) has occurred.

When can we use volatile ? => must met all three criteria :
  1. Writes to the variable do not depend on its current value, or you can ensure that only a single thread ever updates the value;
  2. The variable does not participate in invariants with other state variables; and
  3. Locking is not required for any other reason while the variable is being accessed. */



// 3.2 Publication and Escape : An object that is published when it should not have been is said to have escaped.
  
  // escape objests
  class UnsafeStates {
    private String[] states = new String[] {" "};
    public String[] getStates() {return states; }

  }

  // a very tricky way to escape => implicitly allowing the this reference to escape. http://jcip.net/listings/ThisEscape.java
  // and a way to interprete this http://www.javaspecialists.eu/archive/Issue192.html


// 3.2.1 Safe Construction Practice

// 不要在constructor里start线程，而要等待constructor返回之后再启动线程。因为即使start是constructor里面最后的一行代码，也不能保证它等待到前面所有代码都执行完之后才开始，也就是说新的线程看到的可能是不完整的信息。
// 一个正确的实现方法 ： 利用factory method返回线程，然后再call start

//list 3.8  

public class SafeListener {
  private final EventListner listener;
  
  private SafeListener(){
    listener = new EventListner(){
       public void onEvent(Event e){
        doSomething(e);
       }
    };
  }
  // factory method return a 
  public static SafeListener newInstance(EventSource source) {
    SafeListener safe = new SafeListener();
    source.register(safe.listener);
    return safe;
  }
}

/* 3.3 Thread Confinement 

assumption : if only one thread can access the data, the data will be thread safe */

/* 3.3.1 ad-hoc thread confinement : the whole maintainence of thread confinement will be fall on the implementation
       eg : implement only single thread can write to a 'volatile' variable */

/* 3.3.2 stack confinement : 用一个局部变量保存shared variable的信息。因为每个线程都有一个独立的局部的stack,所以保证了线程安全 */
// List 3.9
public int loadTheArk(Collection<Animal> candidates) {
    SortedSet<Animal> animals;
    int numPairs = 0;
    Animal candidate = null;
    
    // animals confined to method because it is a local variable
    animals = new TreeSet<Animal>(new SpeciesGenderComparator());

    // ??? not sure the add all method is thread safe?
    animals.addAll(candidates);
    for(Animal a : animals) {
      if(candidate == null || !candidate.isPotentialMates(a)) {
        candidate = a;
      } else {
        // not sure the load method is thread safe
        // actually, where is this ark from?
        ark.load(new AnimalPair(candidate, a));
        ++numPairs;
        candidate = null;
      }
    }
    return numPairs;
}



/* 3.3.3 ThreadLocal 
  
  ThreadLocal 有get和set方法，使用get方法可以得到最近的set赋予的值
  使用场景1 ： prevent sharing in designs based on mutable Singletons or global variable
  使用场景2 ： when a frequently used operation requires a temporary object
  使用场景3 ： porting single thread application to a multithreaded environment.

  可以假想ThreadLocal的工作方式是一个Map<Thread, T>, 但是实际情况是线程结束后会被garbage collection
  weak reference : https://en.wikipedia.org/wiki/Weak_reference

   
*/

// List 3.10 ThreadLocal for every thread hold the connection to database, ensure thread confinement
private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>() {
  public Connection initialValue() {
    return DriverManager.getConnection(DB_URL);
  }
}
public static Connection getConnection() {
  // get will return the initialValue if no set is called before
  return connectionHolder.get();
}


/* 3.4 Immutability
  
  Immutable objects are always thread safe

  An object is immutable if 
  1. its state cannot be modified after construction;
  2. all its fields are final; 
  3. It is properly constructed (the this reference is not escaped during construction)

*/


/* 3.4.1 Final field

  It is a good practice to make all fields final unless it needs to be mutable.

  to be continued with safe publication idiom

*/ 

// Listing 3.12 Immutable Holder for Cacheing a number and its factors 
  // the key is local final copy

@immutable
class OneValueCache {
  private final BigInteger lastNumber;
  private final BigInteger[] lastFactors;

  public OneValueCache(BigInteger i, BigInteger[] factors) {
    lastNumber = i;
    lastFactors = Array.copyOf(factors, factors.length);
  }

  public BigInteger[] getFactors(BigInteger i){
    if(lastNumber == null || !lastNumber.equals(i))
      return null;
    else 
      return Arrays.copyOf(lastFactors, lastFactors.length);
  }
}

// Listing 3.13 Use the immutable cache in a BigInteger Servlet

@ThreadSafe
public class VolatileCachedFactorizer implements Servlet {
  private volatile OneValueCache cache = new OneValueCache(null, null);

  public void service(ServletRequest req, ServletResponse resp) {
    BigInteger i = extractFromRequest(req);
    BigInteger[] factors = cache.getFactors();

    if(factors == null){
      factors = factor(i);
      cache = new OneValueCache(i, factors);
    }
    encodeIntoResponse(resp, factors);
  }
}

// 3.5 Safe Publication

// 3.5.2 Immutable Objects and Initialization Safety
// immutable objects can be used safely by any thread without additional sync, even when sync is not used to 
// publish them

/* !!! 3.5.3 Safe Publication Idioms 
    1. Initializing an obj reference from a static initializer
    # storing a reference to it into a volatile field or AtomicReference.
    # Storing a reference to it into a field that is properly guarded by a lock; or
    # Storing a reference to it into a field that is properly guarded by a lock.

    Thread-safe collection library guarantee the visibility
    Placing a K-V pair in a HashTable, SynchronizedMap and ConcurrentMap safely publish it to any thread that retrives it from the Map (from directly or iterator)
    Placing an element in Vector, CopyOnWriteArrayList, CopyOnWriteArrayaet, SynchronizedList or SynchronizedSet safely publish it to any thread that retrive it from the collection.
    Placing an element on a BlockingQueue or ConcurrentLinkedQueue safely publishes it to any thread that retrives it from the queue.

    Static initializers are executed by the JVM at class initialization time; 
    because of internal synchronization in the JVM, this mechanism is guaranteed to safely 
    publish any objects initialized in this way [JLS 12.4.2].


*/

/* 3.5.4 Effectively Immutable Objects 
  
  Objects that are not technically immutable, but whose state will not be modified after publication, are called effectively immutable.

  Safely published effectively immutable objects can be used safely by any thread without additional synchronization.

*/

/* 3.5.5 Mutable Objects

  The publication requirements for an object depend on its mutability : 

  Immutable objects can be published through any mechanism.
  Effectively immutable objects must be safely published.
  Mutable Objects must be safely published, and must be either thread-safe or guarded by a lock.
*/


/*
  The most useful policies for using and sharing objects in a concurrent program are:
      Thread‐confined. A thread‐confined object is owned exclusively by and confined to one thread, and can be modified by its owning thread.
      Shared read‐only. A shared read‐only object can be accessed concurrently by multiple threads without additional synchronization, but cannot be modified by any thread. Shared read‐only objects include immutable and effectively immutable objects.
      Shared thread‐safe. A thread‐safe object performs synchronization internally, so multiple threads can freely access it through its public interface without further synchronization.
      Guarded. A guarded object can be accessed only with a specific lock held. Guarded objects include those that are encapsulated within other thread‐safe objects and published objects that are known to be guarded by a specific lock.
*/


/* 4.1 Design Thread-safe Class 

   The design process for a thread‐safe class should include these three basic elements:
    • Identify the variables that form the object's state;
    • Identify the invariants that constrain the state variables;
    • Establish a policy for managing concurrent access to the object's state.
*/

// The Java Monitor Pattern : Lock with private lock 
    // private lock is easy, and cannot be obtained by outside access
public class PrivateLock {
  private final Ojbect myLock = new Ojbect();
  @GuardedBy("myLock") Widget widget;
  void someMethod() {
    synchronized(myLock) {
      // access or modify the state of widget
    }
  }
}

// delegattion : 
//  CopyOnWriteArrayList

// 4.3.4 Publishing Underlying State Variables
// If a state variable is thread-safe, does not participate in any invariants that constraints its value, and has no prohibited state transitions for any of its operations, then it can safely be published.

// 4.4.1 Client-side Locking
// How to extend existing thread safe class : 1. modify the thread safe class in source code 2. extend the class 3. use helper class

// Listing 4.14 Helper with wrong locking
/* using the wrong lock. synchronized block is on the helper, but not on the list. So it could be wrong when other thread working on other method operating the 
  list
*/
@NotThreadSafe
public class ListHelper<E> {
	// Collections.synchronizedList(new ArrayList<E>()); this will generate a sync list. Try carefully use the iterator, synchronized block for list's iterator is required
	public List<E> list = Collections.synchronizedList(new ArrayList<E>());   
	public synchronized boolean putIfAbsent(E x) {
		boolean absent = !list.contains(x);       
		if(absent) list.add(x);
		return absent;
	}
}

// Listing 4.15 Helper with right lock
@ThreadSafe
public class ListHelper<E> {
	// Collections.synchronizedList(new ArrayList<E>()); this will generate a sync list. Try carefully use the iterator, synchronized block for list's iterator is required
	public List<E> list = Collections.synchronizedList(new ArrayList<E>());   
	public boolean putIfAbsent(E x) {
			synchronized(list){
				boolean absent = !list.contains(x);       
				if(absent) list.add(x);
				return absent;
		}
	}
}

// 4.4.2 Composition of thread-safe class
// Listing 4.16 Implementing put-if-absent using composition
@ThreadSafe
public class ImprovedList<T> implements List<T> {
    private final List<T> list;
    public ImprovedList(List<T> list) { this.list = list; }
    public synchronized boolean putIfAbsent(T x) {
        boolean contains = list.contains(x);
        if (contains)
            list.add(x);
        return !contains;
}
    public synchronized void clear() { list.clear(); }
    // ... similarly delegate other List methods
}


/*
 ch5 building blocks
*/


// 5.1.3 Hidden Iterators
//
// ConcurretnModificationException: This exception may be thrown by methods that have detected concurrent modification of an object when such modification is not permissible.
// 使用locking会杜绝这个exception, 但是一定要记住所有iterator出现的位置并且加锁。

// List 5.6
// Hashset的toString方法里隐藏了一个set的iterator。在对set进行toString操作的同时，对其进行add或remove就会造成iterate过程中modify的情况。
public class HiddenIterator {
	@GuardedBy("this")
	private final Set<Integer> set = new HashSet<Integer>();

	public synchronized void add(Integer i) { set.add(i); }
	public synchronized void remove(Integer i) { set.remove(i); }

	public void addTenThings() {
		Random r = new Random();
		for(int i = 0; i < 10; i++) {
			add(r.nextInt());
		}
		System.out.println("DEBUG: added ten elements to " + set);
	}
}


// 5.2.1 Conccurent HashMap
// Locking Strip: Arbitrarily many reading threads can access the map concurrently, readers can access the map concurrently with writers, and a limited number of writers can modify the map concurrently
// Trade-Off : Concurrent Hashmap的size方法返回的是一个approxiamation, 不是准确值.      ConcurrentHashMap不能用外部的client side locking

/* 5.2.3 	CopyOnWriteArrayList: concurrent version of arraylist
	 				CopyOnWriteArraySet: concurrent version of set

	 				使用copy-on-write使其immutable的方式来实现concurrent。它的iterator会一直保持其初始化状态，避免了concurrentmodificationexception

	 				对于数据量很大的collection, copy-on-write的cost很大。这种collection适合retriving多于modification的collection，例如register event listener.
*/


/*5.3 Blocking Queue

		Put is blocked when the queue is full, get is blocked when the queue is empty.
		If the queue is not bounded, the put will never block,

		If produce is faster than consume, then a policy should be designed to handle this.
		
		LinkedBlockingQueue -> LinkedList
	  ArrayBlockingQueue -> ArrayList
	  PriorityBlockingQueue

	  SYnchronousQueue : not for storage. It maintains a list of queued threads waiting to enqueue or dequeue an element.

	  Deque: work stealing ->  In work stealing way, every consumer has their own deque to work on, and when their own deque is empty, it will steal work from others' tail. So it will not interfere with others deque.
	  Deque is good for problem that a producer is also consumer

*/


/* 5.4 Blocking and Interruptable Methods 
    
   When your code calls a method that throws InterruptedException, then your method is a blocking method too, and must have a plan for responding to interruption. For library code, there are basically two choices:

   1. Propagate the InterruptedException. This is often the most sensible policy if you can get away with it ‐ just propagate the InterruptedException to your caller. 
      This could involve not catching InterruptedException, or catching it and throwing it again after performing some brief activity‐specific cleanup.
   2. Restore the interrupt. Sometimes you cannot throw InterruptedException, for instance when your code is part of a Runnable. In these situations, you must catch InterruptedException
       and restore the interrupted status by calling interrupt on the current thread, so that code higher up the call stack can see that an interrupt was issued, as demonstrated in Listing 5.10.

   Do not do !!! : catch the InterruptedException and do nothing with it 
*/


/* 5.5 Synchronizers */

/* 5.5.1 Latch 
  

    latches: act as a gate, until the latch reaches the terminal state the gate is closed and no thread can pass, and in the terminal state the gate opens, allowing all threads to pass. Once the latch reaches the terminal state, it cannot change state again, so it remains open forever. Latches can be used to ensure that certain activities do not proceed until other one‐time activities complete, such as:
           1. Ensuring that a computation does not proceed until resources it needs have been initialized. A simple binary (two‐state) latch could be used to indicate "Resource R has been initialized", and any activity that requires R would wait first on this latch.
           2. Ensuring that a service does not start until other services on which it depends have started. Each service would have an associated binary latch; starting service S would involve first waiting on the latches for other services on which S depends, and then releasing the S latch after startup completes so any services that depend on S can then proceed.
           3. Waiting until all the parties involved in an activity, for instance the players in a multi‐player game, are ready to proceed. In this case, the latch reaches the terminal state after all the players are ready.
          
*/

//Listing 5.11  Using CountDownLatch for starting and stopping threads for timing tests
// await will block thread until the latch is 0, countDown will reduce count by 1
public class TestHarness {
    public long timeTasks(int nThreads, final Runnable task) throws InterruptedException {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(nThreads);      

        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        startGate.await();
                        try {
                            task.run();
                        } finally {
                            endGate.countDown();
                        }
                    } catch (InterruptedException ignored) { }
                }
            }
          t.start();
        }
        long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        long end = System.nanoTime();
        return end-start;
    }
}



/* 5.5.2 FutureTask 
  
  FutureTask have 3 states: waiting to run, running, completed
  If it is completed, get returns the result immediately, and otherwise blocks until the task transitions to the completed state and then returns the result or throws an exception.
  
  FutureTask is implemented by Callable, and the get method will have three kind of exception: a checked exception from Callable, a RuntimeException or an error.
  Need to handle all this in different ways

*/
// Listing 5.12 Using Future Task to preload data that is needed later
public class Preloader {
    private final FutureTask<ProductInfo> future =
        new FutureTask<ProductInfo>(new Callable<ProductInfo>() {
            public ProductInfo call() throws DataLoadException {
                return loadProductInfo();
            }
        });
    private final Thread thread = new Thread(future);
    public void start() { thread.start(); }
    public ProductInfo get() throws DataLoadException, InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DataLoadException) 
              throw (DataLoadException) cause;
            else 
              throw launderThrowable(cause);
        } 
    }
}
/** If the Throwable is an Error, throw it; if it is a
 *  RuntimeException return it, otherwise throw IllegalStateException
 */
public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException)
        return (RuntimeException) t;
    else if (t instanceof Error)
        throw (Error) t;
    else
        throw new IllegalStateException("Not unchecked", t);
}


// 5.5.3 Semaphore
// Listing 5.14 Bounded Set bounded with Semaphore
// number of element in set is bounded by semaphore
public class BoundedHashSet<T> {
    private final Set<T> set;
    private final Semaphore sem;
    public BoundedHashSet(int bound) {
        this.set = Collections.synchronizedSet(new HashSet<T>());
        sem = new Semaphore(bound);
    }
    public boolean add(T o) throws InterruptedException {
        sem.acquire();
        boolean wasAdded = false;
        try {
            wasAdded = set.add(o);
            return wasAdded;
        }
        finally {
            if (!wasAdded)
              sem.release();
        } 
    }

    public boolean remove(Object o) {
        boolean wasRemoved = set.remove(o);
        if (wasRemoved)
          sem.release();
        return wasRemoved;
    } 
}


/*5.5.4 Barrier : all the threads must come at a barrier point at the same time in order to proceed. Barrier waiting for threads, latch waiting for event.

     If all threads meet at the barrier point, the barrier has been successfully passed, in which case all threads are released and the barrier is reset so it can be used again. 
     If a call to await times out or a thread blocked in await is interrupted, then the barrier is considered broken and all outstanding calls to await terminate with BrokenBarrierException. 
     If the barrier is successfully passed, await returns a unique arrival index for each thread, which can be used to "elect" a leader that takes some special action in the next iteration. 
     CyclicBar rier also lets you pass a barrier action to the constructor; this is a Runnable that is executed (in one of the subtask threads) when the barrier is successfully passed but before the blocked threads are released.

     http://tutorials.jenkov.com/java-util-concurrent/cyclicbarrier.html
*/



// 5.6 Build an efficient scalable result cache
// Listing 5.16 Inital cache attempt using HashMap and Synchronization
public interface Computable<A, V> {
    V compute(A arg) throws InterruptedException;
}
public class ExpensiveFunction implements Computable<String, BigInteger> {
    public BigInteger compute(String arg) {
        // after heavy load computation ...
        return new BigInteger(arg);
    } 
}

public class Memorizer1<A, V> implements Computable<A, V> {
    @GuardedBy("this")
    private final Map<A, V> cache = new HashMap<A, V>();
    private final Computable<A, V> c;
    public Memorizer1(Computable<A, V> c) {
        this.c = c;
    }
    // inefficient here, only allow one thread do the compute because hashmap is not thread safe
    public synchronized V compute(A arg) throws InterruptedException {
        V result = cache.get(arg);
        if (result == null) {
            result = c.compute(arg);
            cache.put(arg, result);
        }
        return result;
    }
}

// Listing 5.17 Replacing HashMap with ConcurrentHashMap
// still expensive in some cases, if two same arg start computing at same time, cache is useless
public class Memorizer2<A, V> implements Computable<A, V> {
    private final Map<A, V> cache = new ConcurrentHashMap<A, V>();
    private final Computable<A, V> c;
    public Memorizer2(Computable<A, V> c) { this.c = c; }
    public V compute(A arg) throws InterruptedException {
        V result = cache.get(arg);
        if (result == null) {
            result = c.compute(arg);
            cache.put(arg, result);
        }
        return result;
    }
}

// Listing 5.18 Memorizing Wrapper Using Future Task
// still have a window that two same arg call compute at the same time, but the window is
// smaller than the Memorizer2, because this one start running after put the arg into map,
// but Memorizer2 put the arg in map after running the whole computation
public class Memorizer3<A, V> implements Computable<A, V> {
    private final Map<A, Future<V>> cache = new ConcurrentHashMap<A, Future<V>>();
    private final Computable<A, V> c;
    public Memorizer3(Computable<A, V> c) { this.c = c; }
    public V compute(final A arg) throws InterruptedException {
        Future<V> f = cache.get(arg);
        if (f == null) {
            Callable<V> eval = new Callable<V>() {
                public V call() throws InterruptedException {
                    return c.compute(arg);
                }
            };
            FutureTask<V> ft = new FutureTask<V>(eval);
            f = ft;
            cache.put(arg, ft);
            ft.run(); // call to c.compute happens here
        }
        try {
            return f.get();
        } catch (ExecutionException e) {
            throw launderThrowable(e.getCause());
        }
    } 
}

// Listing 5.19 Final Implementation

public class Memorizer<A, V> implements Computable<A, V> {
    private final ConcurrentMap<A, Future<V>> cache
        = new ConcurrentHashMap<A, Future<V>>();
    private final Computable<A, V> c;
    public Memorizer(Computable<A, V> c) { this.c = c; }
    public V compute(final A arg) throws InterruptedException {
        while (true) {
            Future<V> f = cache.get(arg);
            if (f == null) {
                Callable<V> eval = new Callable<V>() {
                    public V call() throws InterruptedException {
                        return c.compute(arg);
                    }
                };
                FutureTask<V> ft = new FutureTask<V>(eval);
                // a implicit lock here, so if same args come, 
                // one of them will be blocked by the other here 
                // and jump directly to the try
                f = cache.putIfAbsent(arg, ft);
                if (f == null) { f = ft; ft.run(); }
            }

            try {
                return f.get();
            } catch (CancellationException e) {
                cache.remove(arg, f);
            } catch (ExecutionException e) {
                throw launderThrowable(e.getCause());
            }
        } 
    }
}
// Listing 5.20 Factorizing Servlet that Caches Results Using Memorizer.
@ThreadSafe
public class Factorizer implements Servlet {
    private final Computable<BigInteger, BigInteger[]> c =
        new Computable<BigInteger, BigInteger[]>() {
            public BigInteger[] compute(BigInteger arg) {
                return factor(arg);
            } 
        };
    private final Computable<BigInteger, BigInteger[]> cache
        = new Memorizer<BigInteger, BigInteger[]>(c);
    public void service(ServletRequest req,
                        ServletResponse resp) {
        try {
            BigInteger i = extractFromRequest(req);
            encodeIntoResponse(resp, cache.compute(i));
        } catch (InterruptedException e) {
            encodeError(resp, "factorization interrupted");
        } 
    }
}

// SUMMARY OF PART1
/*
• It's the mutable state, stupid. [1]
All concurrency issues boil down to coordinating access to mutable state. The less mutable state, the easier it is to
ensure thread safety.
• Make fields final unless they need to be mutable.
• Immutable objects are automatically thread‐safe.
Immutable objects simplify concurrent programming tremendously. They are simpler and safer, and can be shared freely without locking or defensive copying.
• Encapsulation makes it practical to manage the complexity.
 
You could write a thread‐safe program with all data stored in global variables, but why would you want to? Encapsulating data within objects makes it easier to preserve their invariants; encapsulating synchronization within objects makes it easier to comply with their synchronization policy.
• Guard each mutable variable with a lock.
• Guard all variables in an invariant with the same lock.
• Hold locks for the duration of compound actions.
• A program that accesses a mutable variable from multiple threads without synchronization is a broken program.
• Don't rely on clever reasoning about why you don't need to synchronize.
• Include thread safety in the design processor explicitly document that your class is not thread‐safe.
• Document your synchronization policy.
*/
 
