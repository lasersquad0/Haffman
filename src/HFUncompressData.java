import java.io.InputStream;
import java.io.OutputStream;

public class HFUncompressData {
	/*
1. Размер сжатого файла — 8 байт (long) - задает размер данных в пункте 5.
2  Кол-во записанных bits в последнем int закодированного потока
3. Размер таблицы символов — 2 байта
4. Таблица символов для раскодировки
5. Сжатые данные — сколько уже получится байт
*/
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // may be to compare with original size
	long CRC32Value;
	byte lastBits;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public HFUncompressData(InputStream sin, OutputStream sout, long compressedSize, byte lastBits)
	{
		this.sin = sin;
		this.sout = sout;
		sizeCompressed = compressedSize;
		this.lastBits = lastBits;
	}
}

class HFCompressData {
	/*
1. Размер сжатого файла — 8 байт (long) - задает размер данных в пункте 5.
2  Кол-во записанных bits в последнем int закодированного потока
3. Размер таблицы символов — 2 байта
4. Таблица символов для раскодировки
5. Сжатые данные — сколько уже получится байт
*/
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // to show compress progress
	byte lastBits;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public HFCompressData(InputStream sin, OutputStream sout)
	{
		this.sin = sin;
		this.sout = sout;
	}
}

class HFCallback
{
	public void compressPercent(int percent)
	{
		System.out.print("\r");
		System.out.printf("Progress %4d%%...", percent);
	}

	public void uncompressPercent(int percent)
	{
		System.out.print("\r");
		System.out.printf("Progress %4d%%...", percent);
	}
}
