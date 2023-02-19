import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class RangeArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	static final Utils.CompressorTypes COMPRESSOR_CODE = Utils.CompressorTypes.ARITHMETIC;
	private final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file
	private int[] symbols = {1,2,3,7,10,5};
	private long[] weights = {3,7,99,55,11,23};
	private long[] cumFreq = null;
	private final int[] symbol_to_freq_index = new int[256];

	@Override
	public void compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Arithmetic Range compression algorithm.");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames, COMPRESSOR_CODE); // that needs stay before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			logger.info(String.format("Analysing file '%s'.", fr.origFilename));

			File fl = new File(fr.origFilename); // возможно хранить File в HFFileRec потому как fillFileRecs тоже создаются File
			int BUFFER = Utils.getOptimalBufferSize(fr.fileSize);
			InputStream sin = new BufferedInputStream(new FileInputStream(fl),BUFFER);

			InputStream sin1 = new BufferedInputStream(new FileInputStream(fl), BUFFER); // stream только для подсчета частот символов
			calcWeights(sin1);
			rescaleWeights(RangeCompressor64.BOTTOM); //*****
			sin1.close();

			saveFreqs(sout);

			logger.info("Starting compression...");

			var cData = new RangeCompressData(sin, sout, fr.fileSize, cumFreq, weights, symbol_to_freq_index);
			//var c = new RangeCompressor32(RangeCompressor32.Strategy.RANGEBOTTOM);//*******
			var c = new RangeCompressor64();

			c.compress(cData);

			fr.compressedSize = cData.sizeCompressed;
			fr.CRC32Value = cData.CRC32Value;

			sin.close();

			printCompressionDone(fr);
		}

		sout.close();

		fh.updateHeaders(arcFilename); // it is important to save info into files table that becomes available only after compression

		logger.info("All files are processed.");
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			unCompressFile(fr, sin);
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	@Override
	public void unCompressFile(HFFileRec fr, InputStream sin) throws IOException
	{
		logger.info(String.format("Extracting file '%s'...", fr.fileName));

		loadFreqs(sin); //здесь читаем таблицы symbols и cumFreq из потока

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), Utils.getOptimalBufferSize(fr.fileSize));

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(sin, sout, fr.compressedSize, fr.fileSize, cumFreq, symbols);

		uc.uncompress(uData);

		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();

		logger.info(String.format("Extracting '%s' done.", fr.fileName));
	}

	private void loadFreqs(InputStream in) throws IOException
	{
		var ds = new DataInputStream(in);

		int count = Byte.toUnsignedInt(ds.readByte()) + 1; // stored is an INDEX of last element in symbols to fit it in byte, that is why we are adding 1
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
	/*
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

			// calculate pos of the next file record, for the next loop iteration
			pos = pos + offsets.get(FHeaderOffs.FileRecSize) + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}
*/
	protected void rescaleWeights(long bottom)
	{
		if (bottom > cumFreq[cumFreq.length - 1])
			return; // no scaling needed

		long SummFreq;
		do
		{
			SummFreq = 0;
			for (int i = 0; i < weights.length; i++)
			{
				weights[i] -= (weights[i] >> 1); // this formula guarantees that cumFre[i] will not become zero.
				SummFreq += weights[i];
			}
		}while(SummFreq > bottom);

		// updating cumFreq
		cumFreq[0] = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumFreq[i + 1] = cumFreq[i] + weights[i];
		}
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