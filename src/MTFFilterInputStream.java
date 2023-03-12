import java.io.InputStream;
import java.util.logging.Logger;

public class MTFFilterInputStream extends CoderFilterInputStream {
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	private final MTFCoder mtfcoder;

	MTFFilterInputStream(InputStream in, int bSize)
	{
		super(in, bSize);
		mtfcoder= new MTFCoder();
	}

	@Override
	protected void encodeBlock()
	{
		mtfcoder.encodeBlock(data);
	}

}
