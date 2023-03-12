
class BlockCoderData
{
	byte[] srcblock;
	byte[] destblock;
	private int blockSize; // initial block size, containing initial file data before first transformation
	int cBlockSize; // compressed block size
	int uBlockSize; // uncompressed block size, initialised during reading block from archive. in most cases uBlockSize==blockSize
	int bytesInBlock = 0; // Needed for the case when last block read from a file. In all other cases bytesInBlock=inputblock.length
	int bwtLineNum = -1;
	byte bflags;

	public void allocate(int blockSize)
	{
		this.blockSize = blockSize;
		srcblock = new byte[blockSize];
		destblock = new byte[blockSize];
		bytesInBlock = blockSize;
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

}
