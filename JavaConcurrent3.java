// 10.1 DeadLock
/* 10.1.1 Lock-ordering Deadlock 
			A program will be free of lock‐ordering deadlocks if all threads acquire the locks they need in a fixed global order.
*/

/* 10.1.2 Dynamic Lock Order Deadlocks 
	the order to lock may be arbitrary and cannot be decided globally
*/

// Listing 10.2 Dynamic Lock-ordering Deadlock
	// if A send money to B, at the same time B is sending money to A, deadlock happened!
public void transferMoney(Account fromAccount, Account toAccount, DollarAmount amount) throws InsufficientFundsException {
  synchronized (fromAccount) {
      synchronized (toAccount) {
          if (fromAccount.getBalance().compareTo(amount) < 0) throw new InsufficientFundsException();
          else {
              fromAccount.debit(amount);
              toAccount.credit(amount);
					} 
			}
	} 
}

// Listing 10.3 Inducing a Lock Ordering to Avoid Deadlock
	// Using hash or unique object to decide lock obtain order

private static final Object tieLock = new Object();
public void transferMoney(final Account fromAcct, final Account toAcct, final DollarAmount amount) throws InsufficientFundsException {
		class Helper {
				public void transfer() throws InsufficientFundsException {
						if (fromAcct.getBalance().compareTo(amount) < 0) throw new InsufficientFundsException();
						else {
								fromAcct.debit(amount);
								toAcct.credit(amount);
						} 
				}
		}
    int fromHash = System.identityHashCode(fromAcct);
    int toHash = System.identityHashCode(toAcct);
    if (fromHash < toHash) {
        synchronized (fromAcct) {
            synchronized (toAcct) {
                new Helper().transfer();
						} 
				}
		} else if (fromHash > toHash) {
        synchronized (toAcct) {
            synchronized (fromAcct) {
                new Helper().transfer();
						} 
				}
		} else {

				// two hash code are equals may happen, so if there is a equal hashcode,
			  // use another tieLock first in case of LockOrder deadlock
        synchronized (tieLock) {
            synchronized (fromAcct) {
                synchronized (toAcct) {
				} 
			}
		   new Helper().transfer();
		}
	} 
}

// Listing 10.5 Lock-ordering Deaklock Between Cooperating Objects. Very bad code
// Warning: deadlock-prone!
// at time point t
// Taxi in thread A called setLocation, it will require lock of taxi, so A -> taxiLock, and then A need dispatcher lock for notifyAvailable
// dispatcher in thread B called getImage, it will require lock of dispatcher, so B -> dispatcherLock, and B need taxi lock for getLocation
// then deadlock here!!
class Taxi {
    @GuardedBy("this") private Point location, destination;
    private final Dispatcher dispatcher;
    public Taxi(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
		}
    public synchronized Point getLocation() {
        return location;
		}
    public synchronized void setLocation(Point location) {
        this.location = location;
        if (location.equals(destination)) {
        	dispatcher.notifyAvailable(this);
        }
		} 
}

class Dispatcher {
    @GuardedBy("this") private final Set<Taxi> taxis;
    @GuardedBy("this") private final Set<Taxi> availableTaxis;
    public Dispatcher() {
        taxis = new HashSet<Taxi>();
        availableTaxis = new HashSet<Taxi>();
		}

    public synchronized void notifyAvailable(Taxi taxi) {
        availableTaxis.add(taxi);
		}

    public synchronized Image getImage() {
        Image image = new Image();
        for (Taxi t : taxis) {
        	Image image = new Image();
        	for(Taxi t : taxis) {
        		image.drawMarker(t.getLocation());
        	}
        }
        return iamge;
		} 
}

// 10.1.4 Open Calls
// Calling a method with no locks held is called an open call 
//  Strive to use open calls throughout your program. Programs that rely on open calls are far easier to analyze for deadlock‐freedom than those that allow calls to alien methods with locks held.

// 10.2 Avoiding Deadlock and diagnosing deadlocks

// 10.2.1 Using Timed Lock Attempts, lick tryLock, instead of instrinsic lock

