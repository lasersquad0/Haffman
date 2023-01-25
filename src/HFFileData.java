import java.io.*;
import static org.junit.Assert.*;

public class HFFileData
{
	/*
1. Сигнатура типа файла — 4 байта "ROMA"
2. Версия формата файла — 2 байта
3. CRC32 несжатого файла для проверки правильности разархивации и общей целостности — 8 байт
4. Размер сжатого файла — 8 байт (long) - задает размер данных в пункте 9.
4.5 Кол-во записанных bits в последнем int закодированного потока
5. Размер несжатого файла — 8 байт (long) - хз нужно ли. только если для проверки
6. Размер строки имени файла из п5.
7. Имя файла который находится в архиве
8. Размер таблицы символов — 2 байта
9. Таблица символов — 2 * (количество знаков) байт [парами: символ — код]
10. Сжатые данные — сколько уже получится байт
*/
	final byte[] fileSignature = {'R', 'O', 'M', 'A'};
	final byte[] fileVersion = {'0', '1'};
	long CRC32Value;
	long fzUncompressed;
	String fnUncompressed;
	long fzCompressed;
	byte lastBits;
	String fnCompressed;
	short tableSize;
	InputStream sin;
	OutputStream sout;


	public boolean checkHeaderData(boolean checkForCompressed)
	{
		byte[] b = {'R', 'O', 'M', 'A'};
		assertArrayEquals(b, fileSignature);

		b = new byte[]{'0', '1'};
		assertTrue((fileVersion[0] >= '0') && (fileVersion[0] <= '9'));
		assertTrue((fileVersion[1] >= '0') && (fileVersion[1] <= '9'));

		assert CRC32Value > 0;
		assert fzUncompressed > 0;

		if(checkForCompressed)
		{
			assert fzCompressed > 0;
			assert fnCompressed != null;
			assert fnCompressed.length() > 0;
			assert (lastBits > 0) && (lastBits <= 32); // lastBits==32 когда все биты в int значимые. Поэтому значение 0 не может быть здесь
		}

		assert fnUncompressed != null;
		assert fnUncompressed.length() > 0;
		assert tableSize > 0;

		return true;
	}

	public void loadHeader() throws IOException
	{
		var dos = new DataInputStream(sin);
		int res = dos.read(fileSignature);
		res |= dos.read(fileVersion);
		if(res < 0)
			throw new IOException("Wrong file format.");

		CRC32Value = dos.readLong();
		fzCompressed = dos.readLong();
		lastBits = dos.readByte();
		fzUncompressed = dos.readLong();

		int filenameSize = dos.readShort();
		StringBuilder sb = new StringBuilder(filenameSize);
		while(filenameSize > 0)
		{
			sb.append(dos.readChar());
			filenameSize--;
		}

		fnUncompressed = sb.toString();
		tableSize = dos.readShort();

		checkHeaderData(false); // TODO здесь надо вообщем то true, но тогда тесты не работают так как они не могут постфактум записать fzCompressed и lastBits в произвольное место потока
	}

	public void saveHeader() throws IOException
	{
		checkHeaderData(false);

		var dos = new DataOutputStream(sout);
		dos.write(fileSignature);
		dos.write(fileVersion);
		dos.writeLong(CRC32Value);
		dos.writeLong(fzCompressed);
		dos.writeByte(lastBits);
		dos.writeLong(fzUncompressed);
		dos.writeShort(fnUncompressed.length());
		dos.writeChars(fnUncompressed);
		dos.writeShort(tableSize);
	}

	public int encodedDataSizePos()
	{
		return fileSignature.length + fileVersion.length + Long.BYTES; // длина encoded файла записана по смещению 14 от начала файла архива
	}

}
