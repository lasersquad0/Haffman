import java.io.*;
import java.util.logging.Logger;

public class HFCompressor
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int i32 = Integer.SIZE; // just for convenience
	CompressData cData;
	byte[] writeBuffer = new byte[4];  // буфер для записи int в OutputStream
	int accum = 0;
	int counter = 0; // счетчик сколько битов вставили в int уже
	long encodedBytes = 0;

	public void compress(CompressData cData) throws IOException
	{
		this.cData = cData;
		encodedBytes = 0;  // сбрасываем счетчики

		compressInternal();

		cData.sizeCompressed = encodedBytes;
	}

	public void compressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"compressInternal");

		delta = cData.sizeUncompressed/100;

		startProgress();

		long total = 0;
		int ch;
		while ((ch = cData.sin.read()) != -1)
		{
			updateProgress(total++);

			encodeSymbol(ch);
		}

		assert counter < i32;

		if(counter > 0) // поток закончился, а еще остались данные в accum, записываем их
		{
			accum = accum << (i32 - counter); // до-сдвигаем accum так что бы "пустые" биты остались справа, а значащие слева
			writeInt(accum);   // записываем весь int даже если значащий бит 1
			// корректируем счетчик encoded bytes что бы при раскодировании не возникали лишние байты.
			int corr = ((counter - 1) >>> 3) + 1; // (counter % 8 == 0) ? (counter >> 3) : (counter >> 3) + 1; // кол-во полных байт занимаемых значимыми битами (округление вверх)
			encodedBytes = encodedBytes - 4 + corr;
			//lastBits = (byte)counter;
		}

		cData.sout.flush();

		finishProgress();

		logger.exiting(this.getClass().getName(),"compressInternal");
	}

	private void encodeSymbol(int sym) throws IOException
	{
		HFCode hfc = cData.tree.codesMap.get(sym);

		if (counter + hfc.len > i32) // новый byte не влазит в остаток слова, делим на 2 части
		{
			accum = accum << (i32 - counter); // освобождаем сколько осталось места в слове
			int len2 = hfc.len + counter - i32; // кол-во не вмещающихся битов
			int code2 = hfc.code >>> len2; // В текущее слово вставляем только часть битов. Остальная часть пойдет в новое слово
			accum = accum | code2;
			writeInt(accum); // заполнили слово полностью
			accum = 0;
			int mask = 0xFFFFFFFF >>> (i32 - len2); // TODO возможно ранее вставленные биты можно и не затирать здесь, они все равно уедут в никуда при сдвигах
			code2 = hfc.code & mask;   // затираем биты которые ранее вставили в предыдущее слово
			accum = accum | code2;
			counter = len2;
		}
		else
		{
			accum = accum << hfc.len;   // освобождаем на длину вставляемого кода
			accum = accum | hfc.code;
			counter += hfc.len;
			if (counter == i32)
			{
				writeInt(accum); // заполнили слово ровно полностью
				accum = 0;
				counter = 0;
			}
		}
	}

	private void writeInt(int v) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v);
		cData.sout.write(writeBuffer, 0, 4);
		encodedBytes += 4;
	}

	private void startProgress()
	{
		if(cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.start();
	}
	private void finishProgress()
	{
		if (cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.finish();
	}

	long threshold = 0;
	long delta;

	private void updateProgress(long total)
	{
		if((cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int)(100*threshold/cData.sizeUncompressed));
		}

	}

}

