import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class RangeArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file
	private int[] symbols = {1,2,3,7,10,5};
	private long[] weights = {3,7,99,55,11,23};
	private long[] cumFreq;
	private final int[] symbol_to_freq_index = new int[256];

	@Override
	public void compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames); // that needs to be before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			File fl = new File(fr.fileName);
			int BUFFER = getOptimalBufferSize(fr.fileSize);
			InputStream sin = new BufferedInputStream(new FileInputStream(fl),BUFFER);

			InputStream sin1 = new BufferedInputStream(new FileInputStream(fl), BUFFER); // stream только для подсчета частот символов
			calcWeights(sin1);
			sin1.close();

			saveFreqs(sout);

			logger.info(String.format("Starting compression '%s' ...", fr.fileName));

			var cData = new RangeCompressData(sin, sout, fr.fileSize, cumFreq, symbol_to_freq_index);
			RangeCompressor c = new RangeCompressor(RangeCompressor.Strategy.RANGEBOTTOM);

			c.compress(cData);

			fr.compressedSize = cData.sizeCompressed;
			fr.CRC32Value = cData.CRC32Value;

			sin.close();

			logger.info(String.format("Compression '%s' done.", fr.fileName));
		}

		sout.close();

		updateHeaders(fh, arcFilename);
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			loadFreqs(sin); //здесь читаем таблицы symbols и cumFreq из потока

			OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), getOptimalBufferSize(fr.fileSize));

			logger.info(String.format("Extracting file '%s'...", fr.fileName));

			RangeUncompressor uc = new RangeUncompressor();
			var uData = new RangeUncompressData(sin, sout, fr.compressedSize, fr.fileSize, cumFreq, symbols);

			uc.uncompress(uData);

			if (uData.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

			sout.close();

			logger.info(String.format("Extracting '%s' done.", fr.fileName));
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	private void loadFreqs(InputStream in) throws IOException
	{
		var ds = new DataInputStream(in);

		int count = ds.readByte() + 1; // stored is an INDEX of last element in symbols to fit it in byte, that is why we are adding 1
		symbols = new int[count];
		cumFreq = new long[count + 1];

		for (int i = 0; i < count; i++)
		{
			symbols[i] = ds.readByte();
		}

		int type = ds.readByte(); // reading type of elements the following cumFreq array

		for (int i = 0; i < count + 1; i++)
		{
			switch (type)
			{
				case 1 -> cumFreq[i] = ds.readLong();
				case 2 -> cumFreq[i] = (long)ds.readInt();
				case 3 -> cumFreq[i] = (long)ds.readByte();
			}
		}
	}

	private void saveFreqs(OutputStream out) throws IOException
	{
		var ds = new DataOutputStream(out);

		ds.writeByte(symbols.length - 1); // saving INDEX of the last element in symbols because we cannot not have more than 256 symbols there
		for (int sym : symbols)
		{
			ds.writeByte((byte) sym);
		}

		int type = getFreqsType();

		ds.writeByte(type); // saving a type of elements in the next array (either byte, int or long)

		for (long l : cumFreq)
		{
			switch (type)
			{
				case 1 -> ds.writeLong(l);
				case 2 -> ds.writeInt((int) l);
				case 3 -> ds.writeByte((byte) l);
			}
		}
	}

	private int getFreqsType()
	{
		long max = 0;
		for (long l : cumFreq)
			if (max < l) max = l;

		if(max <= Byte.MAX_VALUE)
		{
			return 3; //saving as byte values
		}
		if(max < Integer.MAX_VALUE)
		{
			return 2; // saving as int values
		}
		return 1; // saving as long values
	}

	/**
	 * записываем в архив размер закодированного потока в байтах и lastBits для каждого файла в архиве
	 * @param fh Header with correct compressedSize, lastBits and CRC32Values
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
	@Override
	public void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(new File(arcFilename), "rw");

		var offsets = fh.getFieldOffsets();

		int pos = offsets.get(FHeaderOffs.InitialOffset);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			raf.seek(pos + offsets.get(FHeaderOffs.CRC32Value));
			raf.writeLong(fr.CRC32Value);
			raf.seek(pos + offsets.get(FHeaderOffs.compressedSize));
			raf.writeLong(fr.compressedSize);
			//raf.seek(pos + offsets.get("lastBits"));
			//raf.writeByte(fr.lastBits);
			pos = pos + offsets.get(FHeaderOffs.FileRecSize) + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}

	/**
	 * Читает весь файл и считает частоты встречаемости байтов в нем
	 * Для binary files считает частоты тоже корректно.
	 * *** Внимание - портит InputStream sin, передвигает его на конец файла *****
	 * @throws IOException если что-то произошло с потоком
	 */
	protected void calcWeights(InputStream sin) throws IOException
	{
		logger.entering(Main.class.getName(),"calcWeights");

		int BUF_SIZE = 100_000_000;  // если дали поток, то мы не знаем размер файла читаем кусками по BUF_SIZE (100М) тогда

		long[] freq = new long[256];
		byte[] buffer = new byte[BUF_SIZE];
		//CRC32 cc = new CRC32();

		int cntRead;
		do
		{
			cntRead = sin.read(buffer);
			if (cntRead == -1) break;
			//	cc.update(buffer, 0, cntRead); // параллельно считаем CRC32 файла
			for (int i = 0; i < cntRead; i++)
				freq[Byte.toUnsignedInt(buffer[i])]++; // обходим знаковость байта

		}while(cntRead == BUF_SIZE);

		//CRC32Value = cc.getValue(); // сохраняем значение CRC32 для последующего использования

		long sum = 0;
		long min = Long.MAX_VALUE, max = 0;
		int nonzero = 0;
		for (long l : freq)
		{
			sum += l;
			if(l > 0) nonzero++;
			if(l > 0) min = Math.min(min, l);
			max = Math.max(max, l);
		}

		symbols = new int[nonzero];
		weights = new long[nonzero];

		int j = 0;
		for (int i = 0; i < freq.length; i++)
		{
			if(freq[i] > 0)
			{
				symbols[j] = i;
				weights[j] = freq[i];
				j++;
			}
		}

		// preparing data for compressor/uncompressor
		cumFreq = new long[weights.length + 1];
		cumFreq[0] = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumFreq[i+1] = cumFreq[i] + weights[i];
		}
		logger.finer(String.format("freq=%s", Arrays.toString(cumFreq)));

		for (int i = 0; i < symbols.length; i++)
		{
			symbol_to_freq_index[symbols[i]] = i + 1;
		}

		logger.finer(String.format("symbols=%s", Arrays.toString(symbols)));
		logger.finer(String.format("weights=%s",Arrays.toString(weights)));
		logger.finer(String.format("min weight=%d, max weight=%d", min, max));
		logger.finer(String.format("nonzero=%d, sum(size)=%d", nonzero, sum));

		logger.exiting(Main.class.getName(),"calcWeights");
	}

}


class DataStat
{
	int[] symbols;
	long[] weights;
	long[] cumFreq;
	int[] symbol_to_freq_index;
}