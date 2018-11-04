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

	LinkedList<KThread> heldThreads;
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
		this.heldThreads = new LinkedList<KThread>();
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
		heldThreads.add(KThread.currentThread());

		conditionLock.release();
		KThread.sleep();
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

		if (!heldThreads.isEmpty()) {
			KThread thread = heldThreads.removeFirst();
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

		KThread thread = heldThreads.removeFirst();
		while (thread != null){
			thread.ready();
			thread = heldThreads.removeFirst();
		}

		//Restore interrupt state
		Machine.interrupt().restore(interruptState);
	}

	private Lock conditionLock;
}
