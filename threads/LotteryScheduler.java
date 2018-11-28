package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer tickets from waiting threads
	 *					to the owning thread.
	 * @return	a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new lotteryQueue(transferPriority);
	}

	// New Priority Min
	public static final int priorityMinimum = 1;
	// New Priority Max
	public static final int priorityMaximum = Integer.MAX_VALUE;

	@Override
	protected lotteryThreadState getThreadState(KThread thread) {
		// Gets Thread State mainly used to change from ThreadState to lotteryThreadState
		if (thread.schedulingState == null)
			thread.schedulingState = new lotteryThreadState(thread);

		return (lotteryThreadState) thread.schedulingState;
	}
	
	protected class lotteryQueue extends PriorityQueue {

		lotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}

		@Override
		protected ThreadState pickNextThread() {

			//  Sum of all tickets
			int sum = 0;

			/*
			 * The random function below (Thanks StackOverflow)
			 * - Finds all the tickets that every thread has
			 * - Then picks a random number from 0 to that max ticket amount
			 * - It will then keep checking each thread that donates subtracting from our random number
			 * - And pick the thread when the random number is between the previous thread's max tickets and the current thread's max tickets
			 */

			// Gets the sum of all the threads
			for (ThreadState checkThread : waitQueue) {
				sum += checkThread.getEffectivePriority();

				if (sum > priorityMaximum)
					sum = priorityMaximum;
			}

			// Picks a random number
			Random r = new Random();
			int random = r.nextInt(sum);

			for (ThreadState checkThread : waitQueue) {
				// Our current effective priority (Amount of tickets)
				int currEff = checkThread.getEffectivePriority();
				// If we are at our lottery priority
				if (random < currEff)
					return checkThread;
				// If not keep going with a better chance
				random -= currEff;
			}

			return null;
		}
	}

	protected class lotteryThreadState extends ThreadState{

		public lotteryThreadState(KThread thread) {
			super(thread);
			setPriority(priorityDefault);
			effectivePriority = priorityDefault;
		}

		@Override
		public int getEffectivePriority() {
			// Our effective priority should never be lower than our min priority
			effectivePriority = priority;

			// Checks each donation that this thread state has gotten
			for (PriorityQueue checkQueue : donateQueue) {
				// Checks if the queue we are looking at allows donations
				if (checkQueue.transferPriority) {
					// If donations are allowed goes through the queue and adds up all the tickets
					for (ThreadState checkState : checkQueue.waitQueue) {
						// Adds all our tickets together
						effectivePriority += getThreadState(checkState.thread).getEffectivePriority();
					}
				}
			}
			return effectivePriority;
		}
	}
}
