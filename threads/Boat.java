package nachos.threads;
import java.lang.management.LockInfo;

import nachos.ag.BoatGrader;

public class Boat
{
	static BoatGrader bg;
	private static Lock Boat;
	private static Lock Increment;

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  	begin(1, 2, b);

		//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  	begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		int Oahu,Molokai,boatLocation = 0;
		int AdultRowToMolokai,AdultRideToOahu = 2;
		int ChildRideToMolokai,ChildRowToMolokai = 1;
		int ChildMolokai,ChildOahu = 1;
		int AdultMolokai,AdultOahu = 2;
		int passengers = 0;

		// Instantiate global variables here
		this.AdultRowToMolokai = passengers;
		this.AdultRowToOahu = passengers;
		this.ChildRideToMolokai = child;
		this.ChildRideToOahu = child;
		this.ChildRowToMolokai = child;
		this.ChildRowToOahu = child;
		int remainChild =  passengers;

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.


		Boat = new Lock();
		Increment = new Lock();
		Wait = new Lock();
		Sleep = new Lock();

		/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/

		for(int i =0; i < child; i++){
			Kthread kt = new Kthread(new Runnable(){
				public void run(){
					childCrossed();
				}});
			thread.setName("Child Crossed" + i); 
			thread.forkOff();
		}
		// loop and thread/ runnables for adult
		for(int i =0; i < passengers; i++){
			Kthread kt = new Kthread(new Runnable(){
				public void run(){
					adultCrossed();
				}});
			thread.setName("Adult Crossed" + i); 
			thread.forkOff();

		}
		return;

	}

	protected static void adultCrossed() {
		// TODO Auto-generated method stub

	}

	static void AdultItinerary(int remainChild)
	{
		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */

		public void AdultRowToMolokai();
		bg.AdultRowToMolokai();
		if (adult => 0){
			AdultRowToMolokai = true;
			Molokai.aquire(); 
			bog.adultCrossed();
			passengers = adult -1;
			boatLocation = Lock.wait;
			new Increment = passengers;
			system.out.println(" Adult is riding to Molokai");
			else{
				AdultRowToMolokai = false;
				passengers = adult;
				boatLocation = lock.sleep;
			}
		}

		public void AdultRowToOahu();
		bg.AdultRowToOahu();
		if(adult => 0){
			AdultRowToOahu = false;
			boatLocation = lock.sleep;
			//Since an adult takes up 2 seats, an adult should never be piloting back to Oahu.
		}

		public void ChildRowToMolokai();
		bog.ChildRowToMolokai();
		if (child < remainChild){
			ChildRowToMolokai = true;
			Molokai.aquire();
			bg.childCrossed();
			passenger = child -1;
			boatLocation = Lock.wait;
			new Increment = passengers;
			system.out.println(" Child is piloting to Molokai");
			else{
				ChildRowToMolokai = false;
				passengers = remainChild;
				boatLocation = lock.sleep;
			}
		}

		public void ChildRideToMolokai();
		bg.ChildRideToMolokai();
		if (child > remainChild){
			ChildRideToMolokai = true;
			Molokai.aquire();
			bg.childCrossed();
			passenger = child -1;
			boatLocation = Lock.wait;
			new Increment = passengers;
			system.out.println(" Child is riding to Molokai");
			else{
				ChildRideToMolokai = false;
				passengers = remainChild;
				boatLocation = lock.sleep;
			}
		}

		public void ChildRowToOahu();
		bog.ChildRowToOahu();
		if (child =< remainChild){
			ChildRowToOahu = true;
			Oahu.aquire();
			bg.childCrossed();
			passenger = child -1;
			boatLocation = Lock.wait;
			new Increment = passengers;
			system.out.println(" Child is piloting back to Oahu");
			else{
				ChildRowToOahu = false;
				passengers = remainChild;
				boatLocation = lock.sleep;
			}
		}
		public void ChildRideToOahu();
		bog.ChildRideToOahu();
		if (child => remainChild){
			ChildRowToOahu = true;
			Oahu.aquire();
			bg.childCrossed();
			passenger = child -1;
			boatLocation = Lock.wait;
			new Increment = passengers;
			system.out.println(" Child is riding back to Oahu");
			else{
				ChildRideToOahu = false;
				passengers = remainChild;
				boatLocation = lock.sleep;
			}}
	}

	private static void ChildRideToMolokai() {
		// TODO Auto-generated method stub

	}

	static void ChildItinerary()
	{
	}

	static void SampleItinerary()
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

}
