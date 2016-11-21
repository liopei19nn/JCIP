/* Chapter 6 Task Execution */
// Listing 6.1 Single Thread Web Server
class SingleThreadWebServer {
    public static void main(String[] args) throws IOException {
    	  ServerSocket socket = new ServerSocket(80);
				while (true) {
				    Socket connection = socket.accept();
				    handleRequest(connection);
				}
		} 
}

// Listing 6.2 Thread Per Task Web Server
// Enough for a moderate request number
/* Drawback 
		1. Resource consumption. Active threads consume system resources, especially memory. When there are more runnable threads than available processors, threads sit idle. 
		Having many idle threads can tie up a lot of memory, putting pressure on the garbage collector, and having many threads competing for the CPUs can impose other performance costs as well. 
		If you have enough threads to keep all the CPUs busy, creating more threads won't help and may even hurt.

		2.  There is a limit on how many threads can be created. The limit varies by platform and is affected by factors including JVM invocation parameters, the requested stack size in the Thread constructor, 
		and limits on threads placed by the underlying operating system.[2] When you hit this limit, the most likely result is an OutOfMemoryError. trying to recover from such an error is very risky; 
		it is far easier to structure your program to avoid hitting this limit.

		3. 
		


*/
class ThreadPerTaskWebServer {
    public static void main(String[] args) throws IOException {
    	ServerSocket socket = new ServerSocket(80);
	    while (true) {
	        final  Socket connection = socket.accept();
		    Runnable task = new Runnable() {
				public void run() {
					handleRequest(connection);
						} 
				};
		    new Thread(task).start();
		}
	} 
}

// Listing 6.4 Web Server Using Thread Pool

class TaskExecutionWebServer {
    private static final int NTHREADS = 100;
        private static final Executor exec = Executors.newFixedThreadPool(NTHREADS);
	    public static void main(String[] args) throws IOException {
		ServerSocket socket = new ServerSocket(80);
		    while (true) {
			final Socket connection = socket.accept();
			Runnable task = new Runnable() {
			    public void run() {
				handleRequest(connection);
			    }
			};
			exec.execute(task);
		    }
	    }
}


// 6.2.2 Using Executor can implement different flexible policy, instead of just running the Thread.start();

// 6.2.3 Thread Pools : newFixedThreadPool, newCachedThreadPool, newSingleThreadExecutor(provide internal sync, promise memory writes visible to subsequent task.)
//

// Listing 6.7 Lifcycle method in ExecutorService 

public interface ExecutorService extends Executor {
    void shutdown(); // graceful shutdown, will stop accepting new task, but will complete all allowed task. 
    List<Runnable> shutdownNow();// abruptly shutdown.
    boolean isShutdown();
    boolean isShutdown();
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    //
}

// 6.2.5 Use ScheduledThreadPoolExecutor for deferred and periodic tasks, instead of Timer.

// If you need to build your own scheduling service, you may still be able to take advantage of the library by using a DelayQueue, a BlockingQueue implementation that provides the scheduling functionality of ScheduledThreadPoolExecutor. 
// A DelayQueue manages a collection of Delayed objects. A Delayed has a delay time associated with it: DelayQueue lets you take an element only if its delay has expired. Objects are returned from a DelayQueue ordered by the time associated with their delay.


// 6.3. Finding Exploitable Parallelism
// 6.3.5 Completion Service: Combining Executor and BlockingQueue

// Listing 6.15 Uing CompletionService to render page elements as they become available

public class Renderer {
    private final ExecutorService executor;

    Renderer(ExecutorService executor) { this.executor = executor; }

    void renderPage(CharSequence source) {
        final List<ImageInfo> info = scanForImageInfo(source);
        // executor is shared by several ExecutorCompletionService
        CompletionService<ImageData> completionService = new ExecutorCompletionService<ImageData>(executor);
		
				for (final ImageInfo imageInfo : info)
						completionService.submit(new Callable<ImageData>() {
				    		public ImageData call() {
				             return imageInfo.downloadImage();
				         }
						});
				renderText(source);

				try {
						for(int t = 0,n = info.size(); t < n; t++){
								// this will waiting if no available
								Future<ImageData> f = completionService.take();
								ImageData imageData = f.get();
						    renderImage(imageData);
						}
				} catch (InterruptedException e) {
				    Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
				    throw launderThrowable(e.getCause());
				}
		}
}

