import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BWTCoderTest {

	@ParameterizedTest(name = "BWTCoder - {index}")
	@CsvSource(value = {
			"zxcvbMMMbvc",
			"zxcvbMMMb",
			"MMMMMMMMMMMMMMMMMMMM",
			"MMMMAAAzxcvbMMMAAAA",
			"ababababababababababababababababa, 33, 33, 7",
			"ababababababababababababababababa",
			"MMMMMMMMMMMMMMMMMMMMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, 50,50, 10",
			"MMMMMMMMMMMMMMMMMMMMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc",
			"яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя",
			"яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя\0\0\0\0\0\0\0",
			"1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;",
			"1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,.",
			"01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,",
	}, ignoreLeadingAndTrailingWhitespace = true)
	void encodeDecodeBlock(String str)
	{
		var coder = new BWTCoder();
		var data = new BlockCoderData();
		data.allocate(512);
		byte[] b = str.getBytes();
		System.arraycopy(b,0,data.srcblock,0, b.length);
		data.bytesInBlock = b.length;

		coder.encodeBlock(data);

		//assertEquals(20, b.length);
		assertEquals(data.bytesInBlock, b.length);

		data.swapBuffers();
		coder.decodeBlock(data);

		System.out.println("b=" + Arrays.toString(b));
		System.out.println("Line=" + data.bwtLineNum);
		System.out.println("destblock=" + Arrays.toString(data.destblock));
		System.out.println("srcblock=" + Arrays.toString(data.srcblock));

		assertArrayEquals(b, Arrays.copyOf(data.destblock, data.bytesInBlock));
	}

}