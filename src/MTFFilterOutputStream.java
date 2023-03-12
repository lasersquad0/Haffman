import java.io.OutputStream;

public class MTFFilterOutputStream extends CoderFilterOutputStream
{
	private final MTFCoder mftcoder;

	MTFFilterOutputStream(OutputStream sout, int bSize)
	{
		super(sout, bSize);
		mftcoder= new MTFCoder();
	}
	@Override
	protected void decodeBlock()
	{
		mftcoder.decodeBlock(data);
	}
}
