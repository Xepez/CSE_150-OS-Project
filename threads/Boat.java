package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.threads.KThread;
import nachos.threads.Condition2;
import nachos.threads.Lock;
import nachos.machine.Lib;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;
public class Boat {
	//Global Variables
	static int oAdult, oChild;
	static boolean boatOnOahu;
	static int inBoat;
	static Lock genLock;
	static Condition2 adultsOahu, adultsMolokai;
	static Condition2 childrenOahu, childrenMolokai;
	static Semaphore done;
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();
		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		Lib.assertTrue(children >= 2);
		Lib.assertTrue(b != null);
		bg = b;
		
		//init global variables
		oAdult = adults;	//Oahu adult count
		oChild = children;	//Oahu children count
		inBoat = 0;			//# people in boat
		boatOnOahu = true;
		genLock = new Lock();
		adultsOahu = new Condition2(genLock);
		childrenOahu = new Condition2(genLock);
		adultsMolokai = new Condition2(genLock);
		childrenMolokai = new Condition2(genLock);
		done = new Semaphore(0);
		for (int i = 0; i < adults; i++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread thr = new KThread(r);
			thr.setName("Adult " + i + " Thread");
			thr.fork();
		}

		for (int i = 0; i < oChild; i++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread thr = new KThread(r);
			thr.setName("Child " + i + " Thread");
			thr.fork();
		}

		done.P();

		System.out.println("Boat test over");
	}

	static void AdultItinerary() {
		boolean onOahu = true;
		genLock.acquire();
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
		genLock.acquire();
		while(true)
			if(onOahu) {//if Oahu
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
					oChild--;
					onOahu = false;
					bg.ChildRideToMolokai();
					inBoat = 0;
					boatOnOahu = false;
					if (oAdult == 0 && oChild == 0) {
						done.V();
						childrenMolokai.sleep();
					}
				}
				else
					childrenOahu.sleep();
			}
			else {//if Molokai
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
		// Not a valid solution
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
}
