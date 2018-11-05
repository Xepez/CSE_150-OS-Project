package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

public class Alarm {

    public Alarm() {
	     Machine.timer().setInterruptHandler(new Runnable() {
		       public void run() { timerInterrupt(); }
	    });
    }

    public void timerInterrupt() {

      long currentTime = 0;
      boolean status = Machine.interrupt().disable();

      while(!waitQueue.isEmpty()) {	//while there is a thread in wait queue
        currentTime = Machine.timer().getTime();

        if(waitQueue.peek().wakeTime <= currentTime) { //check if it is time to wake up thread
          waitQueue.poll().newThread.ready();//wake
        }

        else if (waitQueue.peek().wakeTime > currentTime) {
          break;	//end iteration
        }
      }

      Machine.interrupt().restore(status);
      KThread.yield();
    }

    public void waitUntil(long waitTime) {

       long currTime = Machine.timer().getTime();
       long wakeTime = currTime + waitTime;	//current time + wait time = wake time
       boolean status = Machine.interrupt().disable();

       ThreadsWaiting waitingThread = new ThreadsWaiting(wakeTime, KThread.currentThread());

       waitQueue.add(waitingThread);
       KThread.sleep();
       Machine.interrupt().restore(status);

       //While it isn't it's time to wake up, yield.
       while (wakeTime > Machine.timer().getTime()) {
         KThread.yield();
       }	//While it isn't it's time to wake up, yield.
     }

     private PriorityQueue<ThreadsWaiting> waitQueue = new PriorityQueue<ThreadsWaiting>();	//This classes' wait queue

     private class ThreadsWaiting implements Comparable<ThreadsWaiting> {
       private long wakeTime;
       private KThread newThread;

       ThreadsWaiting(long wakeTime, KThread newThread) { //update thread and waketime
         this.wakeTime = wakeTime;
         this.newThread = newThread;
       }

      public int compareTo(ThreadsWaiting thread) {
        long currTime = this.wakeTime;
        long newTime = thread.wakeTime;

        int num = Long.signum(currTime - newTime);

        return num;

      }
    }
}
