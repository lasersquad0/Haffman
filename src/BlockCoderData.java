import java.io.IOException;
import java.io.InputStream;

class BlockCoderData
{
	byte[] srcblock;
	byte[] destblock;
	private int blockSize = 0; // initial block size, containing initial file data before first transformation
	int cBlockSize; // compressed block size
	int uBlockSize; // uncompressed block size, initialised during reading block from archive. in most cases uBlockSize==blockSize
	int bytesInBlock = 0; // Needed for the case when last block read from a file. In all other cases bytesInBlock=inputblock.length
	int bwtLineNum = -1;
	byte bflags;

	public void allocate(int blockSize)
	{
		if(blockSize <= 0)
			throw new IllegalArgumentException();

		this.blockSize = blockSize;
		srcblock = new byte[blockSize];
		destblock = new byte[blockSize];
		uBlockSize = blockSize;
		bytesInBlock = 0;
		ind = 0;
	}

	/**
	 * Re-allocates buffers with new size newBlockSize
	 * IMPORTANT - does not change values of int bwtLineNum, bytesInBlock, uBlockSize and cBlockSize since they may be defined before
	 * reAllocate call because they've read from a file
	 * @param newBlockSize - new size of this block
	 */
	public void reAllocate(int newBlockSize)
	{
		if(blockSize <= 0)
			throw new IllegalArgumentException();

		srcblock = new byte[newBlockSize];
		destblock = new byte[newBlockSize];
		blockSize = newBlockSize;
		ind = 0;
	}

	public int getBlockSize() { return blockSize; }

	int ind;
	public void writeDest(int b)
	{
		destblock[ind++] = (byte)b;
	}

	public void writeDest(byte[] b, int off, int len)
	{
		System.arraycopy(b, off, destblock, ind, len);
		ind += len;
	}

	public void resetDest()
	{
		ind = 0;
	}

	public void swapBuffers()
	{
		byte[] temp = srcblock;
		srcblock = destblock;
		destblock = temp;
	}

	public int readFrom(InputStream in) throws IOException
	{
		bytesInBlock = in.read(srcblock, 0, blockSize);
		return bytesInBlock;
	}
}

