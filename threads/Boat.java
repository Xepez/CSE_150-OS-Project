package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.threads.KThread;
import nachos.threads.Condition2;
import nachos.threads.Lock;

public class Boat{
	static Lock Boat;
	static int numChildren;
	static int numAdult;
	static int boatLocation;//0 = Oahu, 1 = Molokai
	static int inBoat;
	static Lock boatLock; //Using boat
    static Lock inBoatLock; //Using boat
	static Condition2 pInBoat ; //People in boat
    static Condition2 mChild ; //Groups children at Molokai
    static Condition2 mAdult ; //Groups adults at Molokai
    static Condition2 oChild ; //Groups children at Oahu
    static Condition2 oAdult ; //Groups adults at Oahu
	static BoatGrader bg;
	static boolean done; //True when everything is finished
	///
        	 
    static boolean locker;
	public static void selfTest(){
		BoatGrader b = new BoatGrader();
		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);
		//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}
	public static void begin( int adults, int children, BoatGrader b ){
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		BoatGrader bg = b;

		// Instantiate global variables here
		int Oahu = 0;
		int Molakai = 0;
		int boatO = 0;
		int boatM = 1;
		int ChildMolokai,ChildOahu = children;
		int AdultMolokai,AdultOahu = adults;
		int passengers = 0;
		Boat = new Lock();
		pInBoat = new Condition2(Boat);
		mChild = new Condition2(Boat);
		oChild = new Condition2(Boat);
		mAdult = new Condition2(Boat);
		oAdult = new Condition2(Boat);

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.


		/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/

		for(int i =0; i < children; i++){
			KThread t = new KThread(new Runnable(){
				public void run(){
					ChildItinerary();
				}});
			t.setName("Child Crossed" + i); 
			t.fork();
		}

		for(int i =0; i < adults; i++){
			KThread t = new KThread(new Runnable(){
				public void run(){
					AdultItinerary();
				}});
			t.setName("Adult Crossed" + i); 
			t.fork();
		}
		if(children<2 && adults == 0){
			done = true;
		}

	}
	static void AdultItinerary(){
		/* This is where you should put your solutions. Make calls
		   to the BoatGrader to show that it is synchronized. For
		   example:
		       bg.AdultRowToMolokai();
		   indicates that an adult has rowed the boat across to Molokai
		*/

		boatLock.acquire();
		while( (numChildren > 1) || (boatLocation == 1) ) {		
			oAdult.sleep();
		}	
		bg.AdultRowToMolokai();
		numAdult--;
		boatLocation = 1;
		mChild.wake();			
		boatLock.release();
	}
	static void ChildItinerary(){
		int currentLocation = 0;	
		boolean waitingFor = false;
		while(!done){
			boatLock.acquire();
			waitingFor = false;	
			while(currentLocation != boatLocation || inBoat == 2) { //wait for the boat
				if(currentLocation == 0) {			
					mChild.wake();				
					oChild.sleep();
				} else {
					oChild.wake();
					mChild.sleep();
				}
			}
			if(currentLocation == 0) {
				if(numChildren > 1) { 
					while(inBoat != 2 && locker) {
						if(waitingFor) {//Wait for two	
							oChild.sleep();
						} else {
							waitingFor = true;
							inBoat++;
							numChildren--;
						}
					}

					
					if(inBoat == 2) {
						bg.ChildRowToMolokai();
						inBoat--;
						locker = false;
						boatLocation = 1;
						oChild.wake();
					} else {
						bg.ChildRideToMolokai();
						numChildren--;
						numChildren--;
						inBoat--;
						boatLocation = 1;
						locker = true;
						mChild.wake();
					}
					currentLocation = 1;
					boatLock.release();
					KThread.yield(); //Finish
				} else if (numAdult == 0 && numChildren == 1) {
					bg.ChildRowToMolokai();
					numChildren--;
					boatLocation = 1;
					done = true;
					currentLocation = 1;
					boatLock.release();
					KThread.yield(); //Finish
				} else { 
					mChild.wake();			
					boatLock.release();
					KThread.yield(); //Finish

				}			
			} else {
				numChildren++;
				boatLocation = 0;
				currentLocation = 0;
				bg.ChildRowToOahu();
				oChild.wake();
				oAdult.wake();
				boatLock.release();			
				KThread.yield(); //Finish
			}
		}
		System.out.println("Todo termino");
	}
}

/*static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
}*/
