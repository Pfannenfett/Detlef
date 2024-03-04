package data.util;

public class MathUtils {
	
	public static int modPow(int base, int exponent, int modulo) {
		int ret = 1;
		String bits = Integer.toBinaryString(exponent);
		
		for(int i = 0; i < bits.length(); i++) {
			char c = bits.charAt(i);
			ret *= ret;
			ret %= modulo;
			if(c == '1') ret *= base;
			ret %= modulo;
		}
		return ret;
	}

}
