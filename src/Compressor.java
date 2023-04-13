import java.io.IOException;

public abstract class Compressor
{
	//private final int SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	protected CompressData cdata;
	protected long p100;
	private long threshold = 0;
	private long delta;

	public abstract void compress(CompressData cData) throws IOException;
	public abstract void uncompress(CompressData uData) throws IOException;

	protected void startProgress(long fullhouse)
	{
		p100 = fullhouse;
		delta = p100/100;
		//if(p100 > SHOW_PROGRESS_AFTER)
		cdata.cb.start();
	}
	protected void finishProgress()
	{
		//if (p100 > SHOW_PROGRESS_AFTER)
		cdata.cb.finish();
	}

	protected void updateProgress(long progress)
	{
		if(/*(p100 > SHOW_PROGRESS_AFTER) && */(progress >= threshold))
		{
			threshold += delta;
			cdata.cb.heartBeat((int)(100*threshold/p100));
		}
	}
}
