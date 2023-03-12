import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class CoderFilterOutputStream extends FilterOutputStream
{
	protected final int blockSize;
	protected int tail;
	protected final BlockCoderData data;

	protected abstract void decodeBlock();


	public CoderFilterOutputStream(OutputStream out, int bSize)
	{
		super(out);
		blockSize = bSize;
		data = new BlockCoderData();
		data.allocate(bSize);  // we know block size here, ready to allocate memory for arrays
		tail = 0;
	}

	@Override
	public void write(int b) throws IOException
	{
		assert tail < blockSize;

		data.srcblock[tail++] = (byte)b;

		if(tail == blockSize)
			decodeAndWriteBlock();
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		assert len > 0;
		assert off >= 0;
		assert off + len <= b.length;

		if(len < blockSize - tail) // it is enough space in a buffer to add len bytes
		{
			System.arraycopy(b, off, data.srcblock, tail, len);
			tail += len;
			return;  // srcblock is not full, wait for more bytes
		}
		else
		{
			int c1 = blockSize - tail;
			System.arraycopy(b, off, data.srcblock, tail, c1);
			tail += c1;
			decodeAndWriteBlock(); // srcblock is full, decoding and writing it

			do
			{
				if(len < c1 + blockSize)
				{
					System.arraycopy(b, off + c1, data.srcblock, 0, len - c1); //srcblock is not full
					tail += len - c1;
					break;
				}
				else
				{
					System.arraycopy(b, off + c1, data.srcblock, 0, blockSize); // srcblock is full
					c1 += blockSize;
					tail = blockSize;
					decodeAndWriteBlock();
					//tail = 0;
				}

			}while(c1 < len);
		}
	}

	protected void decodeAndWriteBlock() throws IOException
	{
		data.bytesInBlock = tail; // bytesInBlock used by MTF and BWT decoders
		decodeBlock();
		out.write(data.destblock, 0, tail);
		tail = 0;
	}

	public void close() throws IOException
	{
		decodeAndWriteBlock();
		out.close();
	}

/*	public void flush() throws IOException
	{
		throw new IOException("flus() method is not supported.");
	}
*/

}