// 10.3 Other Liveness Hazards
// 10.3.1 Starvation: Avoid the temptation to use thread priorities, since they increase platform dependence and can cause liveness problems. Most concurrent applications can use the default priority for all threads.
// 10.3.2 Poor Responsiveness: Using priority makes sense here. Make the thread need responsiveness in high priority, and others in background low priority
// 10.3.3 Live Lock: When a thread retry for ever but put at the head of queue, or two thread change response to the others. Solution: introduce some randomness into the retry mechanism.

// Chapter 11 Performance
// Scalability describes the ability to improve throughput or capacity when additional computing resources are added.
// Avoiding premature optimization. Make it right first, then make it fast

// Amdahl's Law
// Speedup <= 1 / (F + (1 - F) / N)
// F is the portion rate of serialized computation


// 11.3 Costs Introduced by Threads
// 11.3.1 Context Switching
// 	if you have more threads than core
// 	high kernal usage may indicate heacy scheduling activities
//
// 11.3.2 Memory Synchronization
// 11.3.3 Blocking
//
// 11.4 Reducing Lock Contention
// 	The principle threat to scalability in concurrent applications is the exclusive resource lock. 
// 	3 ways to reduce lock contention: 
// 	1. reduce the duration for which lock is hold
// 	2. reduce the frequency with which lock are requested, I think here, it means by seperate lock for independent state, we can reduce the access to each lock, then improve the global performance.
// 	3. replace exclusive locks with coordination mechnism that permit greater concurrency
// 11.4.1 Narrowing lock scope, but remember, sync will have context switch cost, so splitting lock without thinking is not wise. In practice, worry about the size of a synchronized block only when you can move "substantial" computation or blocking operations out of it.
// 11.4.2 Recuding Lock granularity. seperate locks to guard multiple independent state variable, reduce the granulariy at which locking occurs, potentially allowing greater scalability. 
// 11.4.3 Lock Striping: eg: generate a series of lock according to hash function, one lock to one bucket map
// 11.4.4 Avoiding Hot Zone
// 	save size in counter may cache value for single thread collection, but for concurrent collection, it may produce a hot zone, and bottleneck of the performance. ConcurrentHashmap use counter for each stripe

// 11.4.5 Alternatives to Exclusive Locks: reader-writer lock, atomic refernce, notice, atomic reference do not eliminate hot fields, but reduce the cost of update it.
// 11.4.6 Monitoring CPU Util
// if asymetrical untilized, try to improve parallelism
// if CPU is not fully used
// 	1. check if the load is not sufficient, or client capacity is bounded
// 	2. io bound, use iostat or perfmon for disk bound check
// 	3. Externally bound, by service or db
// 	4. Lock contention, check "waiting to lock monitor ..."
// 11.4.7 Allocating new obj is cheap, cheaper than synchronizing
//
// 11.6 Recuding Context Switch Overhead
// eg : using single thread to log, then every thread has less service time, so less context switch.


/*Ch13 Explicit Lock*/
// Listing 13.1 Lock interface
public interface Lock {
	void lock();
	void lockInterruptibly() throws InterruptedException;
	boolean tryLock();
	boolean tryLock(long timeout, TimeUnit unit)
		throws InterruptedException;
	void unlock();
	Condition newCondition();
}

// Listing 13.2 Guarding Object State Using Reentrantlock
Lock lock = new ReentrantLock();
lock.lock();
try {
	// update object state
        // catch exceptions and restore invariants if necessary
} finally {
    lock.unlock();
}

// Listing 13.3 Avoiding Lock-ordering Deadlock Using TryLock
// if try lock cannot get it, if it cannot get, it will retry if it cannot
// get both locks
public boolean transferMoney(Account fromAcct,
		Account toAcct,
		DollarAmount amount,
		long timeout,
		TimeUnit unit)
	throws InsufficientFundsException, InterruptedException {
	long fixedDelay = getFixedDelayComponentNanos(timeout, unit);
	long randMod = getRandomDelayModulusNanos(timeout, unit);
	long stopTime = System.nanoTime() + unit.toNanos(timeout);
	while (true) {
		if (fromAcct.lock.tryLock()) {
			try {
				if (toAcct.lock.tryLock()) {
					try {
						if (fromAcct.getBalance().compareTo(amount)
								< 0)
							throw new InsufficientFundsException();
						else {
							fromAcct.debit(amount);
							toAcct.credit(amount);
							return true;
						}
					} finally {
						toAcct.lock.unlock();
					}
				}
			} finally {
				fromAcct.lock.unlock();
			}
		}
		if (System.nanoTime() >= stopTime)
			return false;
		NANOSECONDS.sleep(fixedDelay + rnd.nextLong() % randMod);
	}
}

