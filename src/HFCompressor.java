import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class HFCompressor extends Compressor
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int i32 = Integer.SIZE; // just for convenience
	private CRC32 crc = new CRC32();
	//CompressData cData;
	int accum = 0;
	int counter = 0; // счетчик сколько битов вставили в int уже
	private long encodedBytes;
	private long decodedBytes;

	@Override
	public void compress(CompressData cData) throws IOException
	{
		this.cdata = cData;
		encodedBytes = 0;  // сбрасываем счетчики

		startProgress(cdata.sizeUncompressed);

		compressInternal();

		cdata.sout.flush();
		finishProgress();

		cData.sizeCompressed = encodedBytes;
	}

	public void compressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"compressInternal");

		long total = 0;
		int ch;
		while ((ch = cdata.sin.read()) != -1)
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
			int corr = ((counter - 1) >>> 3) + 1; // кол-во полных байт занимаемых значимыми битами (округление вверх)
			encodedBytes = encodedBytes - 4 + corr;
		}

		logger.exiting(this.getClass().getName(),"compressInternal");
	}

	private void encodeSymbol(int sym) throws IOException
	{
		HFCode hfc = cdata.tree.codesMap.get(sym);

		if (counter + hfc.len > i32) // новый byte не влазит в остаток слова, делим на 2 части
		{
			accum = accum << (i32 - counter); // освобождаем сколько осталось места в слове
			int len2 = hfc.len + counter - i32; // кол-во не вмещающихся битов
			int code2 = hfc.code >>> len2; // В текущее слово вставляем только часть битов. Остальная часть пойдет в новое слово
			accum = accum | code2;
			writeInt(accum); // заполнили слово полностью
			accum = hfc.code; // accum = 0;
			//int mask = 0xFFFFFFFF >>> (i32 - len2); // TODO возможно ранее вставленные биты можно и не затирать здесь, они все равно уедут в никуда при сдвигах
			//code2 = hfc.code & mask;   // затираем биты которые ранее вставили в предыдущее слово
			//accum = accum | code2;
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

	@Override
	public void uncompress(CompressData uData) throws IOException
	{
		this.cdata = uData;
		decodedBytes = 0;

		startProgress(cdata.sizeCompressed);

		uncompressInternal2();

		cdata.sout.flush();

		finishProgress();

		uData.CRC32Value = crc.getValue();
	}

	private void uncompressInternal2() throws IOException
	{
		//final int i32 = Integer.SIZE; // just for convenience
		long encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = 0;

		while(encodedBytes < cdata.sizeCompressed) // заканчиваем раскодировать как только кончились байты
		{
			updateProgress(encodedBytes);

			// вот так хитро читаем int из потока
			int ch1 = cdata.sin.read();
			int ch2 = cdata.sin.read();
			int ch3 = cdata.sin.read();
			int ch4 = cdata.sin.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0) break;

			encodedBytes +=4;   // считаем сколько encoded bytes прочитали и потока и сравниваем с размером sizeCompressed что бы вовремя остановиться.

			data = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);

			remaining = (remaining == 0) ? i32: remaining;

			if(remaining < i32) // есть остатки с прошлого байта они в data2
			{
				data2 <<= (i32 - remaining);    // старые биты равняем по левому краю, освобождаем место под новые биты
				int tmp = data >>> remaining;            // готовим новые данные в data для соединения с остатками старых
				data2 |= tmp;
				int tmpRemaining = parseInt(data2); // после вызова у нас есть 2 остатка битов: часть битов data2 и вторая часть это неиспользованные биты в data

				//assert ( (remaining + bitsToParse < i32)? (tmpRemaining == 0): true); // если remaining+bitsToParse < i32 то tmpRemaining всегда должен быть == 0

				if(remaining + tmpRemaining > i32) // Остатков может оказаться больше чем вмещает int. Объединяем и раскодируем их
				{
					remaining = remaining + tmpRemaining - i32;
					data2 <<= (i32 - tmpRemaining);  // освобождаем все не занятое в data2 место под новые биты
					tmp = data >>> remaining; //(remaining - (i32 - tmpRemaining));   // убираем лишние младшие биты из data что бы добавить к тому что осталось в data2 после parseInt
					mask = 0xFFFFFFFF >>> tmpRemaining;
					tmp = tmp & mask;        // очищаем лишние биты перед объединением байтов.
					data2 |= tmp;

					assert remaining <= i32;

					tmpRemaining = parseInt(data2);

					//assert (remaining+bitsToParse <= i32) ? tmpRemaining == 0: true;

					//remaining = Math.max(remaining + bitsToParse - i32, 0); // вносим поправку не bitsToParse так как оставшиеся биты могут быть пустыми
				}

				assert remaining <= i32;

				// соединяем остаток битов от parseInt и биты которые не влезли от data
				data2 <<= remaining;
				mask = 0xFFFFFFFF >>> (i32 - remaining);
				data &= mask;        // очищаем лишние биты перед объединением байтов.
				data2 |= data;
				remaining += tmpRemaining;

				if(remaining == i32) // Из остатков набрался целый байт. Записываем его перед чтением следующего
				{
					remaining = parseInt(data2);
				}

				assert (remaining <= i32);
			}
			else
			{
				data2 = data;
				remaining = parseInt(data2);
			}
		}

		//	if((remaining > 0) && (remaining < i32))
		//	{
		data2 <<= (i32 - remaining);
		remaining = parseInt(data2);
		//	}

	}

	/**
	 *
	 * @param code int для парсинга
	 *
	 * @return возвращает кол-во не оставшихся нераспаршеных битов в code.
	 * @throws IOException если при записи в поток, что-то пошло не так
	 */
	private int parseInt(int code) throws IOException
	{
		int remaining = i32;
		int ccode = code;
		while( (remaining > 0) && (decodedBytes < cdata.sizeUncompressed) )
		{
			boolean found = false;
			for(int i = 0; i < cdata.tree.codesList.size(); i++)
			{
				HFCode hfc = cdata.tree.codesList.get(i);
				if(hfc.len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				//int mask = 0xFFFFFFFF << (i32 - hfc.len);
				//int c = hfc.code << (i32 - hfc.len); // готовим код для операции сравнения, делаем его выровненным по левому краю
				//int code2 = ccode & mask;         // оставляем только биты равные длине кода
				//code2 = code2 ^ c;            // XOR - если операнды равны то результат будет 0.
				int code2 = ccode >>> (i32 - hfc.len);
				code2 = code2 ^ hfc.code;
				if(code2 == 0)  // совпал код
				{
					writeByte(hfc.symbol);
					ccode = ccode << hfc.len; // выравниваем оставшиеся биты влево.
					remaining -= hfc.len;
					found = true;
					break;
				}
			}

			// Здесь в целом может быть кейс когда весь цикл прошел, а код так и не найден. Это значит что осталось мало битов в ccode и сравнение не сработало.
			// Сработает когда parent method докинет еще битов в ccode
			assert !((remaining > cdata.tree.maxCodeLen) && (!found));

			if(!found) break;    // прерываем цикл, что бы докинулось еще битов в ccode

		}

		// небольшой hack, если закончили парсинг файла то возвращаем 0, что бы избежать дополнительных попыток допарсить остатки этого int
		return (decodedBytes < cdata.sizeUncompressed)? remaining: 0; // возвращаем сколько осталось нераспаршеных битов из bitsToParse а не из всего int.
	}

	private void writeByte(int v) throws IOException
	{
		cdata.sout.write(v);
		crc.update(v);
		decodedBytes++;
	}


	private final byte[] writeBuffer = new byte[4];  // буфер для записи int в OutputStream
	private void writeInt(int v) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v);
		cdata.sout.write(writeBuffer, 0, 4);
		encodedBytes += 4;
	}

/*
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

 */

}

