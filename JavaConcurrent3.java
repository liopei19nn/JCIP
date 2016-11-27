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







	