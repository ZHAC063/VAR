package pkg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Contains the running process for multiple stock objects 
 * extends singleAsset and implements VARmethods
 * @author Dennis
 *
 */
public class multiAsset extends singleAsset implements VARmethods{

  /**
   *     used to round the math values after division to 9 decimals to keep precision
   */         
  static MathContext d = new MathContext(9);
  /**
   *     stores all the stock market data from the entire portfolio as a 3 dimensional String array
   */
  String[][][] data;
  /**
   *    stores the number of observations
   */
  int dataLength;
  /**
   *    contains the list of all stock file names
   */
  List<String> fileNames;
  /**
   *    contains the amount invested in each stock within the portfolio
   */
  BigDecimal[] investmentValues;
  /**
   *    contains the weighting of each stock compared to the entire portfolio as a percentage
   */
  BigDecimal[] portPerc;
  /**
   *    used to round the math values after division to 9 decimals to keep precision
   */
  BigDecimal conf;
  /**
   *    the value that associates the wanted confidence level to the normal distribution
   */
  BigDecimal zScore;
  /**
   *    the value used in the exponential weighting
   */
  double lambda;
  /**
   *    the total monetary value of the whole porfolio
   */
  BigDecimal portVal;
  /**
   *    the number of stocks within the portfolio
   */
  int stockNum;
  /**
   *    the average gain for each observation across the entire portfolio
   */
  BigDecimal portAvg;
  /**
   *    contains the average profit as a percentage for each observation for each stock stored separately in a 2D array
   */
  BigDecimal[][] profitP;
  /**
   *    stores the average gain of each stock separately across each observation                
   */
  static BigDecimal[] avgP;
        
  /**
   *    contains the standard deviation calculated from exponentially weighted VCV
   */
  BigDecimal ewDEV; 
  /**
   *    contains the standard deviation calculated from the historical method
   */
  BigDecimal histVAR;
  /**
   *    contains the standard deviation calculated from the simple weighted VCV
   */
  BigDecimal nwDEV;
  /**
   *    contains the enum type for the VARmethods object this is always type.MULTI for this object 
   */
  public final type id = type.MULTI;

  @Override
  public void calcVAR() {
    System.out.println("HISTORICAL METHOD");
    //runs the historical method from the singleAsset class
    this.histVAR = super.historicalMethod(this.netProfit, new BigDecimal(dataLength), new BigDecimal(95));
    //tests the historical VAR with historical values
    backTest(histVAR, "HIST");
    System.out.println(" ");
    System.out.println("EWMA METHOD");
    //runs the exponential method from the singleAsset class
    BigDecimal[][] matrix = super.EWMA(this.lambda, this.profitP, stockNum, this.investmentValues, this.zScore, this.dataLength, this.portVal);
    this.ewDEV = calcSD(this.portPerc, matrix);
    //prints the result of the formula for VAR using the EW standard deviation
    System.out.println("Value at risk for portfolio: " + new BigDecimal(-1).multiply(portAvg.add(zScore.multiply(ewDEV)).multiply(portVal)).setScale(4, RoundingMode.HALF_UP));
    //tests the exponentially weighted VAR with historical values
    backTest(new BigDecimal(-1).multiply(portAvg.add(zScore.multiply(ewDEV)).multiply(portVal)), "EWMA");
    System.out.println(" ");
    System.out.println("NON WEIGHTED VOLATILITY METHOD");
   //runs the non weighted method from the singleAsset class
    matrix = super.nonWeightedVolatilityMethod(this.zScore, this.profitP, this.dataLength, stockNum, this.investmentValues, this.portVal);
    this.nwDEV = calcSD(this.portPerc, matrix);
    //prints the result of the formula for VAR using the NW standard deviation
    System.out.println("Value at risk for portfolio: " + new BigDecimal(-1).multiply(portAvg.add(zScore.multiply(nwDEV)).multiply(portVal)).setScale(4, RoundingMode.HALF_UP));
    //tests the non weighted VAR with historical values
    backTest(new BigDecimal(-1).multiply(portAvg.add(zScore.multiply(nwDEV))).multiply(portVal), "NON WEIGHTED"); 
  }

