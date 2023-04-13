import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

class ByteArrayOutputStream2 extends ByteArrayOutputStream
{
	public ByteArrayOutputStream2(byte[] buff)
	{
		buf = buff;
		count = 0;
	}
	//public ByteArrayInputStream2(byte[] buf, int offset, int length)
	//{
	//	super(buf, offset, length);
	//}

	public void setNewBuff(byte[] b)
	{
		buf = b;
		count = 0;
	}

	public byte[] getBuff()
	{
		return buf;
	}
}