// Listing 6.15 Fetching an AD with a Time Budget: Timeout future
Page renderPageWithAd() throws InterruptedException {
    long endNanos = System.nanoTime() + TIME_BUDGET;
    Future<Ad> f = exec.submit(new FetchAdTask());
    // Render the page while waiting for the ad
    Page page = renderPageBody();
    Ad ad;
    try {
        // Only wait for the remaining time budget
    		// this timeLeft can be negative, but java.util.concurrent treat 
    		// negative timeout as 0, so this will work
        long timeLeft = endNanos - System.nanoTime();
        ad = f.get(timeLeft, NANOSECONDS);
    } catch (ExecutionException e) {
        ad = DEFAULT_AD;
    } catch (TimeoutException e) {
        ad = DEFAULT_AD;
        f.cancel(true); // cancel if the task is currently running.
    }
    page.setAd(ad);
    return page;
}

// Listing 6.17 Requesting Travel Quotes Under a Time Budget
public class TimeBudget {
    private static ExecutorService exec = Executors.newCachedThreadPool(); // no size limit

    public List<TravelQuote> getRankedTravelQuotes(
    		TravelInfo travelInfo, Set<TravelCompany> companies,
				Comparator<TravelQuote> ranking, long time, TimeUnit unit)
    		throws InterruptedException {
        		List<QuoteTask> tasks = new ArrayList<QuoteTask>();
        		for (TravelCompany company : companies)
          			tasks.add(new QuoteTask(company, travelInfo));

        		List<Future<TravelQuote>> futures = exec.invokeAll(tasks, time, unit);

        		List<TravelQuote> quotes = new ArrayList<TravelQuote>(tasks.size());
        		Iterator<QuoteTask> taskIter = tasks.iterator();
		        for (Future<TravelQuote> f : futures) {
		            QuoteTask task = taskIter.next();
		            try {
		                quotes.add(f.get());
		            } catch (ExecutionException e) {
		                quotes.add(task.getFailureQuote(e.getCause()));
		            } catch (CancellationException e) {
		                quotes.add(task.getTimeoutQuote(e));
		            }
		        }

		        Collections.sort(quotes, ranking);
    		    return quotes;
    		}
}

private class QuoteTask implements Callable<TravelQuote> {
    private final TravelCompany company;
    private final TravelInfo travelInfo;

    public QuoteTask(TravelCompany company, TravelInfo travelInfo) {
        this.company = company;
        this.travelInfo = travelInfo;
    }

    TravelQuote getFailureQuote(Throwable t) {
        return null;
    }

    TravelQuote getTimeoutQuote(CancellationException e) {
        return null;
    }

    public TravelQuote call() throws Exception {
        return company.solicitQuote(travelInfo);
    }
}


// 7.1 Task Cancellation
// 7.1.1 Interruption
// Listing 7.3 Unreliable Cancellation that can Leave Producers Stuck in a Blocking Operation. Don't Do this.
// If blocking queue is full, and put will be blocked, and if you cancel consumer now, the queue.put will block forever
// and the producer may run forever.
class BrokenPrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;
    private volatile boolean cancelled = false;
    BrokenPrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
		}
    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!cancelled)
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) { }
		}
    public void cancel() { cancelled = true;  }
}

void consumePrimes() throws InterruptedException {
    BlockingQueue<BigInteger> primes = /// ...;
    BrokenPrimeProducer producer = new BrokenPrimeProducer(primes);
    producer.start();
    try {
        while (needMorePrimes())
            consume(primes.take());
    } finally {
        producer.cancel();
    }
}


// Use Thread interupt instead of cancel
// Calling interrupt does not necessarily stop the target thread from doing what it is doing; it merely delivers the message that interruption has been requested.
// so you need set the activity being canceled to poll the status
public class Thread {
    public void interrupt() { ... }
    public boolean isInterrupted() { ... }
    public static boolean interrupted() { ... }
    // ...
}

// Listing 7.5. Using Interruption for Cancellation.
class PrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;

    PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
		}
		// if you call Thread.interupt, there will be 2 place to detect
		// the first place is in while, and second is the queue.put method.
    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!Thread.currentThread().isInterrupted())
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) {
            /*  Allow thread to exit  */
        }
		}
    public void cancel() { interrupt(); }
}


