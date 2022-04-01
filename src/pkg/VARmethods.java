package pkg;

import java.math.BigDecimal;
import java.util.List;

/**
 *    Holds methods used by both types of asset objects to further increase abstraction of the program
 *    @author Dennis
 */
public interface VARmethods {
	/**
	 * 	type of portfolio
	 *  {@link #SINGLE}
	 *  {@link #MULTI}
	 *
	 */
	enum type {
		/**
		 * single portfolio
		 */
		SINGLE,
		/**
		 * multiple portfolio
		 */
		MULTI
	}
	
	/**
	 *             used to determine if the portfolio is made up of 1 or more stocks
	 * @return		the type enum showing if the object is a single or multi stock portfolio
	 */
	public type getId();
	
	/**
	 *             calculates VAR using multiple methods for this portfolio
	 */
	public void calcVAR();
	
	/**
	 * 							initialises the VARmethods object and assigns a value to each of the variables
	 * @param data				the stock market data 
	 * @param dataLength		the total number of observations of each security
	 * @param fileNames			the names of each security
	 * @param investmentValues	the value of each investment within the portfolio
	 * @param portPerc			the weight of each security within the portfolio as a decimal
	 * @param conf				the confidence value for the VAR
	 * @param zScore			the value associated with the normal distribution at the level required by the conf, found using the error function
	 * @param lambda			the value used in EWMA to give weight to each observation
	 */
	public void initialise(String[][][] data, int dataLength, List<String> fileNames, BigDecimal[] investmentValues, BigDecimal[] portPerc, BigDecimal conf, BigDecimal zScore, double lambda);
	
	/**
	 * 							tests the given VAR with the actual data to determine accuracy of VAR
	 * @param VARs				a list of the VAR associated with each stock in the potfolio
	 * @param method			a string storing the type of method used to calculate the VAR, used when printing to the terminal
	 */
	public void backTest(BigDecimal VARs, String method);
}



