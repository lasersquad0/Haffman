import java.util.Arrays;
import java.util.logging.Logger;

public class MTFCoder
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final int ALFABET_SIZE = 256;
	//private int[] cumFreqs = new int[ALFABET_SIZE];
	byte[] alb = new byte[ALFABET_SIZE];

	public void encodeBlock0(BlockCoderData data)
	{
		//Modification of MTF: MTF is defined by the following modified rules: if the next
		//symbol has the current number z, then after its coding we change the number as
		//follows: if z > 1 then shift z to position 1 else shift z to position 0.

		fillAlfabet();

		int i;
		for (i = 0; i < data.bytesInBlock; i++)
		{
			byte x = data.srcblock[i];
			data.destblock[i] = shiftAlfabet(0, x);
		}
	}

	public void encodeBlock(BlockCoderData data)
	{
		//Modification of MTF: MTF is defined by the following modified rules: if the next
		//symbol has the current number z, then after its coding we change the number as
		//follows: if z > 1 then shift z to position 1 else shift z to position 0.

		fillAlfabet();

		int i;
		for (i = 0; i < data.bytesInBlock; i++)
		{
			byte x = data.srcblock[i];
			if(alb[0] == x) // symbol is on top position, do nothing and transfer 0 to dest array
			{
				data.destblock[i] = 0;
			}
			else if (alb[1] == x) // symbol is on position 1, swap it with top symbol and transfer 1
			{
				alb[1] = alb[0];
				alb[0] = x;
				data.destblock[i] = 1;
			}
			else // if symbol is NOT on position 1 or 0, do standard MTF
				data.destblock[i] = shiftAlfabet(1, x);
		}
	}
	public void decodeBlock0(BlockCoderData data)
	{
		//byte[] alb = buildAlfabet(c.mtfblock);
		fillAlfabet();

		for (int i = 0; i < data.bytesInBlock; i++)
		{
			data.destblock[i] = alb[Byte.toUnsignedInt(data.srcblock[i])];
			shiftAlfabet(0, data.destblock[i]);
		}
	}

	public void decodeBlock(BlockCoderData data)
	{
		//byte[] alb = buildAlfabet(c.mtfblock);
		fillAlfabet();

		for (int i = 0; i < data.bytesInBlock; i++)
		{
			int x = Byte.toUnsignedInt(data.srcblock[i]);
			data.destblock[i] = alb[x];
			if(x == 0)
				continue; // if x==0, no more actions required. alb stays as is.
			if(x == 1)
			{
				alb[1] = alb[0];
				alb[0] = data.destblock[i];
			}
			else
				shiftAlfabet(1, data.destblock[i]);
		}
	}

	private byte shiftAlfabet(int off, byte x)
	{
		byte tmp1 = alb[off];
		alb[off] = x;

		int j = off;
		// shift alfabet one-by-one till symbol x met
		while( tmp1 != x )
		{
			j++;
			byte tmp2 = tmp1;
			tmp1 = alb[j];
			alb[j] = tmp2;
		}

		return (byte)j;

		//if(p == 0) return; // nothing to do
		//byte pp = a[p];
		//System.arraycopy(a,0, a,1, p);
		//a[0] = pp;
	}

	private void fillAlfabet()
	{
		// use full (256 symbols) alphabet
		for (int i = 0; i < ALFABET_SIZE; i++)
		{
			alb[i] = (byte)i;
		}

		// заполняем частотами встречающихся символов
		// частоты нам не нужны особо, нужен алфавит
		// cumFreq создаем один раз в конструкторе потому что он небольшой и фикс размера.
		/*int notzero = 0;
		Arrays.fill(cumFreqs,0);
		for (byte b : data.srcblock)
		{
			if(cumFreqs[Byte.toUnsignedInt(b)] == 0) notzero++;
			cumFreqs[Byte.toUnsignedInt(b)]++;
		}

		byte[] alb = new byte[notzero]; // it's ok to allocate this small array for each block
		for (int i = 0, j = 0; i < cumFreqs.length; i++)
		{
			if(cumFreqs[i] != 0)
				alb[j++] = (byte)i;
		}

		Arrays.sort(alb);

		logger.finer(()->String.format("Alfabet:" + Arrays.toString(alb)));


		return alb;
		*/
	}


	/*
	private  int getMTFIndex(byte[] a, byte v)
	{
		for (int i = 0; i < a.length; i++)
		{
			if(a[i] == v) return i;
		}
		return -1; // impossible situation
	}
*/

}
