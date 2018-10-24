package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	
	//Monitor
	Lock mutex;
	Condition2 readyToListen;
	Condition2 readyToSpeak;
	int numListeners;
	int numSpeakers;
	Integer message;
	
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	//Initialize monitor
    	mutex = new Lock();
    	readyToListen = new Condition2(mutex);
    	readyToSpeak = new Condition2(mutex);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	mutex.acquire();
    	numSpeakers++;
    	
    	while (numListeners == 0 && message != null) { //Ensures there's at least one listener and not another speaker speaking
    		readyToSpeak.sleep();
    	}
    	message = word;
    	
    	readyToListen.wake();
    	
    	numSpeakers--;
    	mutex.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	mutex.acquire();
    	numListeners++;
    	while (message == null){
    		readyToListen.sleep();
    	}
    	
    	int word = message.intValue();
    	message = null;
    	readyToSpeak.wake();
    	
	numListeners--;
    	mutex.release();
    	return word;
    }
}


