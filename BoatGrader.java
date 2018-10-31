package nachos.ag;

import nachos.threads.Boat;
import nachos.threads.KThread;
import nachos.machine.Lib;


public class BoatGrader extends BoatGrader();{

public BoatGrader();

    
    int Oahu,Molokai,boatLocation = 0;
    int AdultRowToMolokai,AdultRideToOahu = 2;
    int ChildRideToMolokai,ChildRowToMolokai = 1;
    int ChildMolokai,ChildOahu = 1;
    int AdultMolokai,AdultOahu = 2;
    int passengers = 20;
    
  //method to store Boatgrader/adult/child
public static void beginTest(int adult, int child,BoatGrader g){

        this.AdultRowToMolokai = adult;
        this.AdultRowToOahu = adult;
        this.ChildRideToMolokai = child;
        this.ChildRideToOahu = child;
        this.ChildRowToMolokai = child;
        this.ChildRowToOahu = child;
        int remainChild = passengers - adult;
        Bog = g;

        Boat = new Lock();
        Increment = new Lock();
        Wait = new Lock();
        Sleep = new Lock();
        
        // loop and thread/ runnables for child
        for(int i =0; i < child; i++){
            Kthread kt = new Kthread(new Runnable(){
                public void run(){
                    childCrossed();
                }});
                thread.setName("Child Crossed" + i); 
                thread.forkOff();
            }
        // loop and thread/ runnables for adult
        for(int i =0; i < adult; i++){
            Kthread kt = new Kthread(new Runnable(){
                public void run(){
                    adultCrossed();
                }});
                thread.setName("Adult Crossed" + i); 
                thread.forkOff();

            }
            return;
        }


}
public void AdultRowToMolokai();
    bog.AdultRowToMolokai();
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
    bog.AdultRowToOahu();
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
       bog.childCrossed();
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
    bog.ChildRideToMolokai();
   if (child > remainChild){
       ChildRideToMolokai = true;
       Molokai.aquire();
       bog.childCrossed();
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
       bog.childCrossed();
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
    bog.childCrossed();
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

/*
staic void Test(){

    system.out.println( " All Cross on the boat")

    bog.AdultRideToOahu();
    bog.AdultRowToMolokai();
    bog.ChildRowToMolokai();
    bog.ChildRideToMolokai();
} */
