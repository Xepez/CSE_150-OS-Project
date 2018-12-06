/**
 * The Nachos system call interface. These are Nachos kernel operations that
 * can be invoked from user programs using the syscall instruction.
 * 
 * This interface is derived from the UNIX syscalls. This information is
 * largely copied from the UNIX man pages.
 */

#ifndef SYSCALL_H
#define SYSCALL_H

/**
 * System call codes, passed in $r0 to tell the kernel which system call to do.
 */
#define	syscallHalt		0
#define	syscallExit		1
#define	syscallExec		2
#define	syscallJoin		3
#define	syscallCreate		4
#define	syscallOpen		5
#define	syscallRead		6
#define	syscallWrite		7
#define	syscallClose		8
#define	syscallUnlink		9
#define syscallMmap		10
#define syscallConnect		11
#define syscallAccept		12

protected DescriptorController descControl;
protected static Hashtable<String, Integer> files = new Hashtable<String, Integer>();
protected static HashSet<String> deleted = new HashSet<String>();
protected static final int pageSize = Processor.pageSize;
protected static final char dbgProcess = 'a';

/* Don't want the assembler to see C code, but start.s includes syscall.h. */
#ifndef START_S
public class DescriptorController {
	public OpenFile openf[] = new OpenFile[16];
	public int add(int i, OpenFile file) {
		if (i < 0 || i >= 16) return -1;
		if (openf[i] == null) {
			openf[i] = file;
			if (files.get(file.getName()) != null) {
				files.put(file.getName(), files.get(file.getName()) + 1);
			}
			else {
				files.put(file.getName(), 1);
			}
			return i;
		}
		return -1;
	}
	public int add(OpenFile file) {
		for (int i = 0; i < 16; i++) if (openf[i] == null)return add(i, file);
		return -1;
	}
	public OpenFile get(int fDescriptor) {
		if (fDescriptor < 0 || fDescriptor >= 16) return null;
		return openf[fDescriptor];
	}
	public int close(int fDescriptor) {
		if (openf[fDescriptor] == null) {
			Lib.debug(dbgProcess, " file descriptor " + fDescriptor + " does not exist");
			return -1;
		}
		OpenFile file = openf[fDescriptor];
		openf[fDescriptor] = null;
		file.close();
		String fileName = file.getName();
		if (files.get(fileName) > 1)files.put(fileName, files.get(fileName) - 1);
		else {
			files.remove(fileName);
			if (deleted.contains(fileName)) {
				deleted.remove(fileName);
				UserKernel.fileSystem.remove(fileName);
			}
		}
		return 0;
	}
}
/* When a process is created, two streams are already open. File descriptor 0
 * refers to keyboard input (UNIX stdin), and file descriptor 1 refers to
 * display output (UNIX stdout). File descriptor 0 can be read, and file
 * descriptor 1 can be written, without previous calls to open().
 */
#define fdStandardInput		0
#define fdStandardOutput	1
void halt();
void exit(int status);
int exec(char *file, int argc, char *argv[]);
int join(int processID, int *status);

/* FILE MANAGEMENT SYSCALLS: creat, open, read, write, close, unlink
 *
 * A file descriptor is a small, non-negative integer that refers to a file on
 * disk or to a stream (such as console input, console output, and network
 * connections). A file descriptor can be passed to read() and write() to
 * read/write the corresponding file/stream. A file descriptor can also be
 * passed to close() to release the file descriptor and any associated
 * resources.
 */

/**
 * Attempt to open the named disk file, creating it if it does not exist,
 * and return a file descriptor that can be used to access the file.
 *
 * Note that creat() can only be used to create files on disk; creat() will
 * never return a file descriptor referring to a stream.
 *
 * Returns the new file descriptor, or -1 if an error occurred.
 */
int creat(char *name){
	String fName = readVirtualMemoryString(*name, 256);
	if (fName == null) {
		Lib.debug(dbgProcess, "Invalid file name.");
		return -1;
	}
	if (deleted.contains(fName)) {
		Lib.debug(dbgProcess, "File is currently being deleted");
		return -1;
	}
	OpenFile file = UserKernel.fileSystem.open(fName, true);
	if (file == null) {
		Lib.debug(dbgProcess, "Create file failed");
		return -1;
	}
	return descControl.add(file);
}
/**
 * Attempt to open the named file and return a file descriptor.
 *
 * Note that open() can only be used to open files on disk; open() will never
 * return a file descriptor referring to a stream.
 *
 * Returns the new file descriptor, or -1 if an error occurred.
 */
