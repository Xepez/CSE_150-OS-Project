package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.threads.KThread;
import nachos.threads.Condition2;
import nachos.threads.Lock;

public class Boat{
	static Lock Boat;
	static int numOfChildren;
	static int numOfAdults;
	static int BoatLocation;
	static boolean allOver;
	static Condition2 BoatFull;
	static Condition2 childMolokai;
	static Condition2 childOahu;
	static Condition2 adultMolokai;
	static Condition2 adultOahu;
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
		BoatFull = new Condition2(Boat);
		childMolokai = new Condition2(Boat);
		childOahu = new Condition2(Boat);
		adultMolokai = new Condition2(Boat);
		adultOahu = new Condition2(Boat);

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
		if(children==0 && adults == 0){
			allOver = true;
		}

	}
	static void AdultItinerary(){

		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */

	}
	static void ChildItinerary(){
		//to implement
		
		//			2 adults/ 2 children, 2 children 1 ride/1 row to Molokai  
		
		//			2adults Oahu, 0 children Oahu, 2 children Molakai
		
		//			1 stays on Molokai, the other RowstoOahu  
		
		//			1 child Molakai, 1 child Oahu, 2 adults Oahu
		
		// 			1 adult rows to Molokai     
		
		//			1 adult Molakai ,  1 adult Oahu, 1 child Oahu, 1 child Molakai
		
		//			1 child on Molokai rows back to Oahu  
		
		//			1 adult Molakai, 1 adult Oahu, 2 children Oahu
		
		//			1 child rows to Molokai, other child rides to Molokai
		
		//			1 adult Molakai, 1 adult Oahu, 2 children Molokai
		
		//			1 child rows back to Oahu
		
		//			1 adult 1 child Molakai, 1 adult 1 child Oahu
		
		//			1 adult rows/rides to Molakai
		
		//			2 adults 1 child Molakai, 1 child Oahu
		
		//			1 child on Molokai rows back to Oahu
		
		//			2 adults Molakai, 2 children Oahu
		
		//			1 child rows to Molokai, other child rides back to Molakai
		
		//			2 adults, 2 children Molakai
		
		//			All child/adults should now be on Molakai.
		
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