// Listing 13.4 Locking with a time budget
public boolean trySendOnSharedLine(String message,
		long timeout, TimeUnit unit)
	throws InterruptedException {
	long nanosToLock = unit.toNanos(timeout)
		- estimatedNanosToSend(message);
	if (!lock.tryLock(nanosToLock, NANOSECONDS))
		return false;
	try {
		return sendOnSharedLine(message);
	} finally {
		lock.unlock();
	}
}

// Listing 13.5 Interruptible Lock Acquisition
public boolean sendOnSharedLine(String message)
	throws InterruptedException {
	lock.lockInterruptibly();
	try {
		return cancellableSendOnSharedLine(message);
	} finally {
		lock.unlock();
	}
}
private boolean cancellableSendOnSharedLine(String message)
	throws InterruptedException {  }


// 13.1.3 Non-block-structured locking
// refer to hand-over-hand locking or lock coupling.

// 13.3 Fairness
// fair lock : new request thread is queued
// non-fair lock : thread can jump ahead in the queue if the lock happens to be available when it is requested, with nonfair lock, the thread is queued only if the lock is currently held.
// * tryLock always barges, even for fair lock
//13.4 Reentrantlock vs Intrinsic Lock
// in Java 6 thread dump is only for intrinsic lock
//

// 13.5 Reader Writer Lock
// multiple reader and single writer
//
/* Implementation Choice for read-write lock
 * 
r* Release preference: when lock is realeased and queued reader and writers  want to access the lock, who should be given preference?
 * 
 * Reader barging: If the lock is held by readers but there are waiting writers, should newly arraiving readerrrs be granted immediate access, or should they wait behind the writers? Allowing readers to barge ahead of writers enhances concurrency but runs the risk of starving writers
 *
 * reentrance: are the read and write lock reentrant?
 *
 * Downgrading: if a thread holds the write lock, can it acquire the raed lock without realeasiing the write lock? This would let the writer downgrade to a read lock without letting other writers modify the guarded resources in the same time.
 *
 * Upgrading. Can a read lock be upgraded to a write lock in prefernce toother waiting readers or writers? Most read=write lock implementations do notsupport upgrading, because without an explicit upgrade operation it is deadlock prone. 
 * */

// Chapter 14 Building Customized Synchronizer
//
// Listing 14.1 Sturcture of Blocking State-dependent Actions
//

void blockingAction() throws InterruptedException {
    acquire lock on object state
	while (precondition does not hold) {
	    release lock
	    wait until precondition might hold
	    optionally fail if interrupted or timeout expires
	    reacquire lock
	}
    perform action
}


// Listing 14.2 Base Class for Bounded Buffer implementations

@ThreadSafe
public abstract class BaseBoundedBuffer<V> {
	@GuardedBy("this") private final V[] buf;
	@GuardedBy("this") private int tail;
	@GuardedBy("this") private int head;
	@GuardedBy("this") private int count;
	protected BaseBoundedBuffer(int capacity) {
		this.buf = (V[]) new Object[capacity];
	}
	protected synchronized final void doPut(V v) {
		buf[tail] = v;
		if (++tail == buf.length)
			tail = 0;
		++count;
	}
	protected synchronized final V doTake() {
		V v = buf[head];
		buf[head] = null;
		if (++head == buf.length)
			head = 0;
		--count;
		return v;
	}
	public synchronized final boolean isFull() {
		return count == buf.length;
	}
	public synchronized final boolean isEmpty() {
		return count == 0;
	}
}


