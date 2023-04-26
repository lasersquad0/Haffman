import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class RLECoder
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final int EQUALS_MAXLEN = 129;
	private final int UNEQUALS_MAXLEN = 128;
	private ByteArrayOutputStream2 tout;


	public boolean encodeBlock(BlockCoderData data) throws IOException
	{
		if(tout == null)
			tout = new ByteArrayOutputStream2(data.destblock);
		else
			tout.setNewBuff(data.destblock);

		if(!encodeBlockAlg(data, tout))	return false;

		data.bytesInBlock = data.cBlockSize;
		return true;
	}

	public boolean encodeBlockAlg(BlockCoderData data, OutputStream out) throws IOException
	{
		int cntRead = data.bytesInBlock;
		byte[] buf = data.srcblock;

		int compressed = 0;
		int head = 0;
		int i = 0;
		while(true)
		{
			while ((i - head < UNEQUALS_MAXLEN) && (i < cntRead - 2))
			{
				if ((buf[i] == buf[i + 1]) && (buf[i] == buf[i + 2]))  // we've found three equal bytes in a row
					break;
				i++;
			} // i refers to first symbol of the NEXT group


			if((i >= cntRead - 2) && (i - head + 2 <= UNEQUALS_MAXLEN))
				i = cntRead;

			assert i - head <= UNEQUALS_MAXLEN;

			if (i > head)   // coding sequence of NON equal bytes
			{
				int count = (i - head - 1);  // do not forget to add 1 during decoding
				assert count >= 0;
				//total += count + 1;
				out.write(count);
				out.write(buf, head, i - head);
				compressed += 1 + i - head;
				head = i;
			}

			if(compressed > cntRead) return false;

			assert i <= cntRead;

			if(i == cntRead)
				break;

			while ((i < cntRead - 1) && (buf[i] == buf[i + 1]) && (i - head + 1 < EQUALS_MAXLEN))
			{
				i++;
			} // i refers to the LAST symbol of the current group (except for i-head+1>=EQUALS_MAXLEN)

			assert i - head <= EQUALS_MAXLEN;

			if (i - head >= 2) // coding sequence of equal bytes. 2 equal bytes in a row is not considered as equal seq.
			{
				int count = (i - head + 1 - 2);  // do not forget to add 2 during decoding
				assert count >= 0;
				//total += count + 2;
				count |= 0x80;    // set major bit
				out.write(count);
				out.write(buf[i]);   // saving just one repeated symbol
				compressed += 2;
				i = head = i + 1;
			}
			else i = head; // стопнули на UNEQUALS_MAXLEN в первом цикле и так оказалось что следующие 2 байта (только 2) одинаковые тогда портится i и нужно его вернуть на head
			// другое решение это вверху подправить условие на (i - head >= 1) т.о. в таких редких случаях мы будем записывать 2 одинаковых элемента как equal seq.

			assert i <= cntRead;

			if(compressed > cntRead) return false;

			if(i == cntRead)
				break;
		}

		data.cBlockSize = compressed;
		data.bytesInBlock = compressed;
		return true;
	}

	public void decodeBlock(BlockCoderData data)
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		byte[] buf = data.srcblock;

		data.resetDest();
		int i = 0;
		while(true)
		{
			byte code = buf[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) // saving code+2 equal bytes
				{
					data.writeDest(buf[i + 1]);
					//crc.update(buff[i + 1]);
				}
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < data.bytesInBlock;

				if(i + 1 + code >= data.bytesInBlock)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= data.bytesInBlock) i=%d code=%d data.bytesInBlock=%d encodedbytes=%d", i, code, data.bytesInBlock, data.bytesInBlock));
				}

				assert i + 1 + code < data.bytesInBlock;

				data.writeDest(buf, i + 1, code + 1);
				//crc.update(buff, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == data.bytesInBlock) break;
		}
		data.bytesInBlock = data.ind;
	}
}