// 7.1.3 Responding to Interruption
// Practical Strategies for handling InterruptedException
// • Propagate the exception (possibly after some task‐specific cleanup), making your method an interruptible blocking method, too; or
// • Restore the interruption status so that code higher up on the call stack can deal with it.

//  Only code that implements a thread's interruption policy may swallow an interruption request. General‐purpose task and library code should never swallow interruption requests.
//  一定要handle InterruptedException, 可以throw, 可以在catch里面handle, 但是不能ignore

// Be sure to know the interruption policy before you interrupt it

// Listing 7.8. Scheduling an Interrupt on a Borrowed Thread. Don't Do this.
// Runnable r will use current thread and there is a scheduled task to interrupt r, but this task know no interruption policy,
// eg1. if r completed before interrupt
// eg2. if the interrupt got no response, r will run to complete, or never complete
private static final ScheduledExecutorService cancelExec = ...;
public static void timedRun(Runnable r, long timeout, TimeUnit unit) {
    final Thread taskThread = Thread.currentThread();
    cancelExec.schedule(new Runnable() {
        public void run() { taskThread.interrupt(); }
    }, timeout, unit);
    r.run();
}



// Listing 7.10 Cancelling Future

public static void timedRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
    Future<?> task  = taskExec.submit(r);
    try {
        task.get(timeout, unit);
    } catch (TimeoutException e ){
        // task will be cancelled below
    } catch (ExecutionException e ) {
        // exception thrown in task; rethrow
        throw launderThrowable(e.getCause());
    } finally {
        // Harmless if task already completed
        // set to true will interrupt the thread, or you allowed the task to be completed
        task.cancel(true);  // interrupt if running
    }
}


// Listing 7.11  Encapsulating Nonstandard Cancellation in a Thread by Overriding Interrupt.
public class ReaderThread extends Thread {
    private static final int BUFSZ = 512;
    private final Socket socket;
    private final InputStream in;

    public ReaderThread(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
    }

    public void interrupt() {
        try {
            socket.close();
        } catch (IOException ignored) {
        } finally {
            super.interrupt();
        }
    }

    public void run() {
        try {
            byte[] buf = new byte[BUFSZ];
            while (true) {
                int count = in.read(buf);
                if (count < 0)
                    break;
                else if (count > 0)
                    processBuffer(buf, count);
            }
        } catch (IOException e) { /* Allow thread to exit */
        }
    }

    public void processBuffer(byte[] buf, int count) {
    }
}

// 7.12 Encapsulating Nonstandard Cancellation in a Task with Newtaskfor

@ThreadSafe
class CancellingExecutor extends ThreadPoolExecutor {
    // ...
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        if (callable instanceof CancellableTask)
            return ((CancellableTask<T>) callable).newTask();
        else
            return super.newTaskFor(callable);
    }
}

interface CancellableTask <T> extends Callable<T> {
    void cancel();

    RunnableFuture<T> newTask();
}



public abstract class SocketUsingTask <T> implements CancellableTask<T> {
    @GuardedBy("this") private Socket socket;
    protected synchronized void setSocket(Socket s) {
        socket = s;
    }
    public synchronized void cancel() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }
    public RunnableFuture<T> newTask() {
        return new FutureTask<T>(this) {
            public boolean cancel(boolean mayInterruptIfRunning) {
                try {
                    SocketUsingTask.this.cancel();
                } finally {
                    return super.cancel(mayInterruptIfRunning);
                }
            }
        };
    }
}

/* 7.2 Stopping a Thread-based Service 

    Provide lifecycle methods whenever a thread‐owning service has a lifetime longer than that of the method that created it.

    Application owns services, each service owns threads
    Do not let application to shutdown threads, let service self shut down the threads they own

*/

// Listing 7.16 Logging service that uses an ExecutorService
public class LogService {
    private final ExecutorService exec = newSingleThreadExecutor();
    // ...
    public void start() { }
    public void stop() throws InterruptedException {
        try {
            exec.shutdown();
            exec.awaitTermination(TIMEOUT, UNIT);
        } finally {
            writer.close();
        }
    }
    public void log(String msg) {
        try {
            exec.execute(new WriteTask(msg));
        } catch (RejectedExecutionException ignored) { }
    }
}

