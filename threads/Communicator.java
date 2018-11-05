package nachos.threads;

import java.util.Collections;
import java.util.LinkedList;

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
	boolean isSomeoneSpeaking;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		//Initialize monitor
		mutex = new Lock();
		readyToListen = new Condition2(mutex);
		readyToSpeak = new Condition2(mutex);
		isSomeoneSpeaking = false;
		message = null;
		numListeners = 0;
		numSpeakers = 0;
	}
	
	public void speak(int word) {
		if(!mutex.isHeldByCurrentThread()){ mutex.acquire(); }
		++numSpeakers;
            while((numListeners == 0) || isSomeoneSpeaking){

                readyToSpeak.sleep();

             }

		
		
		message = new Integer(word);
                isSomeoneSpeaking = true;   // Going to be placing a Messgae
                readyToListen.wake();  //Wake any(the) sleeping listners
		--numSpeakers;
                mutex.release();
	}
	
    public int listen()
	{
		if(!mutex.isHeldByCurrentThread()){ mutex.acquire(); }
		readyToSpeak.wakeAll();
		++numListeners;

		while(!isSomeoneSpeaking){ readyToListen.sleep(); }      // No message has been spoken, listners wait.
			

		int word = message.intValue();        //Recieve the message and update the inbox
		isSomeoneSpeaking = false;
		--numListeners;

		readyToSpeak.wakeAll();    //Remind any other speakers to check if there are additional listeners
		mutex.release();
		return word;
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
	/*
	public void speak(int word) {
		mutex.acquire();
		numSpeakers++;

		while (message != null || numListeners == 0) { //Ensures there's at least one listener and not another speaker speaking
			readyToListen.wake();
			readyToSpeak.sleep();
		}
		numSpeakers--;
		message = new Integer(word);

		readyToListen.wakeAll();

		
		mutex.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	/*
	public int listen() {
		mutex.acquire();
		numListeners++;
		
		
		while (message == null){
			readyToSpeak.wake();
			readyToListen.sleep();
		}
		
		numListeners--;
		int word = message.intValue();
		message = null;
		
		
		mutex.release();
		return word;
	}
	*/
	
	//Tester method
	public static void selfTest()
	{
        System.out.println("\n**Communicator test**");
        Communicator test = new Communicator();
        
        // ** Test One **
        System.out.println("[First test: Two threads, one speaker one listener, order of speaker->listener]");
        
        KThread speakOne = new KThread(new Speaker(test, 5));
        speakOne.setName("S1");
        speakOne.fork();
        
        System.out.println("Listener should hear 5");
        KThread listenOne = new KThread(new Listener(test));
        listenOne.setName("L1"); 
        listenOne.fork();
        //speakOne.join();
        listenOne.join(); //Execute thread
        
        
        // ** Test Two **
        System.out.println("\n[Second test: Two threads, one speaker one listener, order of listener ->speaker]");
        KThread listenTwo = new KThread(new Listener(test));
        listenTwo.setName("L2");
        listenTwo.fork();
        
        System.out.println("Listener should hear 37");
        KThread speakTwo = new KThread(new Speaker(test, 37));
        speakTwo.setName("S2");
        speakTwo.fork();
        speakTwo.join();
        listenTwo.join();
        


        // ** Test Three **
        System.out.println("\n[Third test: Four threads, three speakers one listener, order of speaker*3->listener]");
        test = new Communicator();
        KThread speakThree = new KThread(new Speaker(test, 50));
        speakThree.setName("S3");
        
        KThread speakFour = new KThread(new Speaker(test, 19));
        speakFour.setName("S4");
        
        KThread speakFive = new KThread(new Speaker(test, 8));
        speakFive.setName("S5");
        
        KThread scream = new KThread(new Speaker(test, 666));
        scream.setName("Scream");
        
        speakThree.fork();
        speakFour.fork();
        speakFive.fork();
        scream.fork();
        
        System.out.println("Listener should hear 50");
        KThread listenThree = new KThread(new Listener(test));
        listenThree.setName("L3");
        listenThree.fork();
        listenThree.join();
        
        // ** Test Four **
        System.out.println("\n[Fourth test: Six threads, three speakers three listeners, order of listener*3->speaker*3]");
        test = new Communicator();
        KThread listenFour = new KThread(new Listener(test));
        listenFour.setName("L4");
        
        KThread listenFive = new KThread(new Listener(test));
        listenFive.setName("L5");
        
        KThread listenSix = new KThread(new Listener(test));
        listenSix.setName("L6");

        KThread speakSix = new KThread(new Speaker(test, 82));
        speakSix.setName("S6");
        
        KThread speakSev = new KThread(new Speaker(test, 99));
        speakSev.setName("S7");
        
        KThread speakEight = new KThread(new Speaker(test, 111));
        speakEight.setName("S8");
        
        listenFour.fork();
        listenFive.fork();
        listenSix.fork();
        System.out.println("Listener should hear 82");
        speakSix.fork();
        speakSix.join();
        System.out.println("Listener should hear 99");
        speakSev.fork();
        speakSev.join();
        System.out.println("Listener should hear 111");
        speakEight.fork();
        speakEight.join();

        // ** Test 5 **
        System.out.println("\n[Fifth test: Four threads, 1 speaker 3 listeners, order of listener*3->speaker]");
        test = new Communicator();
        KThread SL9 = new KThread(new Listener(test));
        SL9.setName("SL9");

        KThread listenSev = new KThread(new Listener(test));
        listenSev.setName("L7");

        KThread listenEight = new KThread(new Listener(test));
        listenEight.setName("L8");

        KThread speakTen = new KThread(new Speaker(test, 7));
        speakTen.setName("S10");

        SL9.fork();
        listenSev.fork();
        listenEight.fork();
        System.out.println("Listener should hear 7");
        speakTen.fork();
        speakTen.join();    
        

        // ** Test 6 **
        System.out.println("\n[Sixth test: Eight threads, 4 speakers and four listeners but forked in a random order, ran for 5 iterations]");

        //for (int i = 0; i < 5; i++){
        	//System.out.println("\nIteration " + i);
        	test = new Communicator();
        	KThread S61 = new KThread(new Speaker(test, 55));
        	KThread S62 = new KThread(new Speaker(test, 158));
        	KThread S63 = new KThread(new Speaker(test, 172));
        	KThread S64 = new KThread(new Speaker(test, 211));
        	KThread L61 = new KThread(new Listener(test));
        	KThread L62 = new KThread(new Listener(test));
        	KThread L63 = new KThread(new Listener(test));
        	KThread L64 = new KThread(new Listener(test));

        	LinkedList<KThread> list = new LinkedList<KThread>();
        	list.add(S61);
        	list.add(S62);   
        	list.add(S63);
        	list.add(S64);
        	list.add(L61);
        	list.add(L62);
        	list.add(L63);
        	list.add(L64);
		Collections.shuffle(list);
		while (list.peek() != null){
			list.removeFirst().fork();
		}     	

        //}
        System.out.println("\nFinished Communicator testing");

        }
 
	//Below classes are internal classes so as not to have to mess with any external files
	//Test Speaker, holds a reference to the communicator and the message it's saying
	private static class Speaker implements Runnable
	{
		int msg;
		Communicator communicator;

		public Speaker(Communicator com, int word)
		{
			msg = word;
			communicator = com;
		}

		public void run()
		{
			communicator.speak(msg);
            System.out.println("Said: " + msg);
		}
	}

	//Test Listener, receives the message and outputs it
	private static class Listener implements Runnable
	{
		int msg;
		Communicator communicator;

		public Listener(Communicator comm)
		{
			msg = 0;
			communicator = comm;
		}

		public void run()
		{
            msg = communicator.listen();
			System.out.println("Heard: " + msg);
		}
	}

	
}


