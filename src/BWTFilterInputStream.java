import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class BWTFilterInputStream extends CoderFilterInputStream
{
	private final BWTCoder bwtcoder;

	BWTFilterInputStream(InputStream in, int bSize)
	{
		super(in, bSize);
		bwtcoder = new BWTCoder();
	}

	@Override
	protected void encodeBlock()
	{
		bwtcoder.encodeBlock(data);

		data.destblock[bytesRead] = (byte)(data.bwtLineNum >>> 24);
		data.destblock[bytesRead + 1] = (byte)(data.bwtLineNum >>> 16);
		data.destblock[bytesRead + 2] = (byte)(data.bwtLineNum >>> 8);
		data.destblock[bytesRead + 3] = (byte)(data.bwtLineNum & 0xFF);
		bytesRead += 4;
	}

	@Override
	protected void loadAndEncodeNextBlock() throws IOException
	{
		bytesRead = in.read(data.srcblock, 0, data.srcblock.length - 4); // читаем на 4 байта меньше, что бы записать номер строки
		data.bytesInBlock = bytesRead; // this needs for bwtcoder.encode()

		if(bytesRead == -1)
		{
			head = -1;
			bytesRead = 0;
			data.bytesInBlock = 0;
			return;
		}

		encodeBlock();
		head = 0;
	}

}


