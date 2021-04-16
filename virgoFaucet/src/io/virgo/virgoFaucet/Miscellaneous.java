package io.virgo.virgoFaucet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.virgo.virgoAPI.VirgoAPI;

public class Miscellaneous {

	/**
	 * Format an atomic amount of VGO to a more elegant, human-readable format
	 * @param l the atomic VGO amount to format
	 * @return The formated representation
	 */
	public static String formatLong(long l) {
		double d = l/Math.pow(10, VirgoAPI.DECIMALS);
		
	    if(d == (long) d)
	        return String.format("%d",(long)d);
	    else {
	    	BigDecimal number = new BigDecimal(""+d);
	    	return number.setScale(VirgoAPI.DECIMALS, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
	    }
	    
	}
	
}