  /**
   *    sets all the values needed for VAR stored in this object to their values passed into the method
   *    uses the inputed parameters to calculate any other needed variables
   */
  public void initialise(String[][][] data, int dataLength, List<String> fileNames, BigDecimal[] investmentValues, BigDecimal[] portPerc, BigDecimal conf,  BigDecimal zScore, double lambda) { 
    this.dataLength = dataLength;
	this.stockNum = fileNames.size();
	this.fileNames = fileNames;
	this.investmentValues = investmentValues;
	this.portPerc = portPerc;
	this.zScore = zScore;
	this.netProfit = new BigDecimal[stockNum][dataLength];
	this.profitP = new BigDecimal[dataLength][stockNum];
	this.portVal = new BigDecimal(0); 
	//portval is calculated by adding each investment in the total portfolio
	for(int x = 0; x < investmentValues.length; x++) {
	  this.portVal = portVal.add(investmentValues[x]);
	}
	System.out.println(dataLength);
	System.out.println(stockNum);
	//profitP for each observation is calculated using the closing price minus the opening price all divided by the opening price
	for(int x = 0; x < dataLength; x++) {
	  for(int y = 0; y < stockNum; y++) {
	    this.profitP[x][y] = (new BigDecimal(data[x][4][y]).subtract(new BigDecimal(data[x][1][y]))).divide(new BigDecimal(data[x][1][y]),d );
	  }
	}
	this.lambda = lambda;
	//netProfit for each observation and each stock is calculated using a nested loop multiplying each days profit percent by the value invested
	for(int x = 0; x < dataLength; x++) {
	  for(int y = 0; y < stockNum; y++) {
	    if(netProfit[y][x]!=null) {
	      netProfit[y][x] = netProfit[x][y].add((profitP[x][y].multiply(investmentValues[y])));
	    }
	    else { 
	      netProfit[y][x] = (profitP[x][y].multiply(investmentValues[y]));
	    }
	  }
	}
	avgP = new BigDecimal[stockNum];
	BigDecimal[] weightedPortRet = new BigDecimal[dataLength];
	//the weighted average of gain for each observation in relation to the entire portfolio is calculated, this is used to calculate portAvg
	this.portAvg = new BigDecimal(0);  
	for(int x = 0; x < dataLength; x++) {
	  for(int y = 0; y < stockNum; y++) {
	    if(avgP[y]!=null) 
	      avgP[y] = avgP[y].add(profitP[x][y]);
	    else   
	      avgP[y] = profitP[x][y];
	    if(weightedPortRet[x]!=null) {
	      weightedPortRet[x] = weightedPortRet[x].add(profitP[x][y].multiply(portPerc[y]));
	    }
	    else {
	      weightedPortRet[x] = profitP[x][y].multiply(portPerc[y]);
	    }
	  }
    }
	//portAvg is calculated by totaling all weighted portfolio returns of each stock. and dividing by the number of observations minus 1
	for(int x = 0; x < dataLength; x++) {
	  portAvg = portAvg.add(weightedPortRet[x]);
	}
	  portAvg = portAvg.divide(new BigDecimal(dataLength),d);
  }
	
  /**
   *                    this method calculated the standard deviation of the entire portfolio by using the weight, 
   *                    covariance matrix and using these to calculate a corelation matrix for the entire portfolio
   *@param A            the weight of each stock in comparison to the total portfolio
   *@param B            the covariance matrix calculated for the entire portfolio
   */
BigDecimal calcSD(BigDecimal[] A, BigDecimal[][]B){
  //correlation coefficient matrix created with the same dimensions of the covariance matrix
  BigDecimal[][] corCo = new BigDecimal[A.length][A.length];
  System.out.println(" ");
  System.out.println("Correlation Matrix");
  for(int x = 0; x < B.length; x++) {
    for(int y = 0; y < B.length; y++) {
      //correlation coefficient calculated using the covariance matrix and variance of both stocks used to calculate the covariance
      corCo[x][y] = B[x][y].divide((new BigDecimal(Math.sqrt(B[x][x].doubleValue())).multiply(new BigDecimal(Math.sqrt(B[y][y].doubleValue())))),d);
      System.out.print(corCo[x][y]);
      System.out.print(" ");
    }
    System.out.println(" ");
  }
  System.out.println(" ");  
  BigDecimal sum1 = new BigDecimal(0);
  BigDecimal sum2 = new BigDecimal(0);
  //standard deviation sum of formula computed 
  for(int x = 0; x < A.length; x++) {
    //sum1 is used to show a sum of formula where the weight of stock x squared is multiplied by the variance of stock x
    sum1 = sum1.add((new BigDecimal(Math.pow(A[x].doubleValue(), 2))).multiply(B[x][x]));
  }
  //sum2 is used to show the second sum of as part of the total portfolio standard deviation
  for(int x = 0; x < A.length; x++) {
    for(int y = 0; y < A.length; y++) {
      if(x!=y) {
        //sum2 is the sum of the weight of stock x multiplied by the weight of stock y, multiplied by the correlation of stock x and y,
        //multiplied by the variance of stock x and variance of stock y
        sum2 = sum2.add(A[x].multiply(A[y]).multiply(corCo[x][y]).multiply(B[x][x]).multiply(B[y][y]));
      }
    }  
  }
  //the standard deviation is the sum of sum1 and sum2
  BigDecimal SD = new BigDecimal(Math.sqrt((sum1.add(sum2)).doubleValue()));	
  return (SD);
  }
	
 
  public type getId() {
    return this.id;
  }
	
  //tests a given VAR against the historical data used to calculate the VAR
  public void backTest(BigDecimal VAR, String methodVAR) {
    int failSum = 0;
    BigDecimal dailyTotal = new BigDecimal(0);
    for(int x = 0; x < netProfit[0].length; x++) {
      dailyTotal = new BigDecimal(0);
      for(int y = 0; y < netProfit.length; y++) {
        dailyTotal = dailyTotal.add(netProfit[y][x]);
      }	
      if(dailyTotal.multiply(new BigDecimal(-1)).compareTo(VAR)>0){ 
        failSum++;
      }		
    }
    System.out.println("This means the VAR was acurate across the entire portfolio at a confidence level of: " + new BigDecimal(1).subtract(new BigDecimal(failSum).divide(new BigDecimal(dataLength),d)).round(new MathContext(4, RoundingMode.HALF_UP)) + " " + methodVAR);
    }
}