// Listing 14.6 Bounded Buffer Using Condition Queues

@ThreadSafe
public class BoundedBuffer<V> extends BaseBoundedBuffer<V> {
    // CONDITION PREDICATE: not-full (!isFull())
    // CONDITION PREDICATE: not-empty (!isEmpty())
    public BoundedBuffer(int size) { super(size); }
    // BLOCKS-UNTIL: not-full
    public synchronized void put(V v) throws InterruptedException {
    while (isFull())
        wait();
	doPut(v);
        notifyAll();
    }
    // BLOCKS-UNTIL: not-empty
    public synchronized V take() throws InterruptedException {
	while (isEmpty())
	   wait();
	V v = doTake();
	notifyAll();
	return v;
    }
}


// 14.2 Condition Queue
// 14.2.1 The condition predicate
// Document the condition predicate associated with a condition queue and the operations that wait on them
//

// Every call to wait is implicitly associated with a specific condition predicate. When calling wait regarding a particular condition predicate, the caller must already hold the lock associated with the condition queue, and that lock must also guard the state variables from which the condition predicate is composed.
//

// Listing 14.7 Cannonical Form for state-dependent methods
//


void stateDependentMethod() throws InterruptedException {
	// condition predicate must be guarded by lock
	synchronized(lock) {
	    while (!conditionPredicate())
	        lock.wait();
        // object is now in desired state
	}
}
/*
When using condition waits (Object.wait or Condition.await):
	• Always have a condition predicatesome test of object state that must hold before proceeding;
	• Always test the condition predicate before calling wait, and again after returning from wait;
	• Always call wait in a loop;
	• Ensure that the state variables making up the condition predicate are guarded by the lock associated with the condition queue;
	• Hold the lock associated with the the condition queue when calling wait, notify, or notifyAll; and
	• Do not release the lock after checking the condition predicate but before acting on it.
*/


// 14.2.4 Notification
// Whenever you wait on a condition, make sure that someone will perform a notification whenever the condition predicate becomes true.
//
// Single notify can be used instead of notifyAll only when both of the following conditions hold:
// • Uniform waiters. Only one condition predicate is associated with the condition queue, and each thread executes the
// same logic upon returning from wait; and
// • One‐in, one‐out. A notification on the condition variable enables at most one thread to proceed.
//

// Listing 14.8 Using Conditional Notification in BoundedBuffer.put

public synchronized void put(V v) throws InterruptedException {
	while (isFull())
	    wait();
	boolean wasEmpty = isEmpty();
	doPut(v);
	if (wasEmpty)
	    notifyAll();
}


// 14.2.6 Subclass Safety Issues
//
// A state‐dependent class should either fully expose (and document) its waiting and notification protocols to subclasses,
// or prevent subclasses from participating in them at all.
//

// Listing 14.10 Condition Interface
public interface Condition {
    void await() throws InterruptedException;
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    void awaitUninterruptibly();
    boolean awaitUntil(Date deadline) throws InterruptedException;
    void signal();
    void signalAll();
}
//  Hazard warning: The equivalents of wait, notify, and notifyAll for Condition objects are await, signal, and
// signalAll. However, Condition extends Object, which means that it also has wait and notify methods. Be sure to
// use the proper versions ‐ await and signal - instead!

@ThreadSafe
public class ConditionBoundedBuffer<T> {
    protected final Lock lock = new ReentrantLock();
    // CONDITION PREDICATE: notFull (count < items.length)
    private final Condition notFull = lock.newCondition();
    // CONDITION PREDICATE: notEmpty (count > 0)
     private final Condition notEmpty = lock.newCondition();
     @GuardedBy("lock")
     private final T[] items = (T[]) new Object[BUFFER_SIZE];
     @GuardedBy("lock") private int tail, head, count;
     // BLOCKS-UNTIL: notFull
     public void put(T x) throws InterruptedException {
        lock.lock();
	try {
	    while (count == items.length)
	        notFull.await();
	        items[tail] = x;
	        if (++tail == items.length)
	            notEmpty.signal();
		} finally {
		    lock.unlock();
                }
     }
    // BLOCKS-UNTIL: notEmpty
    public T take() throws InterruptedException {
	lock.lock();
	try {
	    while (count == 0)
	        notEmpty.await();
            T x = items[head];
	    items[head] = null;
	    if (++head == items.length)
                head = 0;
                --count;
                notFull.signal();
                return x;
            } finally {
                lock.unlock();
            }	
    }
}

