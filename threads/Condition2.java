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
    

}
