import java.io.IOException;
import java.util.zip.CRC32;

public class AdaptHuffman extends Compressor implements BlockCompressable, BlockUncompressable
{
	private static class HNode
	{
		int up;      // next node up the tree
		int down;         // pair of down nodes
		int symbol;       // node symbol value
		long weight;       // node weight
		void fill(HNode t)
		{
			up = t.up;
			down = t.down;
			symbol = t.symbol;
			weight = t.weight;
		}
	}

	private final int ABC_SIZE = 256;
	int ESC;
	int root;
	int size;
	int[] map;
	HNode[] table;
	//CompressData cdata;
	private long encodedBytes;
	private long decodedBytes;
	private int blockNum;
	private CRC32 crc = new CRC32();


	AdaptHuffman()
	{
		initData();
	}

	private void initData()
	{
		// initialize an adaptive coder for alphabet size, and count of nodes to be used
		size = ABC_SIZE;
		root = size << 1;    // root учитывает еще и места для "групповых" nodes поэтому отводим всем node в 2 раза больше места чем алфавит.
		map = new int[ABC_SIZE]; // alphabet size is 256 symbols by default
		table = new HNode[root];
		root--;  // 'root' is an index that refers to the last item in 'table' array

		//  create the initial escape node
		//  at the tree root
		table[root] = new HNode();
		ESC = root;
		ArcBit = 0;
		ArcChar = 0;
		encodedBytes = 0;  // сбрасываем счетчики
		decodedBytes = 0;
		blockNum = 0;
	}

	@Override
	public void startBlockCompressing(CompressData cData)
	{
		startCompressing(cData);
		startProgress((cdata.sizeUncompressed - 1)/Utils.BLOCK_SIZE + 1);
	}

	@Override
	public void finishBlockCompressing()
	{
		finishCompressing();
	}

	@Override
	public void startBlockUncompressing(CompressData cData)
	{
		startCompressing(cData);
		startProgress((cdata.sizeUncompressed - 1)/Utils.BLOCK_SIZE + 1);
	}

	@Override
	public void finishBlockUncompressing()
	{
		finishProgress();
	}

	private void finishCompressing()
	{
		finishProgress();

		cdata.sizeCompressed = encodedBytes;
		cdata.CRC32Value = crc.getValue();
	}

	private void startCompressing(CompressData cData)
	{
		this.cdata = cData;
		initData();
	}

	@Override
	public void compressBlock(BlockCoderData data) throws IOException
	{
		long save = encodedBytes;

		updateProgress(blockNum++);

		for (int i = 0; i < data.bytesInBlock; i++)
		{
			int sym = Byte.toUnsignedInt(data.srcblock[i]);
			crc.update(sym);

			encodeSymbol(sym);
		}

		writeLastBits(); // write partial bits for every block since they needed to decode last bytes of that block

		data.cBlockSize = (int)(encodedBytes - save);
	}

	@Override
	public void uncompressBlock(BlockCoderData data) throws IOException
	{
		resetBitCache(); // decode each block from scratch. because partial bits are written as a last byte of previous block.
		data.resetDest();

		updateProgress(blockNum++);

		for (int i = 0; i < data.uBlockSize; i++)
		{
			int symbol = decodeSymbol();
			crc.update(symbol);
			data.writeDest(symbol);
			//decodedBytes++;
		}
	}

	public void compress(CompressData cData) throws IOException
	{
		startCompressing(cData);
		startProgress(cdata.sizeUncompressed);

		long total = 0;
		int ch;
		while( (ch = cdata.sin.read()) != -1 )
		{
			crc.update(ch);
			updateProgress(total++);

			encodeSymbol(ch);
		}

		writeLastBits();

		cdata.sout.flush();

		finishCompressing();
	}

	public void uncompress(CompressData uData) throws IOException
	{
		startCompressing(uData);
		startProgress(cdata.sizeUncompressed);

		while(decodedBytes < cdata.sizeUncompressed)
		{
			updateProgress(decodedBytes);

			int symbol = decodeSymbol();
			crc.update(symbol);
			cdata.sout.write(symbol);
			decodedBytes++;
		}

		cdata.sout.flush();

		finishProgress();

		cdata.CRC32Value = crc.getValue();
	}