// Listing 7.17, 7.18, 7.19 Shutdown with poison pills
// Poison pills work reliably only with unbounded queues. You need to know the exact number
// of producer and consumer
/*
    Extended to multiple producer: having each producer place a pill on the queue and having the consumer stop only when it receives N_PRODUCER pills
    Extended to multiple consumer: having each producer place N_CONSUMER pills in the queue

*/

public class IndexingService {
    private static final File POISON = new File("");
    private final IndexerThread consumer = new IndexerThread();
    private final CrawlerThread producer = new CrawlerThread();
    private final BlockingQueue<File> queue;
    private final FileFilter fileFilter;
    private final File root;
    class CrawlerThread extends Thread { /* Listing 7.18 */ }
    class IndexerThread extends Thread { /* Listing 7.19 */ }
    public void start() {
        producer.start();
        consumer.start();
    }
    public void stop() { producer.interrupt(); }
    public void awaitTermination() throws InterruptedException {
        consumer.join();    
    } 
}
 
 // producer
public class CrawlerThread extends Thread {
    public void run() {
        try {
            crawl(root);
        } catch (InterruptedException e) { /*  fall through  */  }
        finally {
            while (true) {
                try {
                    queue.put(POISON);
                    break;
                } catch (InterruptedException e1) { /*  retry  */ }
            }
        }
    }
    private void crawl(File root) throws InterruptedException { // ...  
    }
}

// consumer
public class IndexerThread extends Thread {
    public void run() {
        try {
            while (true) {
                File file = queue.take();
                if (file == POISON)
                    break; 
                else
                    indexFile(file);
            }
        } catch (InterruptedException consumed) { }
    } 
}


// 7.2.5 Limitation of Shutdownnow is, it will return all tasks never started, but will not return tasks in progress
// Listing 7.21 ExecutorService that keeps track of cancelled tasks after shutdown
public class TrackingExecutor extends AbstractExecutorService {
    private final ExecutorService exec;
    private final Set<Runnable> tasksCancelledAtShutdown = Collections.synchronizedSet(new HashSet<Runnable>());
    // ...
    public List<Runnable> getCancelledTasks() {
        if (!exec.isTerminated())
            throw new IllegalStateException(...);
        return new ArrayList<Runnable>(tasksCancelledAtShutdown);
    }
    public void execute(final Runnable runnable) {
        exec.execute(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    if (isShutdown() && Thread.currentThread().isInterrupted())
                        tasksCancelledAtShutdown.add(runnable);
                } 
            }
        }); 
    }
    // delegate other ExecutorService methods to exec
}



// Listing 7.23 TYpical Thread-pool worker thread structure
public void run() {
    Throwable thrown = null;
    try {
        while (!isInterrupted())
            runTask(getTaskFromWorkQueue());
    } catch (Throwable e) {
        thrown = e;
    } finally {
        threadExited(this, thrown);
    } 
}


// 7.3.1 Uncaught Exception Handler
/*
    When a thread exits due to an uncaught exception, the JVM reports this event to an application‐provided UncaughtExceptionHandler (see Listing 7.24); 
    if no handler exists, the default behavior is to print the stack trace to System.err.[8]
*/
// Listing 7.24
public interface UncaughtExceptionHandler {
    void uncaughtException(Thread t, Throwable e);
}

/*
    Somewhat confusingly, exceptions thrown from tasks make it to the uncaught exception handler only for tasks submitted with execute; 
    for tasks submitted with submit, any thrown exception, checked or not, is considered to be part of the task's return status. 
    If a task submitted with submit terminates with an exception, it is rethrown by Future.get, wrapped in an ExecutionException.

*/

// Listing 7.26 Registering a Shutdown Hook to Stop the Logging service
// Do not add hook to each service, because hook will run concurrently, and for example, if you close
    // logging in the wrong order, it may have exception. A safe way to do is add only one hook for all services
public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            try { 
                LogService.this.stop(); 
            } catch (InterruptedException ignored) {
                // 
            }
        } 
    });
}

