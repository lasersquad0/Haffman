import java.io.*;
import java.util.logging.Logger;

public class BlockManager
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	public static final int BLOCK_HEADER_SIZE = 3*Integer.BYTES + Byte.BYTES; // size of fields: Compressed+Uncompressed+Line+flags
	BlockCoderData data;
	BWTCoder bwt;
	MTFCoder mtf;

	BlockManager() { init(); }
	private void init()
	{
		if(data == null)
		{
			data = new BlockCoderData();
			data.allocate(Utils.BLOCK_SIZE);
			bwt = new BWTCoder();
			mtf = new MTFCoder();
		}
	}

	public void compressFile(HFFileRec fr, OutputStream out, BlockCompressable compressor) throws IOException
	{
		File fl = new File(fr.origFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var bs = new ByteArrayOutputStream(data.getBlockSize());
		var cd = new CompressData(null, bs, fr.fileSize);

		compressor.startBlockCompressing(cd);

		while( (data.bytesInBlock = sin.read(data.srcblock, 0, data.getBlockSize())) != -1 )
		{
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/Uncompressed" + fr.blockCount +".txt");
			bwt.encodeBlock(data);
			data.swapBuffers();
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/AfterBWT" + fr.blockCount +".txt");
			mtf.encodeBlock(data);
			data.swapBuffers();
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/AfterMTF" + fr.blockCount + ".txt");
			compressor.compressBlock(data);
			//Utils.saveBuf(bs, "ttt/Compressed" + fr.blockCount + ".txt");

			assert data.cBlockSize == bs.size();

			if(data.cBlockSize > data.srcblock.length) // in some rare cases it is FALSE (when block size is small). making sure that block is really compressed.
				logger.info(()->String.format("[compressFile] Compressed block#%d is larger than initial block (compressed %d, initial %d)",fr.blockCount,data.cBlockSize, data.srcblock.length));

			logger.finer(()->String.format("[compressFile] Compressed block size %d, block# %d", data.cBlockSize, fr.blockCount));
			saveBlock(out, bs);
			fr.blockCount++;
		}

		compressor.finishBlockCompressing();

		sin.close();

		fr.compressedSize = cd.sizeCompressed;
		fr.CRC32Value = cd.CRC32Value;
	}

	public void uncompressFile(HFFileRec fr, InputStream sin, OutputStream sout, BlockUncompressable uncompressor) throws IOException
	{
		var bs = new ByteArrayInputStream2(data.srcblock);
		var cd = new CompressData(bs, null, fr.compressedSize, fr.fileSize);

		uncompressor.startBlockUncompressing(cd);

		int bcnt = fr.blockCount;
		while(bcnt-- > 0)
		{
			assert bs.getBuff() == data.srcblock;

			int blockNum = fr.blockCount - bcnt - 1;

			if(loadBlock(sin))
				bs.setNewBuff(data.srcblock); // put new buf to the stream because old buf reference became invalid after rellocation

			logger.finer(()->String.format("[uncompressFile] cBlockSize=%d, uBlocSize=%d, block# %d", data.cBlockSize,data.uBlockSize, blockNum));

			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/OrigCompressed" + blockNum + ".txt");
			bs.resetAndSize(data.bytesInBlock);
			uncompressor.uncompressBlock(data);
			data.bytesInBlock = data.uBlockSize; // setting proper value for bytesInBlock after uncompression because size of block changes
			data.swapBuffers();
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/BeforeMTF" + blockNum + ".txt");
			mtf.decodeBlock(data);
			data.swapBuffers();
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/BeforeBWT" + blockNum + ".txt");
			bwt.decodeBlock(data);
			//Utils.saveBuf(data.destblock, data.bytesInBlock, "ttt/OrigUncompressed" + blockNum + ".txt");
			sout.write(data.destblock, 0, data.bytesInBlock);
			//swapBuffers(data); // temporary need to be commented out!!!
		}

		sout.close();
		uncompressor.finishBlockUncompressing();
	}

	private void saveBlock(OutputStream out, ByteArrayOutputStream cblock) throws IOException
	{
		//if(cblock.size() < 1024)  // it's weird when block size is less than 1K
		//	logger.warning(()->String.format("[saveBlock] WEIRD, block size is less than 1024 bytes (%d)", cblock.size()));

		Utils.writeInt(out, data.cBlockSize);
		Utils.writeInt(out, data.bytesInBlock); // uncompressed size
		Utils.writeInt(out, data.bwtLineNum);
		out.write(data.bflags); // block bit flags
		cblock.writeTo(out);
		cblock.reset(); // clear byte array stream for the next block
	}

	/**
	 *
	 * @param in
	 * @return returns true if buffers in 'data' were reallocated, so, upper method may require to do additional actions for that case.
	 * @throws IOException in case of an error
	 */
	private boolean loadBlock(InputStream in) throws IOException
	{
		data.cBlockSize = Utils.readInt(in);
		data.uBlockSize = Utils.readInt(in);
		assert data.cBlockSize > 0;
		assert data.uBlockSize > 0;

		data.bwtLineNum = Utils.readInt(in);
		data.bflags = (byte)in.read(); // block bit flags
		assert data.bflags == 0;
		//assert data.bwtLineNum >= 0;
		//assert data.bwtLineNum < data.getBlockSize();

		boolean bufRealloc = false;
		if(data.cBlockSize > data.getBlockSize())
		{
			logger.warning(()->String.format("[loadBlock] Reallocating block size. New size: (%d)", data.cBlockSize * 6 / 5));
			data.allocate(data.cBlockSize * 6 / 5); // allocate 20% more space than read block (to reduce re-allocations)
			bufRealloc = true;
		}

		int bytesRead = in.read(data.srcblock,0, data.cBlockSize);
		data.bytesInBlock = data.cBlockSize;
		assert bytesRead > 0;
		assert bytesRead == data.cBlockSize;

		if(data.cBlockSize < 1024)  // it's weird when block size is less than 1K
			logger.warning(()->String.format("[loadBlock] WEIRD, block size is less than 1024 bytes (%d)", data.cBlockSize));

		return bufRealloc;
	}

	/** Dest block after BWT should become src block for MFT
	 * @param data - block data to transform
	 */
/*	private void swapBuffers(BlockCoderData data)
	{
		byte[] temp = data.srcblock;
		data.srcblock = data.destblock;
		data.destblock = temp;
	}
*/
	public void compressFiles(HFArchiveHeader fh, OutputStream out, BlockCompressable compressor) throws IOException
	{
		init();

		for (HFFileRec fr: fh.files)
		{
			compressFile(fr, out, compressor);
		}
	}

	public void uncompressFiles(HFArchiveHeader fh, InputStream in) throws IOException
	{
		init();

		for (HFFileRec fr: fh.files)
		{
			//uncompressFile(fr, in);
		}
	}

}