	// split escape node to incorporate new symbol
	private int split(int symbol)
	{
		int pair, node;

		pair = ESC;
		node = --ESC;
		table[node] = new HNode();

		// is the tree already full???
		/*if (pair > 0)    // TODO возможно эта проверка не нужна так как у нас алфавит всегда полный - 256
		{
			esc--;
			table[esc] = new HNode();
		} else
			return 0;*/

		// if this is the last symbol, it moves into
		// the escape node's old position, and
		// huff->esc is set to zero.

		// otherwise, the escape node is promoted to
		// parent a new escape node and the new symbol.

		if (node > 0)
		{
			table[pair].down = node;
			table[pair].weight = 1;
			table[node].up = pair;
			ESC--;
			table[ESC] = new HNode();
		} else
		{
			pair = 0;
			node = 1;
		}

		//  initialize the new symbol node
		table[node].symbol = symbol;
		table[node].weight = 0;
		table[node].down = 0;
		map[symbol] = node;

		//  initialize a new escape node.
		table[ESC].weight = 0;
		table[ESC].down = 0;
		table[ESC].up = pair;

		return node;
	}

	//  swap leaf to group leader position
	//  return symbol's new node
	private int leader(int node)
	{
		long weight = table[node].weight;
		int leader = node, prev, symbol;

		while (weight == table[leader + 1].weight)
			leader++;

		if (leader == node)
			return node;

		// swap the leaf nodes
		symbol = table[node].symbol;
		prev = table[leader].symbol;

		table[leader].symbol = symbol;
		table[node].symbol = prev;
		map[symbol] = leader;
		map[prev] = node;

		return leader;
	}

	HNode swap = new HNode();

	//  slide internal node up over all leaves of equal weight;
	//  or exchange leaf with next smaller weight internal node
	//  return node's new position
	private int slide(int node)
	{
		int next = node;
		next++;

		swap.fill(table[node]);

		// if we're sliding an internal node, find the
		// highest possible leaf to exchange with
		if ((swap.weight & 1) > 0)   // если есть несколько подряд меньшего веса чем swap то ищется самая последняя из нихи swap меняется с ней
			while (swap.weight > table[next + 1].weight)
				next++;

		//  swap the two nodes
		table[node].fill(table[next]);
		table[next].fill(swap);

		table[next].up = table[node].up;
		table[node].up = swap.up;

		//  repair the symbol map and tree structure
		if ((swap.weight & 1) > 0)
		{
			table[swap.down].up = next;
			table[swap.down - 1].up = next;
			map[table[node].symbol] = node;
		}
		else
		{
			table[table[node].down - 1].up = node;
			table[table[node].down].up = node;
			map[swap.symbol] = next;
		}

		return next;
	}

	// increment symbol weight and re-balance the tree.
	private void increment(int node)
	{
		int up;

		// obviate swapping a parent with its child:
		//    increment the leaf and proceed
		//    directly to its parent.

		//  otherwise, promote leaf to group leader position in the tree

		if (table[node].up == node + 1)
		{
			table[node].weight += 2;
			node++;
		}
		else
		{
			node = leader(node);
		}

		//  increase the weight of each node and slide
		//  over any smaller weights ahead of it
		//  until reaching the root

		//  internal nodes work upwards from
		//  their initial positions; while
		//  symbol nodes slide over first,
		//  then work up from their final
		//  positions.

		table[node].weight += 2; // это выражение стояло в while - здесь оно раздвоилось - перед циклом и в конце первого while.
		while ((up = table[node].up) > 0)  // идем по дереву вверх и инкрементируем веса. huff_slide может наверное чуть попутать нас так как он переставляет местами ноды.
		{
			while(table[node].weight > table[node + 1].weight)
				node = slide(node);

			if( (table[node].weight & 1) > 0) // внутренняя нода
				node = up;
			else
				node = table[node].up;  // внешняя - leaf

			table[node].weight += 2; // возможно не будет работать правильно так как это выражение стояло в первом while.
		}
	}

	//  scale all weights and rebalance the tree
	//  zero weight nodes are removed from the tree
	//  by sliding them out the left of the rank list
	private void scale(int bits)
	{
		int node = ESC, prev;
		long weight;

		//  work up the tree from the escape node
		//  scaling weights by the value of bits

		while(++node <= root)
		{
			//  recompute the weight of internal nodes;
			//  slide down and out any unused ones
			if ((table[node].weight & 1) > 0)
			{
				if ((weight = (table[table[node].down].weight & ~1)) > 0)
					weight += table[table[node].down - 1].weight | 1;

				//  remove zero weight leaves by incrementing HuffEsc
				//  and removing them from the symbol map.  take care

			} else if ((weight = (table[node].weight >> bits & ~1)) == 0)
			{
				map[table[node].symbol] = 0;
				if (ESC++ > 0) ESC++;  // TODO странный 2х разовый инкремент esc
			}

			// slide the scaled node back down over any
			// previous nodes with larger weights

			table[node].weight = weight;
			prev = node;

			while (weight < table[--prev].weight)
				slide(prev);
		}

		// prepare a new escape node
		table[ESC].down = 0;
	}