// 7.4.2 Daemon Threads
/*
    Difference between daemon thread and normal thread
    Normal threads and daemon threads differ only in what happens when they exit. 
    When a thread exits, the JVM performs an inventory of running threads, and if the only threads that are left are daemon threads, 
    it initiates an orderly shutdown. When the JVM halts, any remaining daemon threads are abandoned ‐ finally blocks are not executed, stacks are not unwound ‐ the JVM just exits.
*/






/*
    Chapter 8 Applying Thread Pool

    Types of task require specific execution policies
    1. Dependent Task, a task is dependent on timing, results or side effect of other thread
    2. Tasks exploit thread confinement: eg, a task require single thread for thread safety, of you have multiple thread, the safety maybe broken
    3. Response‐time‐sensitive tasks. 
    4. Tasks that use ThreadLocal. ThreadLocal is attached to a thread, but not task. e standard Executor implementations may reap idle threads when demand is low and add new ones when demand is high, 
    and also replace a worker thread with a fresh one if an unchecked exception is thrown from a task. ThreadLocal makes sense to use in pool threads only if the thread‐local value has a lifetime that
     is bounded by that of a task; Thread-Local should not be used in pool threads to communicate values between tasks.

    Design Rule 1
    Some tasks have characteristics that require or preclude a specific execution policy. Tasks that depend on other tasks require that the thread pool be large enough that tasks are never queued or rejected; tasks that exploit thread confinement require sequential execution. Document these requirements so that future maintainers do not undermine safety or liveness by substituting an incompatible execution policy.


*/


/*
    8.1.1 Thread Starvation Deadlock
    The second task sits on the work queue until the first task completes, but the first will not complete because it is waiting for the result of the second task.
     The same thing can happen in larger thread pools if all threads are executing tasks that are blocked waiting for other tasks still on the work queue. This is called thread starvation deadlock

     Whenever you submit to an Executor tasks that are not independent, be aware of the possibility of thread starvation deadlock, 
     and document any pool sizing or configuration constraints in the code or configuration file where the Executor is configured.

    Warning on implicit limit, like connection number to a DB 

*/


/* 8.2 Sizing Thread Pools

    Thread pool size should not be an exact number, but by some config mechanism or consulting Runtime.availableProcessors

    For compute intensive, you want N_CPU + 1 threads, +1 for some pagefault and I/O interruption happned, and it can act as substitution

    For more socket and I/O handler, you want more thread because CPU will be idle on handling I/O

    Optimal pool size 

    N_threads = N_CPU * U_CPU * (1 + w/c)
    U_CPU is target CPU utility rate, 0 < U < 1
    w/c is ratio of waiting time to compute time 

    int N_CPUS = Runtime.getRuntime().availableProcessors();

*/



// Listing 8.2 General Constructor for ThreadPoolExecutor
    // 1. When a ThreadPoolExecutor is initially created, the core threads are not started immediately but instead as tasks are submitted, unless you call prestartAllCoreThreads.
    // 2. Developers are sometimes tempted to set the core size to zero so that the worker threads will eventually be torn down and therefore won't prevent the JVM from exiting, but 
    // this can cause some strange‐seeming behavior in thread pools that don't use a SynchronousQueue for their work queue (as newCachedThreadPool does). If the pool is already at the core size, 
    // ThreadPoolExecutor creates a new thread only if the work queue is full. So tasks submitted to a thread pool with a work queue that has any capacity and a core size of zero 
    // will not execute until the queue fills up, which is usually not what is desired. In Java 6, allowCoreThreadTimeOut allows you to request that all pool threads be able to time out; enable this 
    // feature with a core size of zero if you want a bounded thread pool with a bounded work queue but still have all the threads torn down when there is no work to do.
    /*
        relation between corePoolSize and workQueue size
            If the number of threads is less than the corePoolSize, create a new Thread to run a new task.
            If the number of threads is equal (or greater than) the corePoolSize, put the task into the queue.
            If the queue is full, and the number of threads is less than the maxPoolSize, create a new thread to run tasks in.
            If the queue is full, and the number of threads is greater than or equal to maxPoolSize, reject the task.
    */
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) { }
/*
    The newFixedThreadPool factory sets both the core pool size and the maximum pool size to the requested pool size, creating the effect of infinite timeout; the newCachedThreadPool 
    factory sets the maximum pool size to Integer.MAX_VALUE and the core pool size to zero with a timeout of one minute, creating the effect of an infinitely expandable thread pool that will contract again 
    when demand decreases. Other combinations are possible using the explicit ThreadPool-Executor constructor.
*/