int open(char *name){
	String fileName = readVirtualMemoryString(*name, 256);
	if (fileName == null) {
		Lib.debug(dbgProcess, "Invalid file name.");
		return -1;
	}
	OpenFile file = UserKernel.fileSystem.open(fileName, false);
	if (file == null) {
		Lib.debug(dbgProcess, "File does not exist.");
		return -1;
	}
	if (deleted.contains(fileName)) {
		Lib.debug(dbgProcess, "File is undergoing deletion.");
		return -1;
	}
	return descControl.add(file);
}
/**
 * Attempt to read up to count bytes into buffer from the file or stream
 * referred to by fileDescriptor.
 *
 * On success, the number of bytes read is returned. If the file descriptor
 * refers to a file on disk, the file position is advanced by this number.
 *
 * It is not necessarily an error if this number is smaller than the number of
 * bytes requested. If the file descriptor refers to a file on disk, this
 * indicates that the end of the file has been reached. If the file descriptor
 * refers to a stream, this indicates that the fewer bytes are actually
 * available right now than were requested, but more bytes may become available
 * in the future. Note that read() never waits for a stream to have more data;
 * it always returns as much as possible immediately.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is read-only or
 * invalid, or if a network stream has been terminated by the remote host and
 * no more data is available.
 */

int read(int fileDescriptor, void *buffer, int count){
	int buffr;
	try{
		buffr = *buffer;
	}
	catch (...){
		Lib.debug(dbgProcess, "Invalid Buffer.");
		return -1;
	}
	OpenFile file = descControl.get(fileDescriptor);
	if (file == null) {
		Lib.debug(dbgProcess, "Invalid file descriptor.");
		return -1;
	}
	if (!(buffr >= 0 && count >= 0)) {
		Lib.debug(dbgProcess, "Both buffer and count should be bigger then zero!");
		return -1;
	}
	byte buf[] = new byte[count];
	int length = file.read(buf, 0, count);
	if (length == -1) {
		Lib.debug(dbgProcess, "Failed to read from the file.");
		return -1;
	}
	length = writeVirtualMemory(buffr, buf, 0, length);
	return length;
}
/**
 * Attempt to write up to count bytes from buffer to the file or stream
 * referred to by fileDescriptor. write() can return before the bytes are
 * actually flushed to the file or stream. A write to a stream can block,
 * however, if kernel queues are temporarily full.
 *
 * On success, the number of bytes written is returned (zero indicates nothing
 * was written), and the file position is advanced by this number. It IS an
 * error if this number is smaller than the number of bytes requested. For
 * disk files, this indicates that the disk is full. For streams, this
 * indicates the stream was terminated by the remote host before all the data
 * was transferred.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
 * if a network stream has already been terminated by the remote host.
 */

int write(int fileDescriptor, void *buffer, int count){
	int buffr;
	try{
		buffr = *buffer;
	}
	catch (...){
		Lib.debug(dbgProcess, "Invalid Buffer.");
		return -1;
	}
	OpenFile file = descControl.get(fileDescriptor);
	if (file == null) {
		Lib.debug(dbgProcess, "Invalid file descriptor.");
		return -1;
	}
	if (!(buffr >= 0 && count >= 0)) {
		Lib.debug(dbgProcess, "Both buffer and count should be bigger then zero!");
		return -1;
	}
	byte buf[] = new byte[count];
	int length = readVirtualMemory(buffr, buf, 0, count);
	length = file.write(buf, 0, length);
	return length;
}
/**
 * Close a file descriptor, so that it no longer refers to any file or stream
 * and may be reused.
 *
 * If the file descriptor refers to a file, all data written to it by write()
 * will be flushed to disk before close() returns.
 * If the file descriptor refers to a stream, all data written to it by write()
 * will eventually be flushed (unless the stream is terminated remotely), but
 * not necessarily before close() returns.
 *
 * The resources associated with the file descriptor are released. If the
 * descriptor is the last reference to a disk file which has been removed using
 * unlink, the file is deleted (this detail is handled by the file system
 * implementation).
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
int close(int fileDescriptor) return descControl.close(fileDescriptor);
/**
 * Delete a file from the file system. If no processes have the file open, the
 * file is deleted immediately and the space it was using is made available for
 * reuse.
 *
 * If any processes still have the file open, the file will remain in existence
 * until the last file descriptor referring to it is closed. However, creat()
 * and open() will not be able to return new file descriptors for the file
 * until it is deleted.
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
int unlink(char *name);

/**
 * Map the file referenced by fileDescriptor into memory at address. The file
 * may be as large as 0x7FFFFFFF bytes.
 * 
 * To maintain consistency, further calls to read() and write() on this file
 * descriptor will fail (returning -1) until the file descriptor is closed.
 *
 * When the file descriptor is closed, all remaining dirty pages of the map
 * will be flushed to disk and the map will be removed.
 *
 * Returns the length of the file on success, or -1 if an error occurred.
 */
int mmap(int fileDescriptor, char *address);
int connect(int host, int port);
int accept(int port);

#endif /* START_S */

#endif /* SYSCALL_H */
