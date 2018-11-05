package nachos.threads;
import java.util.LinkedList;
public class Communicator {
	private static Lock lock;
	private LinkedList<thrInfo> Speaker;
	private LinkedList<thrInfo> Listener;
	public Communicator() {
		lock = new Lock();
		Speaker = new LinkedList<thrInfo>();
		Listener = new LinkedList<thrInfo>();
	}
	public int listen() {
		lock.acquire();
		int word = 0;
		if (!Speaker.isEmpty()) {
			thrInfo speaker = Speaker.removeFirst();
			word = speaker.getWord();
			speaker.getCondition().wake();
		}
		else {
			thrInfo listener = new thrInfo();
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
			thrInfo listen = Listener.removeFirst();
			listen.setWord(int1);
			listen.getCondition().wake();
		}
		else {
			thrInfo spk = new thrInfo();
			spk.setWord(int1);
			Speaker.add(spk);
			spk.getCondition().sleep();
		}
		lock.release();
	}
	private class thrInfo {
		int int1;
		Condition condition;

		public thrInfo() {
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