/* Choose between using explicit Conditions and intrinsic condition queues in the same way as you would choose
between ReentrantLock and synchronized: use Condition if you need its advanced features such as fair queueing or
multiple wait sets per lock, and otherwise prefer intrinsic condition queues. (If you already use ReentrantLock because	you need its advanced features, the choice is already made.) */


// Listing 14.12 Counting Semaphore Implemented Using Lock


public class SemaphoreOnLock {
	private final Lock lock = new ReentrantLock();
	// CONDITION PREDICATE: permitsAvailable (permits > 0)
	private final Condition permitsAvailable = lock.newCondition();
	@GuardedBy("lock") private int permits;
	SemaphoreOnLock(int initialPermits) {
	     lock.lock();
	     try {
	        permits = initialPermits;
	     } finally {
	        lock.unlock();
	     }
	}
	// BLOCKS-UNTIL: permitsAvailable
	public void acquire() throws InterruptedException {
	    lock.lock();
	    try {
	        while (permits <= 0)
	            permitsAvailable.await();
		    --permits;
	    } finally {
	       lock.unlock();
	    }
	}
	public void release() {
	    lock.lock();
	    try {
	    	++permits;
	    	permitsAvailable.signal();
	    } finally {
	        lock.unlock();
	    }
	}
 }

// Listing 14.13 Canonical Forms for Acquisition and Release in AQS.

boolean acquire() throws InterruptedException {
	while (state does not permit acquire) {
		if (blocking acquisition requested) {
			enqueue current thread if not already queued
				block current thread
		}
		else
			return failure
	}
	possibly update synchronization state
		dequeue thread if it was queued
		return success
}
void release() {
	update synchronization state
		if (new state may permit a blocked thread to acquire)
			unblock one or more queued threads
}

// A synchronizer supporting exclusive acquisition should implement the protected methods TRyAcquire, TRyRelease,
// and isHeldExclusively, and those supporting shared acquisition should implement tryAcquireShared and
// TRyReleaseShared.
//

// 14.5.1 A simple Latch Using AQS
// Listing 14.14 Binary Latch Using AbstractQueuedSynchronizer

@ThreadSafe
public class OneShotLatch {
	private final Sync sync = new Sync();
	public void signal() { sync.releaseShared(0); }
	public void await() throws InterruptedException {
		sync.acquireSharedInterruptibly(0);
	}
	private class Sync extends AbstractQueuedSynchronizer {
		protected int tryAcquireShared(int ignored) {
		    // Succeed if latch is open (state == 1), else fail
		  return (getState() == 1) ? 1 : -1;
		 }
		protected boolean tryReleaseShared(int ignored) {
		    setState(1); // Latch is now open
		    return true; // Other threads may now be able to acquire
		}
	}
}


// Chapter 15 Atomic Variables and Non-blocking Sync

// 15.2.1 Compare and Swap
// CAS: Compare and swap instruction: CAS has three operands ] a memory location V on which to operate, the expected old
// value A, and the new value B. CAS atomically updates V to the new value B, but only if the value in V matches the
// expected old value A; otherwise it does nothing. In either case, it returns the value currently in V. (The variant called
// compare]and]set instead returns whether the operation succeeded.) CAS means "I think V should have the value A; if it
// does, put B there, otherwise don't change it but tell me I was wrong." CAS is an optimistic technique ] it proceeds with
// the update in the hope of success, and can detect failure if another thread has updated the variable since it was last
// examined.
//
//
//

// Listing 15.1 Simulated CAS Operation

@ThreadSafe
public class SimulatedCAS {
	@GuardedBy("this") private int value;
	public synchronized int get() { return value; }
	public synchronized int compareAndSwap(int expectedValue,
			int newValue) {
		int oldValue = value;
		if (oldValue == expectedValue)
			value = newValue;
		return oldValue;
	}
	public synchronized boolean compareAndSet(int expectedValue,
			int newValue) {
		return (expectedValue
				== compareAndSwap(expectedValue, newValue));
	}
}


