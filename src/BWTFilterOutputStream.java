import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

public class BWTFilterOutputStream extends CoderFilterOutputStream
{
	private final BWTCoder bwtcoder;

	BWTFilterOutputStream(OutputStream sout, int bSize)
	{
		super(sout, bSize);
		bwtcoder= new BWTCoder();
	}
	@Override
	protected void decodeBlock()
	{
		int ch1 = Byte.toUnsignedInt(data.srcblock[data.bytesInBlock]);
		int ch2 = Byte.toUnsignedInt(data.srcblock[data.bytesInBlock + 1]);
		int ch3 = Byte.toUnsignedInt(data.srcblock[data.bytesInBlock + 2]);
		int ch4 = Byte.toUnsignedInt(data.srcblock[data.bytesInBlock + 3]);

		data.bwtLineNum = ( (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4 );

		assert data.bwtLineNum >= 0;

		bwtcoder.decodeBlock(data);
	}

	@Override
	protected void decodeAndWriteBlock() throws IOException
	{
		data.bytesInBlock = tail - 4; // in last 4 bytes (int) stored line number
		decodeBlock(); // decode block reduced by 4 bytes
		out.write(data.destblock, 0, data.bytesInBlock);
		tail = 0;
	}
}
