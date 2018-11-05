package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
	
	ThreadQueue heldThreads;
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
    	this.conditionLock = conditionLock;
    	this.heldThreads = ThreadedKernel.scheduler.newThreadQueue(false);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	//Stores interrupt state
    	boolean interruptState = Machine.interrupt().disable();
    	
    	//Adds the thread to list of sleeping threads
		heldThreads.waitForAccess(KThread.currentThread());

		conditionLock.release();
		KThread.sleep();
		//System.out.println("Sleeping thread " + KThread.currentThread().getName());
		conditionLock.acquire();
		
		//Restore interrupt state
		Machine.interrupt().restore(interruptState);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	//Stores interrupt state
    	boolean interruptState = Machine.interrupt().disable();
    	int i = 0;
    	KThread thread = heldThreads.nextThread();
		if (thread != null) {
			//System.out.println("Waking thread " + KThread.currentThread().getName());
			thread.ready();
		}
		
		//Restore interrupt state
		Machine.interrupt().restore(interruptState);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	//Stores interrupt state
    	boolean interruptState = Machine.interrupt().disable();
    	//Get the thread
    	KThread thread = heldThreads.nextThread();
    	//Iterate through queue
    	while (thread != null) {
    		thread.ready();
    		thread = heldThreads.nextThread();
    	}
	
		//Restore interrupt state
		Machine.interrupt().restore(interruptState);
    }

    private Lock conditionLock;
    
	private static class PingTest implements Runnable {
		PingTest(int which, Condition2 cond, Lock lock) {
			this.which = which;
			this.cond = cond;
			this.lock = lock;
		}

		public void run() {
			lock.acquire();
			if (this.which < 3) {
				numWaiting++;
				cond.sleep();
			} else {
				while (numWaiting > 0) {
//					cond.wake();
//					numWaiting--;
					cond.wakeAll();
					numWaiting = 0;
				}
			}
			lock.release();
		}
		private int which;
		private Condition2 cond;
		private Lock lock;
		private static int numWaiting = 0;
		private static boolean wakeAll = false;
	}

	// Test stuff below
	public static void selfTest() {

		System.out.println("\n Entering Condition2.selfTest()");
		Lock lock = new Lock();
		Condition2 conditionVar = new Condition2(lock);


		System.out.println("\n***Testing sleep and wake***");
		for (int i = 0; i < 3; i++) {
			KThread newThread = new KThread(new PingTest(i+1,conditionVar,lock));
			newThread.setName("" + i);
			newThread.fork();
		}
		new PingTest(0,conditionVar, lock).run();


		System.out.println("\n Finished testing Condition2.java");

	}
}
