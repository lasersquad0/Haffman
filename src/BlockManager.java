import java.io.*;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class BlockManager
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	public static final int BLOCK_HEADER_SIZE = 3*Integer.BYTES + Byte.BYTES; // size of fields: Compressed+Uncompressed+Line+flags
	private final int MAX_BLOCK_MEMORY_USAGE = 500_000_000; // 500M
	private final int MAX_BBUF_COUNT = MAX_BLOCK_MEMORY_USAGE/2/Utils.BLOCK_SIZE;
	HashMap<Integer, Future<BWTTask>> tasks;
	static Stack<BWTTask> utasks; // Made static to reuse this array between different instances of BlockManager to reduce memory usage
	int loadBlockCounter;


	public void compressFileInThread(HFFileRec fr, OutputStream out, BlockCompressable compressor) throws IOException
	{
		File fl = new File(fr.origFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var bs = new ByteArrayOutputStream(Utils.BLOCK_SIZE);
		var cd = new CompressData(null, bs, fr.fileSize);

		if(utasks == null) utasks = new Stack<>();
		else if (!utasks.empty()) // check whether BLOCK_SIZE had changed between different calls of this method. If so, remove all cached BWTTasks since they have wrong blockSize
				if (Utils.BLOCK_SIZE != utasks.peek().data.getBlockSize()) utasks.clear();

		tasks = (tasks == null)? new HashMap<>(): tasks;

		compressor.startBlockCompressing(cd);

		ThreadPoolExecutor pool = Utils.getThreadPool();

		loadBlockCounter = 1;
		boolean moreBlocks = loadMoreBlocks(sin, pool);

		long sleepTime = 100; // millisec
		if(Utils.BLOCK_SIZE <= 1<<16) sleepTime = 5;
		if(Utils.BLOCK_SIZE > 1<<22) sleepTime = 500;

		int blockId = 1;
		while(true)
		{
			try
			{
				Thread.sleep(sleepTime);

				if( (sleepTime > 1) && (pool.getQueue().size() + pool.getActiveCount() == 0) ) // if main thread is bottleneck reduce sleeping time
				{
					sleepTime -= sleepTime / 2;
					logger.finer("[compressFileinThread] Main thread bottleneck detected. Reducing sleep time.");
				}
/*
				if( (sleepTime < 5000) && (wblocks.size() == MAX_BBUF_COUNT)) //if block buffer is full after sleeping, increase sleep time but not more than 5sec
				{
					sleepTime *= 2;
					logger.finer("[compressFileinThread] Long task queue detected. Increasing sleep time.");
				}*/
			}
			catch(InterruptedException e){
				logger.warning("Sleep call has been interrupted.");
			}

			if(moreBlocks && ((blockId % (MAX_BBUF_COUNT>>>1)) == 0) ) // load more blocks only after half of blocks have been compressed
				moreBlocks = loadMoreBlocks(sin, pool);

			if(tasks.size() == 0) break; // попробовали загрузить блоки и если после этого список пуст значит работа закончена

			Future<BWTTask> ft = tasks.get(blockId);
			assert ft != null;

			if(ft.isDone())
			{
				BWTTask bwt = null;
				try { bwt = ft.get();}
				catch (InterruptedException | ExecutionException e)
				{
					throw new RuntimeException(e);
				}

				compressor.compressBlock(bwt.data);

				assert bwt.data.cBlockSize == bs.size();

				//if (wd.data.cBlockSize > wd.data.srcblock.length) // in some rare cases it is FALSE (when block size is small). making sure that block is really compressed.
				//	logger.info(String.format("[compressFile] Compressed block#%d is larger than initial block (compressed %d, initial %d)", fr.blockCount, wd.data.cBlockSize, wd.data.srcblock.length));

				logger.finer(String.format("[compressFileinThread] Compressed block size %d, block #%d", bwt.data.cBlockSize, fr.blockCount));
				logger.finer("[compressFileinThread] Current task count in a queue:" + pool.getQueue().size() + "  " + pool.getActiveCount());

				saveBlock(out, bs, bwt.data);

				fr.blockCount++;

				tasks.remove(blockId); // block is fully processed, remove it
				utasks.push(ft.resultNow()); // adding task to the list of unused tasks
				blockId++;
			}

		}

		//pool.shutdown();

		assert tasks.size() == 0;
		logger.finer("[compressFileinThread] utasks size after compressing file:" + utasks.size());

		compressor.finishBlockCompressing();

		sin.close();

		fr.compressedSize = cd.sizeCompressed;
		fr.CRC32Value = cd.CRC32Value;
	}

	private boolean loadMoreBlocks(InputStream sin, ExecutorService pool) throws IOException
	{
		int bcount = tasks.size();
		if(bcount >= MAX_BBUF_COUNT) return true;

		logger.finer("[loadMoreBlocks] Loading blocks ...");

		int save = bcount;
		int newBlocks = 0;
		int reusedBlocks = 0;
		boolean moreBlocks = true;
		BWTTask bwt;
		while(bcount < MAX_BBUF_COUNT) // кол-во блоков вычисляем так что бы массив блоков BlockCoderData занимал в памяти не больше 1G
		{
			if(utasks.empty())
			{
				bwt = new BWTTask();
				newBlocks++;
			}
			else
			{
				bwt = utasks.pop();
				reusedBlocks++;
			}

			int cntRead = bwt.data.readFrom(sin);
			moreBlocks = cntRead == bwt.data.getBlockSize(); // means 'no more blocks can be loaded
			if(cntRead == -1) break; // if read bytes less than blockSize it means there will be no more blocks

			Future<BWTTask> fut = pool.submit(bwt, bwt);

			tasks.put(loadBlockCounter, fut);
			loadBlockCounter++;
			bcount++;

			if(!moreBlocks) break;
		}

		logger.finer("[loadMoreBlocks] Blocks loaded: "+ (bcount-save));
		logger.finer("[loadMoreBlocks] NEW blocks created: " + newBlocks);
		logger.finer("[loadMoreBlocks] REUSED blocks: " + reusedBlocks);

		return moreBlocks; // true means 'there are more blocks can be loaded'
	}


	public void compressFile(HFFileRec fr, OutputStream out, BlockCompressable compressor) throws IOException
	{
		File fl = new File(fr.origFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var bs = new ByteArrayOutputStream(Utils.BLOCK_SIZE);
		var cd = new CompressData(null, bs, fr.fileSize);
		var transformer = new TransformSeq();
		var data = new BlockCoderData();
		data.allocate(Utils.BLOCK_SIZE);

		compressor.startBlockCompressing(cd);

		while( (data.bytesInBlock = sin.read(data.srcblock, 0, data.getBlockSize())) != -1 )
		{
			//boolean lastBlock = data.bytesInBlock != data.getBlockSize();

			transformer.doTransformEncode(data);

			compressor.compressBlock(data);
			//Utils.saveBuf(bs, "ttt/Compressed" + fr.blockCount + ".txt");

			assert data.cBlockSize == bs.size();

			if(data.cBlockSize > data.srcblock.length) // in some rare cases it is FALSE (when block size is small). making sure that block is really compressed.
				logger.info(()->String.format("[compressFile] Compressed block#%d is larger than initial block (compressed %d, initial %d)",fr.blockCount,data.cBlockSize,data.srcblock.length));

			logger.finer(()->String.format("[compressFile] Compressed block size %d, block# %d",data.cBlockSize,fr.blockCount));

			saveBlock(out, bs, data);
			fr.blockCount++;
		}

		compressor.finishBlockCompressing();

		out.flush();
		sin.close();

		fr.compressedSize = cd.sizeCompressed;
		fr.CRC32Value = cd.CRC32Value;
	}

	public void uncompressFile(HFFileRec fr, CompressData udata, BlockUncompressable uncompressor) throws IOException
	{
		var data = new BlockCoderData();
		data.allocate(fr.blockSize);

		InputStream in = udata.sin;
		var bs = new ByteArrayInputStream2(data.srcblock);
		//var cd = new CompressData(bs, null, fr.compressedSize, fr.fileSize);
		udata.sin = bs;
		var transformer = new TransformSeq();

		uncompressor.startBlockUncompressing(udata);

		int bcnt = fr.blockCount;
		while(bcnt-- > 0)
		{
			int blockNum = fr.blockCount - bcnt - 1;

			loadBlock(in, data);
			bs.setNewBuff(data.srcblock); // put new buf to the stream to make sure that uncompressBlock always uses right data to uncompress (because .swapBuffers() may be called odd number of times)

			logger.finer(()->String.format("[uncompressFile] cBlockSize=%d, uBlocSize=%d, block# %d",data.cBlockSize,data.uBlockSize,blockNum));

			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/OrigCompressed" + blockNum + ".txt");
			bs.resetAndSize(data.bytesInBlock);
			uncompressor.uncompressBlock(data); // saves uncompressed data into via data.writeDest()
			data.swapBuffers();
			data.bytesInBlock = data.uBlockSize; // setting proper value for bytesInBlock after uncompression because size of block changes
			//Utils.saveBuf(data.srcblock, data.bytesInBlock, "ttt/BeforeMTF" + blockNum + ".txt");

			transformer.doTransformDecode(data);

			udata.sout.write(data.srcblock, 0, data.bytesInBlock);
		}

		//sout.close();

		uncompressor.finishBlockUncompressing();

	}

	private void saveBlock(OutputStream out, ByteArrayOutputStream cblock, BlockCoderData dt) throws IOException
	{
		//if(cblock.size() < 1024)  // it's weird when block size is less than 1K
		//	logger.warning(()->String.format("[saveBlock] WEIRD, block size is less than 1024 bytes (%d)", cblock.size()));

		assert dt.cBlockSize > 0;
		//assert dt.uBlockSize > 0;
		assert dt.bytesInBlock > 0;
		assert dt.bwtLineNum >= 0;
		//assert dt.bflags == 0;

		Utils.writeInt(out, dt.cBlockSize);
		Utils.writeInt(out, dt.bytesInBlock); // uncompressed size
		Utils.writeInt(out, dt.bwtLineNum);
		out.write(dt.bflags); // block bit flags
		cblock.writeTo(out);
		cblock.reset(); // clear byte array stream for the next block
	}

	/**
	 *
	 * @param in
	 * @return returns true if buffers in 'data' were reallocated, so, upper method may require to do additional actions for that case.
	 * @throws IOException in case of an error
	 */
	private boolean loadBlock(InputStream in, BlockCoderData dt) throws IOException
	{
		dt.cBlockSize = Utils.readInt(in);
		dt.uBlockSize = Utils.readInt(in);
		dt.bwtLineNum = Utils.readInt(in);
		dt.bflags = (byte)in.read(); // block bit flags

		assert dt.cBlockSize > 0;
		assert dt.uBlockSize > 0;
		assert dt.bwtLineNum >= 0;

		boolean bufRealloc = false;
		int maxSize = Math.max(dt.uBlockSize, dt.cBlockSize); // sometimes cBlockSize can be larger than uBlockSize
		if(maxSize > dt.getBlockSize())
		{
			logger.info(()->String.format("[loadBlock] Reallocating block size. New size:%d", maxSize));
			dt.reAllocate(maxSize);
			bufRealloc = true;
		}

		assert dt.bwtLineNum < dt.getBlockSize(); // WARNING! This assert should be AFTER possible block relocate statement.

		int bytesRead = in.read(dt.srcblock,0, dt.cBlockSize);
		dt.bytesInBlock = dt.cBlockSize;
		assert bytesRead > 0;
		assert bytesRead == dt.cBlockSize;

		//if(dt.cBlockSize < 1024)  // it's weird when block size is less than 1K
		//	logger.warning(()->String.format("[loadBlock] WEIRD, block size is less than 1024 bytes (%d)", dt.cBlockSize));

		return bufRealloc;
	}

}
