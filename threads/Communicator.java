package nachos.threads;
import java.util.LinkedList;
public class Communicator {
	private static Lock lock;
	private LinkedList<ThreadInfo> Speaker;
	private LinkedList<ThreadInfo> Listener;
	public Communicator() {
		lock = new Lock();
		Speaker = new LinkedList<ThreadInfo>();
		Listener = new LinkedList<ThreadInfo>();
	}
	public int listen() {
		lock.acquire();
		int word = 0;
		if (!Speaker.isEmpty()) {
			ThreadInfo speaker = Speaker.removeFirst();
			word = speaker.getWord();
			speaker.getCondition().wake();
		}
		else {
			ThreadInfo listener = new ThreadInfo();
			Listener.add(listener);
			listener.getCondition().sleep();
			word = listener.getWord();
		}
		lock.release();
		return word;
	}
	public void speak(int int1) {
		lock.acquire();
		if (!Listener.isEmpty()) {
			ThreadInfo listen = Listener.removeFirst();
			listen.setWord(int1);
			listen.getCondition().wake();
		}
		else {
			ThreadInfo spk = new ThreadInfo();
			spk.setWord(int1);
			Speaker.add(spk);
			spk.getCondition().sleep();
		}
		lock.release();
	}
	private class ThreadInfo {
		int int1;
		Condition condition;
		public ThreadInfo() {
			int1 = 0;
			condition = new Condition(lock);
		}
		public Condition getCondition() {
			return condition;
		}
		public int getWord() {
			return int1;
		}
		public void setWord(int int2) {
			this.int1 = int2;
		}
	}
}