// Listing 15.2 Non-blocking Counter Using CAS

@ThreadSafe
public class CasCounter {
	private SimulatedCAS value;
	public int getValue() {
		return value.get();
	}
	public int increment() {
		int v;
		do {
			v = value.get();
		}
		while (v != value.compareAndSwap(v, v + 1));
		return v + 1;
	}
}



// 15.3.2 Lock VS Atomic Variables
//

// Listing 15.6 Non-blocking Stack

@ThreadSafe
public class ConcurrentStack <E> {
	AtomicReference<Node<E>> top = new AtomicReference<Node<E>>();
	public void push(E item) {
		Node<E> newHead = new Node<E>(item);
		Node<E> oldHead;
		do {
			oldHead = top.get();
			newHead.next = oldHead;
		} while (!top.compareAndSet(oldHead, newHead));
	}
	public E pop() {
		Node<E> oldHead;
		Node<E> newHead;
		do {
			oldHead = top.get();
			if (oldHead == null)
				return null;
			newHead = oldHead.next;
		} while (!top.compareAndSet(oldHead, newHead));
		return oldHead.item;
	}
	private static class Node <E> {
		public final E item;
		public Node<E> next;
		public Node(E item) {
			this.item = item;
		}
	}
}

// 15.4.2 Non-blocking Linked List
// As in many queue algorithms, an empty queue
// consists of a "sentinel" or "dummy" node, and the head and tail pointers are initialized to refer to the sentinel. The tail
// pointer always refers to the sentinel (if the queue is empty), the last element in the queue, or (in the case that an
// operation is in mid‐update) the second‐to‐last element.


// Chapter 16 Java Memory Model
//
// The rules for happens‐before are:
// • Program order rule. Each action in a thread happens‐before every action in that thread that comes later in the
// program order.
// • Monitor lock rule. An unlock on a monitor lock happens‐before every subsequent lock on that same monitor lock.[3]
// • Volatile variable rule. A write to a volatile field happens‐before every subsequent read of that same field.[4]
// • Thread start rule. A call to Thread.start on a thread happens‐before every action in the started thread.
// • Thread termination rule. Any action in a thread happens‐before any other thread detects that thread has
// terminated, either by successfully return from Thread.join or by Thread.isAlive returning false.
// • Interruption rule. A thread calling interrupt on another thread happens‐before the interrupted thread detects the
// interrupt (either by having InterruptedException thrown, or invoking isInterrupted or interrupted).
// • Finalizer rule. The end of a constructor for an object happens‐before the start of the finalizer for that object.
// • Transitivity. If A happens‐before B, and B happens‐before C, then A happens‐before C.

// 16.1.4 Piggyback on Synchronization
//
// We call this technique "piggybacking" because it uses an existing happens乚before ordering that was created for some
// other reason to ensure the visibility of object X, rather than creating a happens乚before ordering specifically for
// publishing X.
// Piggybacking of the sort employed by FutureTask is quite fragile and should not be undertaken casually. However, in
// some cases piggybacking is perfectly reasonable, such as when a class commits to a happens乚before ordering between
// methods as part of its specification. For example, safe publication using a BlockingQueue is a form of piggybacking. One
// thread putting an object on a queue and another thread subsequently retrieving it constitutes safe publication because
// there is guaranteed to be sufficient internal synchronization in a BlockingQueue implementation to ensure that the
// enqueue happens乚before the dequeue.
// Other happens乚before orderings guaranteed by the class library include:
// . Placing an item in a thread乚safe collection happens乚before another thread retrieves that item from the
// collection;
// . Counting down on a CountDownLatch happens乚before a thread returns from await on that latch;
// . Releasing a permit to a Semaphore happens乚before acquiring a permit from that same Semaphore;
// . Actions taken by the task represented by a Future happens乚before another thread successfully returns from
// Future.get;
// . Submitting a Runnable or Callable to an Executor happens乚before the task begins execution; and
// . A thread arriving at a CyclicBarrier or Exchanger happens乚before the other threads are released from that
// same barrier or exchange point. If CyclicBarrier uses a barrier action, arriving at the barrier happens乚before
// the barrier action, which in turn happens乚before threads are released from the barrier.


