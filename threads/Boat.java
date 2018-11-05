package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.threads.KThread;
import nachos.threads.Condition2;
import nachos.threads.Lock;
import nachos.machine.Lib;
//oahu ->Malokai

import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***");
		// begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);

		// System.out.println("\n ***Testing Boats with 20 children, 10 adults***");
		// begin(10, 20, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		Lib.assertTrue(children >= 2);
		Lib.assertTrue(b != null);

		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		oAdult = adults;
		oChild = children;
		inBoat = 0;
		boatOnOahu = true;
		commonLock = new Lock();
		adultsOahu = new Condition2(commonLock);
		adultsMolokai = new Condition2(commonLock);
		childrenOahu = new Condition2(commonLock);
		childrenMolokai = new Condition2(commonLock);
		done = new Semaphore(0);

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		for (int i = 0; i < adults; i++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Adult " + i + "Thread");
			t.fork();
		}

		for (int i = 0; i < children; i++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Child " + i + "Thread");
			t.fork();
		}

		done.P();

		System.out.println(" ***Boat Test ends!***");
	}

	static void AdultItinerary() {
		boolean onOahu = true;
		commonLock.acquire();
		while (true) {
			Lib.assertTrue(onOahu);
			if (inBoat == 0 && boatOnOahu && oChild <= 1) {
				onOahu = false;
				oAdult--;
				boatOnOahu = false;
				bg.AdultRowToMolokai();
				if (oAdult == 0 && oChild == 0) {
					done.V();
					adultsMolokai.sleep();
				}
				childrenMolokai.wakeAll();
				adultsMolokai.sleep();
			}
			else
				adultsOahu.sleep();
		}
	}

	static void ChildItinerary() {
		boolean onOahu = true;
		commonLock.acquire();
		while (true)
			if (onOahu) {
				if (boatOnOahu && inBoat == 0) {
					onOahu = false;
					oChild--;
					bg.ChildRowToMolokai();
					if (oChild > 0) {
						inBoat = 1;
						childrenOahu.wakeAll();
					}
					else {
						boatOnOahu = false;
						inBoat = 0;
						if (oAdult == 0 && oChild == 0) {
							done.V();
							childrenMolokai.sleep();
						}
						childrenMolokai.wakeAll();
					}
					childrenMolokai.sleep();
				}
				else if (boatOnOahu && inBoat == 1) {
					onOahu = false;
					oChild--;
					bg.ChildRideToMolokai();
					boatOnOahu = false;
					inBoat = 0;
					if (oAdult == 0 && oChild == 0) {
						done.V();
						childrenMolokai.sleep();
					}
					childrenMolokai.wakeAll();
					childrenMolokai.sleep();
				}
				else
					childrenOahu.sleep();
			}
			else {
				if (!boatOnOahu) {
					bg.ChildRowToOahu();
					oChild++;
					boatOnOahu = true;
					adultsOahu.wakeAll();
					childrenOahu.wakeAll();
					onOahu = true;
					childrenOahu.sleep();
				}
				else
					childrenMolokai.sleep();
			}
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out
				.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

	static int oAdult, oChild;
	static boolean boatOnOahu;
	static int inBoat;
	static Lock commonLock;
	static Condition2 adultsOahu, adultsMolokai;
	static Condition2 childrenOahu, childrenMolokai;
	static Semaphore done;
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
