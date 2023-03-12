import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class HFUncompressor
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	CompressData udata;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;

	public void uncompress(CompressData uData) throws IOException
	{
		this.udata = uData;
		decodedBytes = 0;

		uncompressInternal2();

		uData.CRC32Value = crc.getValue();
	}

	private void uncompressInternal2() throws IOException
	{
		logger.entering(this.getClass().getName(),"uncompressInternal");

		delta = udata.sizeCompressed/100;
		final int i32 = Integer.SIZE; // just for convenience
		long encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = 0;

		startProgress();

		while(encodedBytes < udata.sizeCompressed) // заканчиваем раскодировать как только кончились байты
		{
			updateProgress(encodedBytes);

			// вот так хитро читаем int из потока
			int ch1 = udata.sin.read();
			int ch2 = udata.sin.read();
			int ch3 = udata.sin.read();
			int ch4 = udata.sin.read();
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

		udata.sout.flush();

		finishProgress();

		logger.exiting(this.getClass().getName(),"uncompressInternal");
	}

/*
	private void uncompressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"uncompressInternal");

		delta = uData.sizeCompressed/100;
		final int i32 = Integer.SIZE; // just for convenience
		int bitsToParse = i32;
		long encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = i32;

		startProgress();

		while(encodedBytes < uData.sizeCompressed) // заканчиваем раскодировать как только кончились байты
		{
			updateProgress(encodedBytes);

			// вот так хитро читаем int из потока
			int ch1 = uData.sin.read();
			int ch2 = uData.sin.read();
			int ch3 = uData.sin.read();
			int ch4 = uData.sin.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0) break;

			encodedBytes +=4;   // считаем сколько encoded bytes прочитали и потока и сравниваем с размером sizeCompressed что бы вовремя остановиться.

			if(encodedBytes >= uData.sizeCompressed) // вычитали последний int который нужно парсить не полностью
			{
				assert uData.lastBits <= i32;
				bitsToParse = uData.lastBits;
			}

			data = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);

			if(remaining < i32) // есть остатки с прошлого байта они в data2
			{
				data2 <<= (i32 - remaining);    // старые биты равняем по левому краю, освобождаем место под новые биты
				int tmp = data >>> remaining;            // готовим новые данные в data для соединения с остатками старых
				data2 |= tmp;
				int tmpRemaining = parseInt(data2, Math.min(remaining + bitsToParse, i32)); // после вызова у нас есть 2 остатка битов: часть битов data2 и вторая часть это неиспользованные биты в data

				assert ( (remaining + bitsToParse < i32)? (tmpRemaining == 0): true); // если remaining+bitsToParse < i32 то tmpRemaining всегда должен быть == 0

				if(remaining + tmpRemaining > i32) // Остатков может оказаться больше чем вмещает int. Объединяем и раскодируем их
				{
					data2 <<= (i32 - tmpRemaining);  // освобождаем все не занятое в data2 место под новые биты
					tmp = data >>> (remaining - (i32 - tmpRemaining));   // убираем лишние младшие биты из data что бы добавить к тому что осталось в data2 после parseInt
					mask = 0xFFFFFFFF >>> tmpRemaining;
					tmp = tmp & mask;        // очищаем лишние биты перед объединением байтов.
					data2 |= tmp;
					remaining += tmpRemaining;
					remaining -= i32;

					assert remaining <= i32;

					tmpRemaining = parseInt(data2, Math.min(remaining+bitsToParse, i32));

					assert (remaining+bitsToParse <= 32) ? tmpRemaining == 0: true;

					remaining = Math.max(remaining + bitsToParse - i32, 0); // вносим поправку не bitsToParse так как оставшиеся биты могут быть пустыми
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
				remaining = parseInt(data2, bitsToParse);
			}

			remaining = (remaining == 0) ? i32: remaining;
		}

		if((remaining > 0) && (remaining < i32))
		{
			data2 <<= (i32 - remaining);
			remaining = parseInt(data2, remaining + bitsToParse - i32);
		}

		uData.sout.flush();

		finishProgress();

		logger.exiting(this.getClass().getName(),"uncompressInternal");
	}

*/
	/**
	 *
	 * @param code int для парсинга
	 *
	 * @return возвращает кол-во не оставшихся нераспаршеных битов в code.
	 * @throws IOException если при записи в поток, что-то пошло не так
	 */
	private int parseInt(int code) throws IOException
	{
		int remaining = Integer.SIZE;
		int ccode = code;
		while( (remaining > 0) && (decodedBytes < udata.sizeUncompressed) )
		{
			boolean found = false;
			for(int i = 0; i < udata.tree.codesList.size(); i++)
			{
				HFCode hfc = udata.tree.codesList.get(i);
				if(hfc.len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				//int mask = 0xFFFFFFFF << (Integer.SIZE - hfc.len);
				//int c = hfc.code << (Integer.SIZE - hfc.len); // готовим код для операции сравнения, делаем его выровненным по левому краю
				//int code2 = ccode & mask;         // оставляем только биты равные длине кода
				//code2 = code2 ^ c;            // XOR - если операнды равны то результат будет 0.
				int code2 = ccode >>> (Integer.SIZE - hfc.len);
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
			assert !((remaining > udata.tree.maxCodeLen) && (!found));

			if(!found) break;    // прерываем цикл, что бы докинулось еще битов в ccode

		}

		// небольшой hack, если закончили парсинг файла то возвращаем 0, что бы избежать дополнительных попыток допарсить остатки этого int
		return (decodedBytes < udata.sizeUncompressed)? remaining: 0; // возвращаем сколько осталось нераспаршеных битов из bitsToParse а не из всего int.
	}

	private void writeByte(int v) throws IOException
	{
		udata.sout.write(v);
		crc.update(v);
		decodedBytes++;
	}

	private void startProgress()
	{
		if(udata.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) udata.cb.start();
	}

	private void finishProgress()
	{
		if(udata.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) udata.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long encodedBytes)
	{
		if((udata.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) && (encodedBytes > threshold))
		{
			threshold += delta;
			udata.cb.heartBeat((int)(100*threshold/ udata.sizeCompressed));
		}
	}


}