// 16.2.3 Safe Initialization Idioms
// Listing 16.4 Thread-safe Lazy Initialization
@ThreadSafe
public class SafeLazyInitialization {
	private static Resource resource;
	public synchronized static Resource getInstance() {
		if (resource == null)
			resource = new Resource();
		return resource;
	}
}

// Listing 16.5 Thread-safe Eager Initializations
@ThreadSafe
public class EagerInitialization {
	private static Resource resource = new Resource();
	public static Resource getResource() { return resource; }
}

// Listing 16.6 Lazy Initialization Holder Class Idiom
@ThreadSafe
public class ResourceFactory {
	private static class ResourceHolder {
		public static Resource resource = new Resource();
	}
	public static Resource getResource() {
		return ResourceHolder.resource ;
	}
}

// Initialization safety makes visibility guarantees only for the values that are reachable through final fields as of the time
// the constructor finishes. For values reachable through non‐final fields, or values that may change after construction, you
// must use synchronization to ensure visibility.
//

// Appendix A. Annotations for Concurrency
// We've used annotations such as @GuardedBy and @ThreadSafe to show how thread乚safety promises and
// synchronization policies can be documented. This appendix documents these annotations; their source code can be
// downloaded from this book's website. (There are, of course, additional thread乚safety promises and implementation
// details that should be documented but that are not captured by this minimal set of annotations.)
// A.1. Class Annotations
// We use three class乚level annotations to describe a class's intended thread乚safety promises: @Immutable, @ThreadSafe,
// and @NotThreadSafe. @Immutable means, of course, that the class is immutable, and implies @ThreadSafe.
// @NotThreadSafe is optional 乚 if a class is not annotated as thread乚safe, it should be presumed not to be thread乚safe, but
// if you want to make it extra clear, use @NotThreadSafe.
// These annotations are relatively unintrusive and are beneficial to both users and maintainers. Users can see
// immediately whether a class is thread乚safe, and maintainers can see immediately whether thread乚safety guarantees
// must be preserved. Annotations are also useful to a third constituency: tools. Static code analysis tools may be able to
// verify that the code complies with the contract indicated by the annotation, such as verifying that a class annotated with
// @Immutable actually is immutable.
// A.2. Field and Method Annotations
// The class乚level annotations above are part of the public documentation for the class. Other aspects of a class's threadsafety
// strategy are entirely for maintainers and are not part of its public documentation.
// Classes that use locking should document which state variables are guarded with which locks, and which locks are used
// to guard those variables. A common source of inadvertent non乚thread乚safety is when a thread乚safe class consistently
// uses locking to guard its state, but is later modified to add either new state variables that are not adequately guarded by
// locking, or new methods that do not use locking properly to guard the existing state variables. Documenting which
// variables are guarded by which locks can help prevent both types of omissions.
// @GuardedBy(lock) documents that a field or method should be accessed only with a specific lock held. The lock
// argument identifies the lock that should be held when accessing the annotated field or method. The possible values for
// lock are:
// . @GuardedBy("this"), meaning the intrinsic lock on the containing object (the object of which the method or
// field is a member);
// . @GuardedBy("fieldName"), meaning the lock associated with the object referenced by the named field, either
// an intrinsic lock (for fields that do not refer to a Lock) or an explicit Lock (for fields that refer to a Lock);
// . @GuardedBy("ClassName.fieldName"), like @GuardedBy("fieldName"), but referencing a lock object held in a
// static field of another class;
// . @GuardedBy("methodName()"), meaning the lock object that is returned by calling the named method;
// . @GuardedBy("ClassName.class"), meaning the class literal object for the named class.
// Using @GuardedBy to identify each state variable that needs locking and which lock guards it can assist in maintenance
// and code reviews, and can help automated analysis tools spot potential thread乚safety errors.
