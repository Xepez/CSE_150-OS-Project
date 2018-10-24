package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { 
				timerInterrupt(); 
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {
		boolean status = Machine.interrupt().disable();
		while(!waitQueue.isEmpty()) {	//while there is a thread in wait queue
			if(waitQueue.peek().wakeTime <= Machine.timer().getTime()) { //check if it is time to wake up thread
				waitQueue.poll().thread.ready();//wake
			}
			else {
				break;	//end iteration
			}
		}
		Machine.interrupt().restore(status);
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		boolean status = Machine.interrupt().disable();
		
		long wakeTime = Machine.timer().getTime() + x;	//current time + wait time = wake time
		waitQueue.add(new Waiter(KThread.currentThread(),wakeTime));
		KThread.sleep();
		Machine.interrupt().restore(status);;
		while (wakeTime > Machine.timer().getTime())	//While it isn't it's time to wake up, yield.
			KThread.yield();
	}
	private PriorityQueue<Waiter> waitQueue = new PriorityQueue<Waiter>();	//This classes' wait queue
																			//Queue updated in Waiter class
	// TASK 1.3
	private class Waiter implements Comparable<Waiter> {
		private KThread thread;
		private long wakeTime;

		Waiter(KThread thread, long wakeTime) { //update thread and waketime
			this.thread = thread;
			this.wakeTime = wakeTime;
		}

		@Override
		public int compareTo(Waiter that) {
			return Long.signum(wakeTime - that.wakeTime);	//Long.signum() returns -1 if negative...
		}													//0 if value is equal and 1 if value is positive
	}
}
