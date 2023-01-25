import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class HFUncompressor
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	HFTree tree;
	HFFileData fileData;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;


	public void uncompress(HFTree tree, HFFileData fileData) throws IOException
	{
		decodedBytes = 0;
		this.tree = tree;
		this.fileData = fileData;
		uncompressInternal();

		long v = crc.getValue();
		if(fileData.CRC32Value != v)
			logger.warning(String.format("CRC values are not equal: %d and %d", fileData.CRC32Value, v));
	}

	private void uncompressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"uncompressInternal");

		final int i32 = Integer.SIZE; // просто для удобства чтения
		int bitsToParse = i32;
		long encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = Integer.SIZE;

		while (encodedBytes < fileData.fzCompressed) // заканчиваем раскодировать как только кончились байты
		{
			// вот так хитро читаем int из потока
			int ch1 = fileData.sin.read();
			int ch2 = fileData.sin.read();
			int ch3 = fileData.sin.read();
			int ch4 = fileData.sin.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0)
				break;

			encodedBytes +=4;   // считаем сколько encoded bytes прочитали и потока и сравниваем с размером fileSizeComp что бы вовремя остановиться.

			if(encodedBytes >= fileData.fzCompressed) // вычитали последний int который нужно парсить не полностью
			{
				assert fileData.lastBits <= Integer.SIZE;
				bitsToParse = fileData.lastBits;
			}

			data =  ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

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

		fileData.sout.flush();

		logger.exiting(this.getClass().getName(),"uncompressInternal");
	}

	private int parseInt(int code) throws IOException
	{
		return parseInt(code, Integer.SIZE);
	}

	/**
	 *
	 * @param code int для парсинга
	 *
	 * @return возвращает кол-во не оставшихся нераспаршеных битов в code.
	 * @throws IOException если при записи в поток, что-то пошло не так
	 */
	private int parseInt(int code, int bitsToParse) throws IOException
	{
		int remaining = bitsToParse;
		int ccode = code;
		while(remaining > 0 /*Math.max(tree.minCodeLen, Integer.SIZE - bitsToParse)*/)
		{
			boolean found = false;
			for(int i = 0; i < tree.codesList.size(); i++)
			{
				HFCode hfc = tree.codesList.get(i);
				if(hfc.len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				int mask = 0xFFFFFFFF << (Integer.SIZE - hfc.len);
				int c = hfc.code << (Integer.SIZE - hfc.len); // готовим код для операции сравнения, делаем его выровненным по левому краю
				int code2 = ccode & mask;         // оставляем только биты равные длине кода
				code2 = code2 ^ c;            // XOR - если операнды равны то результат будет 0.
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
			assert !((remaining > tree.maxCodeLen) && (!found));

			if(!found) break;    // прерываем цикл, что бы докинулось еще битов в ccode

		}

		//assert (remaining - (Integer.SIZE - bitsToParse)) > 0;
		return remaining;// - (Integer.SIZE - bitsToParse); // возвращаем сколько осталось нераспаршеных битов из bitsToParse а не из всего int.
	}

	private void writeByte(int v) throws IOException
	{
		fileData.sout.write(v);
		crc.update(v);
		decodedBytes++;
	}


}
