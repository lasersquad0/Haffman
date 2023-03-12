import java.io.ByteArrayInputStream;

public class ByteArrayInputStream2 extends ByteArrayInputStream
{
	public ByteArrayInputStream2(byte[] buf)
	{
		super(buf);
	}
	public ByteArrayInputStream2(byte[] buf, int offset, int length)
	{
		super(buf, offset, length);
	}

	public void resetAndSize(int c)
	{
		reset();
		count = c;
	}

	public void setNewBuff(byte[] b)
	{
		buf = b;
		pos = 0;
		count = buf.length;
	}

	public byte[] getBuff()
	{
		return buf;
	}
}
