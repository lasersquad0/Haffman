import java.io.*;
import java.util.logging.Logger;

public class HFCompressor
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	HFTree tree;
	HFCompressData cData;
	byte[] writeBuffer = new byte[4];  // буфер для записи int в OutputStream
	public long encodedBytes = 0;
	public byte lastBits = 0;


	public void compress(HFTree tree, HFCompressData cData) throws IOException
	{
		encodedBytes = 0;  // сбрасываем счетчики
		lastBits = 0;
		this.tree = tree;
		this.cData = cData;

		compressInternal();

		cData.sizeCompressed = encodedBytes;
		cData.lastBits = lastBits;
	}

	public void compressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"compressInternal");

		long threshold = 0;
		long delta = cData.sizeUncompressed/100;
		long total = 0;
		int ch;
		int accum = 0;
		int counter = 0;
		while ((ch = cData.sin.read()) != -1)
		{
			if(total > threshold)
			{
				threshold +=delta;
				cData.cb.compressPercent((int)(100*threshold/cData.sizeUncompressed));
			}

			total++;
			HFCode hfc = tree.codesMap.get(ch);

			if (counter + hfc.len > Integer.SIZE) // новый byte не влазит в остаток слова, делим на 2 части
			{
				accum = accum << (Integer.SIZE - counter); // освобождаем сколько осталось места в слове
				int len2 = hfc.len + counter - Integer.SIZE; // кол-во не вмещающихся битов
				int code2 = hfc.code >>> len2; // В текущее слово вставляем только часть битов. Остальная часть пойдет в новое слово
				accum = accum | code2;
				writeInt(accum); // заполнили слово полностью
				accum = 0;
				int mask = 0xFFFFFFFF >>> (Integer.SIZE - len2);
				code2 = hfc.code & mask;   // затираем биты которые ранее вставили в предыдущее слово
				accum = accum | code2;
				counter = len2;
			}
			else
			{
				accum = accum << hfc.len;   // освобождаем на длину вставляемого кода
				accum = accum | hfc.code;
				counter += hfc.len;
				if (counter == Integer.SIZE)
				{
					writeInt(accum); // заполнили слово ровно полностью
					accum = 0;
					counter = 0;
				}
			}
		}

		assert counter < Integer.SIZE;

		lastBits = Integer.SIZE;
		if(counter > 0) // поток закончился, а еще остались данные в accum, записываем их
		{
			accum = accum << (Integer.SIZE - counter); // до-сдвигаем accum так что бы "пустые" биты остались справа, а не слева
			writeInt(accum);   // записываем весь
			// корректируем счетчик encoded bytes что бы при раскодировании не возникали лишние байты.
			int corr = counter % 8 == 0 ? counter/8 : counter/8 + 1;
			encodedBytes = encodedBytes - 4 + corr;
			lastBits = (byte)counter;
		}

		cData.sout.flush();

		logger.exiting(this.getClass().getName(),"compressInternal");
	}

	private void writeInt(int v) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v >>>  0);
		cData.sout.write(writeBuffer, 0, 4);
		encodedBytes += 4;
	}


}

