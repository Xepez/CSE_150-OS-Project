package nachos.threads;

import nachos.machine.*;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion (lol) to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}
		
		public void addToQueue(ThreadState state) {
			// Adds to our wait queue
			this.waitQueue.add(state);
			// Refreshs our effective priority
			this.lockThread.getEffectivePriority();
		}
		
		public void removeFromQueue(ThreadState state) {
			// Removes from our wait queue
			this.waitQueue.remove(state);
			// Sets our new lock to inputed thread state
			this.lockThread = state;
			// Adds this to the states donation queue
			state.donateQueue.add(this);
			// Refreshs our effective priority
			state.getEffectivePriority();
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			// Initial check of wait queue to see we need to keep going or not
			if(waitQueue.isEmpty())
				return null;

			// Checks if there is a thread with a lock
			if (this.lockThread != null) {
				// Removes this from the lock threads donation queue since no longer needed
				this.lockThread.donateQueue.remove(this);
				// Need to refresh effective priority
				lockThread.getEffectivePriority();
			}

			// Find next Thread 
			ThreadState nextThread = pickNextThread();

			// If we didn't get a thread from the pickNextThread fcn
			if (nextThread == null) {
				return null;
			}
			
			// Acquire our new thread
			nextThread.acquire(this);
			// Return our ThreadState thread
			return nextThread.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {

			// Our Next Thread
			ThreadState pickedThread = null;
			// Priority of the Current Thread
			int checkPriority = -999;
			// Max Threads Priority
			int maxPriority = -999;

			// Checks each thread state in our wait Queue
			for (ThreadState checkThread : waitQueue) {
				// Our current thread priority we are checking
				checkPriority = checkThread.effectivePriority;		// Maybe check if eff priority not working
				//checkPriority = checkThread.getEffectivePriority();
				
				/* Checking if the picked thread hasn't been picked yet or 
				 * if the our current max priority is smaller then our checked thread.
				 * 
				 * Also ensures we pick the thread thats been the the queue the longest
				 */
				if ((checkPriority > maxPriority) || pickedThread == null) {
					// Sets new max priority & our new priority
					maxPriority = checkPriority;
					pickedThread = checkThread;
				}				
			}

			return pickedThread;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			// no
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		// Tracks threads with a lock
		ThreadState lockThread = null;
		// Linked List that store all the waiting threads in it
		protected LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		protected int effectivePriority;
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
			effectivePriority = priorityDefault;
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// Our effective priority should never be lower than our priority
			effectivePriority = priority;
			// Temp variable to check the current thread we are checking priority
			int checkEffPriority = -999;
			
			// Checks each donation that this thread state has gotten
 			for (PriorityQueue checkQueue : donateQueue) {
 				// Checks if the queue we are looking at allows donations
				if (checkQueue.transferPriority) {
					/*
					 *  If donations are allowed goes through the queue to find the thread
					 *  with the highest priority
					 */
					for (ThreadState checkState : checkQueue.waitQueue) {
						/*
						 *  Our current thread effective priority we are checking
						 *  
						 *  We check effective priority since threads can pass their priorities up a chain
						 *  like if one thread is waiting for one thread that is waiting for one thread
						 *  we need to pass that priority up to ensure that we can go back through that chain
						 */
						checkEffPriority = checkState.effectivePriority;
						/*
						 *  If the new thread has a larger priority
						 *  Set that priority to our current thread effective priority
						 */
						if (checkEffPriority > effectivePriority)
							effectivePriority = checkEffPriority;
					}
				}
			}
 			
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// Refreshs effective priority
			getEffectivePriority();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {

			// If there exists a thread with a lock
			if (waitQueue.lockThread != null) {
				// Add this ThreadState to the wait queue
				waitQueue.addToQueue(this);
			}
			else {
				waitQueue.waitQueue.add(this);
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// Remove this ThreadState from the wait queue to get ready
			waitQueue.removeFromQueue(this);
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		// Donation Queue that tracks donations from each thread
		protected LinkedList<PriorityQueue> donateQueue = new LinkedList<PriorityQueue>();
	}
}