/*
    8.3.2 Managing Queued Tasks

    1. bounded queue
    2. unbounded queue
    3. synchronized handoff

    LinkedBlockingQueue: Default for newFixedThreadPool and newSingleThreadExecutor: queue could grow without bound
    Stable: Bounded! use ArrayBlockingQueue or bounded LinkedPriorityQueue

    For very large pool or unbounded pool, use synchronous handoff:
        SynchronousQueue: in order to put an element in a queue, a thread must be available to accept the work
            applicable for unbounded or large threadpool, or rejecting excess tasks is acceptable

    The newCachedThreadPool factory is a good default choice for an Executor, providing better queuing performance than a fixed thread pool， because newCachedThreadPool is implemented SynchronousQueue.
    A fixed size thread pool is a good choice when you need to limit the number of concurrent tasks for resource‐management purposes, as in a server application 
    that accepts requests from network clients and would otherwise be vulnerable to overload.

*/

/*
    8.3.3 Saturation Policy : what to do if queue is full?

    ThreadPoolExecutor.setRejectedExecutionHandler

        1. AbortPolicy: throw the unchecked Rejected-ExecutionException. Discard the newest or oldest?
        2. CallerRunsPolicy: 
            If the queue is full, run the task in the thread that calls the execute(). for webserver, tasks submitted to the main thread will be blocked in the TCP layer, and then if the
            TCP buffer is full, then the rejection would be pushed to client
*/
// List 8.3 Creating a Fixed­sized Thread Pool with a Bounded Queue and the Caller­runs Saturation Policy.
ThreadPoolExecutor executor = new ThreadPoolExecutor(N_THREADS, N_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(CAPACITY)); executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

/*
    Listing 8.4 Using a Semaphore to Throttle Task Submission

    You can set the size of queue, but cannot reject running the execute function in default, so try to use Semaphore to restrict execute() for task submission
*/

@ThreadSafe
public class BoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;
    public BoundedExecutor(Executor exec, int bound) {
        this.exec = exec;
        this.semaphore = new Semaphore(bound);
    }


    public void submitTask(final Runnable command) throws InterruptedException {
        semaphore.acquire();
        try {
            exec.execute(new Runnable() {
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release(); 
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            semaphore.release();
        }
    } 
}

// Listing 8.5 Thread Factory : 
public interface ThreadFactory {
    Thread newThread(Runnable r);
}


/* 8.4 Extending ThreadPoolExecutor
    
    before and after are called in the working thread
    beforeExecute: if this throw RuntimeException, then execute and afterExecute will not called
    afterExecute: run after execute complete normally, or throw exception
    terminated: called when the thread pool completes the shutdown process, after all tasks have finished and all worker threads have shut down
*/ 

// List 8.9 ThreadPool log 
public class TimingThreadPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<Long>();
    private final Logger log = Logger.getLogger("TimingThreadPool");
    private final AtomicLong numTasks = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        log.fine(String.format("Thread %s: start %s", t, r));
        startTime.set(System.nanoTime());
    }
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long endTime = System.nanoTime();
            long taskTime = endTime - startTime.get();
            numTasks.incrementAndGet();
            totalTime.addAndGet(taskTime);
            log.fine(String.format("Thread %s: end %s, time=%dns",
                    t, r, taskTime));
        } finally {
            super.afterExecute(r, t);
        } 
    }
    protected void terminated() {
        try {
            log.info(String.format("Terminated: avg time=%dns",
                    totalTime.get() / numTasks.get()));
        } finally {
            super.terminated();
        } 
    }
}


// 8.5 Parallelizing Recursive Algorithm
// Listing 8.10 Transform Independent Loop to Parallel Execution
void processSequentially(List<Element> elements) {
    for (Element e : elements)
        process(e);
    }

void processInParallel(Executor exec, List<Element> elements) {
    for (final Element e : elements)
        exec.execute(new Runnable() {
            public void run() { process(e); }
    }); 
}







































