package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int size=Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[size];
		for (int i = 0; i < size; ++i){
			pageTable[i] = new TranslationEntry(i, 0, false, false, false, false);
		}

		counterLock=new Lock();
		counterLock.acquire();
		counterLock.release();
		status=new Lock();

		type=new OpenFile[16];
		boolean inStatus=Machine.interrupt().disable();
		Machine.interrupt().restore(inStatus);

		processID=counter++;

		stdin = UserKernel.console.openForReading();
		stdout = UserKernel.console.openForWriting();
		type[0]=stdin;
		type[1]=stdout;

		parent=null;
		children=new LinkedList<UserProcess>();
		childrenExitStatus=new HashMap<Integer,Integer>();



	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}


	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(absoluteFileName(name), args))
			return false;

		Lib.debug(dbgProcess, "process created, processID = " + processID);
		//
		thread = (UThread) (new UThread(this).setName(name));
		thread.fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	public int getprocessID() {
		return processID;
	}

	protected boolean allocate(int vpn, int desiredPages, boolean readOnly) {
		//    	System.out.println("hehe");
		LinkedList<TranslationEntry> allocated = new LinkedList<TranslationEntry>();

		for (int i = 0; i < desiredPages; ++i) {
			if (vpn >= pageTable.length)
				return false;

			int ppn = UserKernel.makePage();
			if (ppn == -1) {
				Lib.debug(dbgProcess, "\tcannot allocate new page");

				for (TranslationEntry te: allocated) {
					pageTable[te.vpn] = new TranslationEntry(te.vpn, 0, false, false, false, false);
					UserKernel.delPage(te.ppn);
					--numPages;
				}

				return false;
			} else {
				TranslationEntry a = new TranslationEntry(vpn + i,
						ppn, true, readOnly, false,false);
				allocated.add(a);
				pageTable[vpn + i] = a;
				++numPages;
			}
		}
		return true;
	}

	protected void releaseResource() {
		for (int i = 0; i < pageTable.length; ++i)
			if (pageTable[i].valid) {
				UserKernel.delPage(pageTable[i].ppn);
				pageTable[i] = new TranslationEntry(pageTable[i].vpn, 0, false, false, false, false);
			}
		numPages = 0;
	}


	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	protected TranslationEntry lookUpPageTable(int vpn) {
		if (pageTable == null)
			return null;

		if (vpn >= 0 && vpn < pageTable.length)
			return pageTable[vpn];
		else
			return null;
	}

	protected TranslationEntry translate(int vaddr) {
		return lookUpPageTable(UserKernel.getVirtualPageNumber(vaddr));
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int firstVPN = Processor.pageFromAddress(vaddr), firstOffset = Processor
				.offsetFromAddress(vaddr), lastVPN = Processor
				.pageFromAddress(vaddr + length);

		TranslationEntry entry = getTranslationEntry(firstVPN, false);

		if (entry == null)
			return 0;

		int amount = Math.min(length, pageSize - firstOffset);
		System.arraycopy(memory, Processor.makeAddress(entry.ppn, firstOffset),
				data, offset, amount);
		offset += amount;

		for (int i = firstVPN + 1; i <= lastVPN; i++) {
			entry = getTranslationEntry(i, false);
			if (entry == null)
				return amount;
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(memory, Processor.makeAddress(entry.ppn, 0), data,
					offset, len);
			offset += len;
			amount += len;
		}

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int firstVPN = Processor.pageFromAddress(vaddr), firstOffset = Processor
				.offsetFromAddress(vaddr), lastVPN = Processor
				.pageFromAddress(vaddr + length);

		TranslationEntry entry = getTranslationEntry(firstVPN, true);

		if (entry == null)
			return 0;

		int amount = Math.min(length, pageSize - firstOffset);
		System.arraycopy(data, offset, memory, Processor.makeAddress(entry.ppn,
				firstOffset), amount);
		offset += amount;

		for (int i = firstVPN + 1; i <= lastVPN; i++) {
			entry = getTranslationEntry(i, true);
			if (entry == null)
				return amount;
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(data, offset, memory, Processor.makeAddress(
					entry.ppn, 0), len);
			offset += len;
			amount += len;
		}

		return amount;
	}
	
	public TranslationEntry getTranslationEntry(int vpn, boolean isWrite) {
		if (vpn < 0 || vpn >= numPages)
			return null;
		TranslationEntry result = pageTable[vpn];
		if (result == null)
			return null;
		if (result.readOnly && isWrite)
			return null;
		result.used = true;
		if (isWrite)
			result.dirty = true;
		return result;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	protected boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			if (!allocate(numPages, section.getLength(), section.isReadOnly())) {
				releaseResource();
				return false;
			}
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		if (!allocate(numPages, stackPages, false)) {
			releaseResource();
			return false;
		}
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		if (!allocate(numPages, 1, false)) {
			releaseResource();
			return false;
		}

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
			+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				TranslationEntry te = lookUpPageTable(vpn);
				if (te == null)
					return false;
				section.loadPage(i, te.ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		releaseResource();
		for(int i=0;i<16;i++){
			if(type[i]!=null){
				type[i].close();
				type[i]=null;
			}	
		}
		coff.close();

	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < Processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	public String absoluteFileName(String s) {
		return s;
	}

	private int handleHalt() {
		if(processID!=0){
			return 0;
		}

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private int handleRead(int typeD,int bufferVAddr,int size){
		if(typeD<0||typeD>15){
			Lib.debug(dbgProcess, "Read:Descriptor out of range");
			return -1;
		}
		if(size<0){
			Lib.debug(dbgProcess, "Read:Size to read cannot be negative");
			return -1;
		}
		OpenFile file;
		if(type[typeD]==null){
			Lib.debug(dbgProcess, "Read:File doesn't exist in the descriptor table");
			return -1;
		}else{
			file=type[typeD];
		}
		int length=0;
		byte[] reader=new byte[size];
		length=file.read(reader, 0, size);
		if(length==-1){
			Lib.debug(dbgProcess, "Read:Error occurred when try to read file");
			return -1;
		}
		int count=0;
		count=writeVirtualMemory(bufferVAddr,reader,0,length);
		return count;

	}

	private int handleCreate(int vaddr){
		if(vaddr<0){
			Lib.debug(dbgProcess, "Create:Invalid virtual address");
			return -1;
		}
		String fileName=readVirtualMemoryString(vaddr,256);
		if(fileName==null){
			Lib.debug(dbgProcess, "Create:Read filename failed");
			return -1;
		}
		int availableIndex=-1;
		for(int i=0;i<16;i++){
			if(type[i]==null){
				availableIndex=i;
				break;
			}
		}
		if(availableIndex==-1){
			Lib.debug(dbgProcess, "Create:Cannot create more than 16 files");
			return -1;
		}else{
			OpenFile file=ThreadedKernel.fileSystem.open(fileName, true);
			if(file==null){
				Lib.debug(dbgProcess, "Create:Create failed");
				return-1;
			}else{
				type[availableIndex]=file;
				return availableIndex;
			}		
		}	

	}

	private int handleWrite(int typeD,int bufferVAddr,int size){
		if(typeD<0||typeD>15){
			Lib.debug(dbgProcess,"Wirte:type out of range");
			return -1;
		}
		if(size<0){
			Lib.debug(dbgProcess, "Write:Size  cannot be negative");
			return -1;	
		}
		OpenFile file;
		if(type[typeD]==null){
			Lib.debug(dbgProcess, "Write:File doesn't exist");
			return -1;
		}else{
			file=type[typeD];
		}
		int length=0;
		byte[] writer=new byte[size];
		length=readVirtualMemory(bufferVAddr,writer,0,size);
		int count=0;
		count=file.write(writer, 0, length);
		//System.out.println(size==count);
		if(count==-1){
			Lib.debug(dbgProcess, "Write:Error occured");
			return -1;
		}
		return count;
	}

	private int handleExit(int status){
		if(parent!=null){
			parent.status.acquire();
			parent.childrenExitStatus.put(processID, status);
			parent.status.release();

		}
		unloadSections();
		int childrenNum=children.size();
		for(int i=0;i<childrenNum;i++){
			UserProcess child=children.removeFirst();
			child.parent=null;
		}
		System.out.println("exit"+processID+status);

		if(processID==0){
			Kernel.kernel.terminate();
		}else{
			UThread.finish();
		}
		return 0;

	}

	private int handleJoin(int processID,int statusV){
		if(processID<0||statusV<0){
			return -1;
		}
		UserProcess child=null;
		int childrenNum=children.size();
		for(int i=0;i<childrenNum;i++){
			if(children.get(i).processID==processID){
				child=children.get(i);
				break;
			}
		}

		if(child==null){
			Lib.debug(dbgProcess, "handleJoin:processID is not the child");
			return -1;
		}

		child.thread.join();

		child.parent=null;

		children.remove(child);

		status.acquire();
		Integer stat=childrenExitStatus.get(child.processID);
		status.release();

		if(stat==null){
			Lib.debug(dbgProcess, "Join:Cannot find the exit status of the child");
			return 0;
		}
		else{

			byte[] buffer=new byte[4];
			buffer=Lib.bytesFromInt(stat);
			int count=writeVirtualMemory(statusV,buffer);
			if(count==4){
				return 1;
			}else{
				Lib.debug(dbgProcess, "Join:Write status failed");
				return 0;
			}
		}
	}

	private int handleExec(int name,int num,int argsV ){
		if(name<0||num<0||argsV<0){
			Lib.debug(dbgProcess, "eExec:Invalid parameter");
			return -1;
		}
		String fileName=readVirtualMemoryString(name, 256);
		if(fileName==null){
			Lib.debug(dbgProcess, "Exec:Read filename failed");
			return -1;
		}
		if(!fileName.contains(".coff")){
			Lib.debug(dbgProcess, "Exec:Filename needs to end with .coff");
			return -1;
		}

		String[] args=new String[num];
		for(int i=0;i<num;i++){
			byte[] buffer=new byte[4];
			int readLength;
			readLength=readVirtualMemory(argsV+i*4,buffer);
			if(readLength!=4){
				Lib.debug(dbgProcess, "Exec:Read arg faijled");
				return -1;
			}
			int argV=Lib.bytesToInt(buffer, 0);
			String arg=readVirtualMemoryString(argV,256);
			if(arg==null){
				Lib.debug(dbgProcess, "Exec:Read arg failed");
				return -1;
			}
			args[i]=arg;
		}
		UserProcess child=UserProcess.newUserProcess();
		boolean isSuccessful=child.execute(fileName, args);
		if(!isSuccessful){
			Lib.debug(dbgProcess, "Exec:Execute child process failed");
			return -1;
		}
		child.parent=this;
		this.children.add(child);
		int id=child.processID;
		return id;
	}

	private int handleOpen(int vaddr){
		if(vaddr<0){
			Lib.debug(dbgProcess, "handleOpen:Invalid virtual address");
			return -1;
		}
		String fileName=readVirtualMemoryString(vaddr,256);
		if(fileName==null){
			Lib.debug(dbgProcess, "handleOpen:Read filename failed");
			return -1;

		}
		int availableIndex=-1;
		for(int i=0;i<16;i++){
			if(type[i]==null){
				availableIndex=i;
				break;
			}
		}
		if(availableIndex==-1){
			Lib.debug(dbgProcess, "Open:Cannot make more than 15 files");
			return -1;
		}else{
			OpenFile file=ThreadedKernel.fileSystem.open(fileName, false);
			if(file==null){
				Lib.debug(dbgProcess, "Open:Open failed");
				return -1;
			}else{
				type[availableIndex]=file;
				return availableIndex;
			}
		}
	}



	private int handleClose(int typeD){
		if(typeD<0||typeD>15){
			Lib.debug(dbgProcess, "Close:Descriptor out of range");
			return -1;
		}
		if(type[typeD]==null){
			Lib.debug(dbgProcess, "Close:File doesn't exist in the descriptor table");
			return -1;
		}else{
			type[typeD].close();
			type[typeD]=null;
		}
		return 0;
	}


	private int handleUnlink(int vaddr){
		if(vaddr<0){
			Lib.debug(dbgProcess, "Unlink:Invalid virtual address");
			return -1;
		}
		String fileName=readVirtualMemoryString(vaddr,256);
		if(fileName==null){
			Lib.debug(dbgProcess, "Unlink:Read filename failed");
			return -1;
		}
		OpenFile file;
		int index=-1;
		for(int i=0;i<16;i++){
			file=type[i];
			if(file!=null&&file.getName().compareTo(fileName)==0){
				index=i;
				break;
			}
		}	
		if(index!=-1){
			Lib.debug(dbgProcess, "Unlink:File should be closed first");
			return -1;
		}
		boolean isSuccessful=ThreadedKernel.fileSystem.remove(fileName);
		if(!isSuccessful){
			Lib.debug(dbgProcess, "Unlink:Remove failed");
			return -1;
		}

		return 0;


	}



	protected static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
	 * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
	 * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td></tr>
	 * <tr><td>3</td><td><tt>int  join(int processID, int *status);</tt></td></tr>
	 * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
	 * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
	 * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
	 * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
	 * </table>
	 * 
	 * @param	syscall	the syscall number.
	 * @param	a0	the first syscall argument.
	 * @param	a1	the second syscall argument.
	 * @param	a2	the third syscall argument.
	 * @param	a3	the fourth syscall argument.
	 * @return	the value to be returned to the user.
	 */

	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!, goodbye");

		case syscallCreate:
			Lib.debug(dbgProcess, "Create called from process " + processID);
			return handleCreate(a0);

		case syscallClose:
			Lib.debug(dbgProcess, "Close called from process " + processID);
			return handleClose(a0);

		case syscallJoin:
			Lib.debug(dbgProcess, "Join called from process " + processID);
			return handleJoin(a0, a1);

		case syscallUnlink:
			Lib.debug(dbgProcess, "Unlink called from process " + processID);
			return handleUnlink(a0);

		case syscallRead:
			return handleRead(a0, a1, a2);

		case syscallWrite:
			return handleWrite(a0, a1, a2);

		case syscallExec:
			return handleExec(a0, a1, a2);

		case syscallOpen:
			return handleOpen(a0);

		case syscallExit:
			return handleExit(a0);
		}
		//return 0;
	}   
	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */

	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0), processor
					.readRegister(Processor.regA1), processor
					.readRegister(Processor.regA2), processor
					.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	public static final int exceptionIllegalSyscall = 100;

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of pages occupied by the program. */
	protected int numPages;
	protected OpenFile stdin;
	protected OpenFile stdout;
	protected OpenFile[] type;
	protected OpenFile[] descriptors;

	/** The number of pages in the program's stack. */
	protected final int stackPages = Config.getInteger("Processor.numStackPages", 8);
	protected static final int pageSize = Processor.pageSize;
	protected static final char dbgProcess = 'a';
	protected int initialPC, initialSP;
	protected int argc, argv;

	//Instantiate locks
	protected Lock counterLock;
	protected Lock status;

	// parents and children list/hash
	protected UserProcess parent;
	protected LinkedList<UserProcess> children;
	protected HashMap<Integer,Integer> childrenExitStatus;

	//Instantiation for thread(s)
	protected UThread thread;

	//Instantiate counter and processID
	protected static int counter = 0;
	protected int processID;
}
