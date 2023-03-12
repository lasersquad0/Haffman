import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class CoderFilterInputStream extends FilterInputStream {
	protected final int blockSize;
	protected int bytesRead;
	protected int head;
	protected final BlockCoderData data;

	protected abstract void encodeBlock();

	CoderFilterInputStream(InputStream in, int bSize)
	{
		super(in);
		blockSize = bSize;
		data = new BlockCoderData();
		data.allocate(bSize);  // we know block size here, ready to allocate memory for arrays
		bytesRead = 0;
		head = -1;
	}

	@Override
	public int read() throws IOException
	{
		if( (head >= 0) && (head < bytesRead) ) return Byte.toUnsignedInt(data.destblock[head++]);
		if( (head == bytesRead) && (bytesRead < blockSize) ) return -1; // input stream is over, returning EOF result

		// buffer is over, need to read more data from input stream
		loadAndEncodeNextBlock();

		return (head == -1)? -1: Byte.toUnsignedInt(data.destblock[head++]);
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		return read(b,0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		assert len > 0;
		assert off >= 0;
		assert off + len <= b.length;

		if(bytesRead == 0)
		{
			loadAndEncodeNextBlock();
			if (bytesRead == 0) return -1;
		}

		if(len <= bytesRead - head) // enough bytes in buffer
		{
			System.arraycopy(data.destblock, head, b, off, len);
			head += len;
			return len;
		}
		else
		{
			int c1 = bytesRead - head;
			System.arraycopy(data.destblock, head, b, off, c1); // copy rest of the buffer into output buffer

			do
			{
				loadAndEncodeNextBlock();
				if (bytesRead == 0) return c1;

				if (c1 + bytesRead >= len) // we have more bytes than len
				{
					System.arraycopy(data.destblock, 0, b, off + c1, len - c1);
					head += len - c1;
					return len; // we were able to read and copy len bytes
				}
				else
				{
					System.arraycopy(data.destblock, 0, b, off + c1, bytesRead);
					c1 += bytesRead;
					head = bytesRead;
				}
			} while (c1 < len);

			return c1; // we were able to read and copy len bytes
		}
	}

	protected void loadAndEncodeNextBlock() throws IOException
	{
		bytesRead = in.read(data.srcblock);
		data.bytesInBlock = bytesRead; // that needs for bwtcoder.encode()

		if(bytesRead == -1)
		{
			head = -1;
			bytesRead = 0;
			return;
		}

		encodeBlock();
		head = 0;
	}

}