	//  send the bits for an escaped symbol
	private void sendId(int symbol) throws IOException
	{
		int empty = 0, max;

		//  count the number of empty symbols
		//  before the symbol in the table
		while (symbol-- > 0)           // видимо хитрость для маленьких алфавитов. сохраняем число которое меньше чем (byte)Symbol занимает меньше бит но его мы можем восстановить при раскодировании
			if (map[symbol] == 0)
				empty++;

		//  send LSB of this count first, using // LSB - это least significant bit
		//  as many bits as are required for
		//  the maximum possible count

		if ((max = size - (root - ESC) / 2 - 1) > 0) // здесь пока непонятно что считается этой формулой, почему бы просто не записать в файл emmpty
			do
			{
				writeBit(empty & 1);
				empty >>= 1;         // кстати empty здесь записывается в файл в обратном порядке битов
			}
			while ((max >>= 1) > 0);
	}

	// encode the next symbol
	public void encodeSymbol(int symbol) throws IOException
	{
		int emit = 1, bit;
		int up, idx, node;

		node = map[symbol];

		/*if (symbol < size)   // TODO снова проверки на размер алфавита, возможно нужно убрать
			node = map[symbol];
		else
			return;*/

		// for a new symbol, direct the receiver to the escape node
		// but refuse input if table is already full.
		if ((idx = node) == 0)
			if ((idx = ESC) == 0)
				return;

		//  accumulate the code bits by working up the tree from the node to the root
		while ((up = table[idx].up) > 0)  // идем по дереву вверх и собираем биты для Hf кода.
		{
			emit <<= 1;
			emit |= (idx & 1);
			idx = up;
		}

		//  send the code, root selector bit first
		bit = emit & 1;  // TODO проверить правильно ли работает так как эта операция стояла внутри while. при переводе в java она раздвоилась
		while ((emit >>= 1) > 0)  // переворачиваем биты и посылаем на выход пачкой когда соберется их 8
		{
			writeBit(bit);
			bit = emit & 1;
		}

		//  send identification and incorporate new symbols into the tree
		if (node == 0)
		{
			sendId(symbol);
			node = split(symbol);
		}

		//  adjust and re-balance the tree
		increment(node);
	}

	// read the identification bits for an escaped symbol
	private int readId() throws IOException
	{
		int empty = 0, bit = 1, max, symbol;

		//  receive the symbol, LSB first, reading
		//  only the number of bits necessary to
		//  transmit the maximum possible symbol value

		if ((max = size - (root - ESC) / 2 - 1) > 0)
			do
			{
				empty |= readBit() > 0 ? bit : 0;
				bit <<= 1;
			}
			while ((max >>= 1) > 0);

		//  the count is of unmapped symbols
		//  in the table before the new one

		for (symbol = 0; symbol < size; symbol++)
			if (map[symbol] == 0)
				if (empty-- == 0)
					return symbol;

		// oops!  our count is too big, either due
		// to a bit error, or a short node count given to huff_init.

		return 0;
	}

//  decode the next symbol
	private int decodeSymbol() throws IOException
	{
		int node = root;
		int symbol, down;

		// work down the tree from the root until reaching either a leaf or the escape node.
		// A one bit means go left, a zero means go right.
		while( (down = table[node].down) > 0 )
			if (readBit() > 0)
				node = down - 1;  // the left child preceeds the right child
			else
				node = down;

		// sent to the escape node???
		// refuse to add to a full tree
		if (node == ESC)
			if (ESC > 0)
			{
				symbol = readId();
				node = split(symbol);
			} else
				return 0;
		else
			symbol = table[node].symbol;

		//  increment weights and rebalance
		//  the coding tree

		increment(node);

		return symbol;
	}

	public void writeLastBits() throws IOException
	{
		while(ArcBit > 0)  // flush last few bits
			writeBit(0);
	}

	byte ArcBit;
	int ArcChar;

	// we use writeBit during compression ONLY
	private void writeBit(int bit) throws IOException
	{
		ArcChar <<= 1;

		if(bit > 0)
			ArcChar |= 1;

		if(++ArcBit < 8)
			return;

		cdata.sout.write(ArcChar);
		encodedBytes++;

		ArcChar = ArcBit = 0;
	}

	// we use readBit during Uncompression ONLY
	private int readBit() throws IOException
	{
		if(ArcBit == 0)
		{
			//ArcChar = getc(In);
			ArcChar = cdata.sin.read();
			assert ArcChar != -1;
			ArcBit = 8;
			//encodedBytes++;
		}

		return (ArcChar >> --ArcBit) & 1;
	}
	private void resetBitCache()
	{
		ArcBit = 0;
		ArcChar = 0;
	}

}
