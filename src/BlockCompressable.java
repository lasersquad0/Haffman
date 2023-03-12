import java.io.IOException;

public interface BlockCompressable
{
	void startBlockCompressing(CompressData cData);
	void finishBlockCompressing() throws IOException;
	void compressBlock(BlockCoderData data) throws IOException;
}

interface BlockUncompressable
{
	void startBlockUncompressing(CompressData cData);
	void finishBlockUncompressing() throws IOException;
	void uncompressBlock(BlockCoderData data) throws IOException;
}