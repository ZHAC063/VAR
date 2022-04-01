package pkg;
import java.util.List;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**#
 * contains the running process for single stock portfolios
 * implements VARmethods
 * @author Dennis
 *
 */
public class singleAsset implements VARmethods{

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
  *    stores the average gain of the stock across each observation                
  */
BigDecimal netProfit[][];
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
  *    contains the enum type for the VARmethods object this is always type.SINGLE for this object 
  */
 public final type id = type.MULTI;

 
public void calcVAR(){  
   System.out.println(" ");
   //states the confidence level used
   System.out.println("Confidence percent: " + conf);   
   System.out.println(" ");
   System.out.println("HISTORICAL METHOD");
   //runs the historical VAR method
   historicalMethod(netProfit, new BigDecimal(dataLength), conf);   
   System.out.println(" ");
   System.out.println("NON WEIGHTED VOLATILITY METHOD");
   //runs the non weighted VAR method
   BigDecimal nwMAT = nonWeightedVolatilityMethod(this.zScore, this.profitP, this.dataLength, this.stockNum, this.investmentValues, this.portVal)[0][0];
   BigDecimal nwDEV = calcSD(nwMAT);
   System.out.println("Value at risk for portfolio: " + new BigDecimal(-1).multiply(portAvg.add((new BigDecimal(-1.65).multiply(nwDEV)))).multiply(portVal).round(new MathContext(4, RoundingMode.HALF_UP)));
   backTest(new BigDecimal(-1).multiply(portAvg.add((new BigDecimal(-1.65).multiply(nwDEV)))).multiply(portVal), "EWMA");   
   System.out.println(" ");
   System.out.println("EWMA METHOD");
   //runs the exponentially weighted VAR
   BigDecimal ewMAT = EWMA(lambda, profitP, stockNum, investmentValues, zScore, dataLength, portVal)[0][0];
   BigDecimal ewDEV = calcSD(ewMAT);
   System.out.println("Value at risk for portfolio: " + new BigDecimal(-1).multiply(portAvg.add((new BigDecimal(-1.65).multiply(ewDEV)))).multiply(portVal).round(new MathContext(4, RoundingMode.HALF_UP)));
   backTest(new BigDecimal(-1).multiply(portAvg.add((new BigDecimal(-1.65).multiply(ewDEV)))).multiply(portVal), "EWMA");	
 }
/**
 *    sets all the values needed for VAR stored in this object to their values passed into the method
 *    uses the inputed parameters to calculate any other needed variables
 */
 public void initialise(String[][][] data, int dataLength, List<String> fileNames, BigDecimal[] investmentValues, BigDecimal[] portPerc, BigDecimal conf, BigDecimal zScore, double lambda) {   
   //data is initialised using passed in variables
   this.dataLength = dataLength;
   this.stockNum = fileNames.size();
   this.fileNames = fileNames;
   this.investmentValues = investmentValues;
   this.portPerc = portPerc;
   this.conf = conf;
   this.zScore = zScore;
   this.data = data;
   this.profitP = new BigDecimal[dataLength][stockNum];
   this.netProfit = new BigDecimal[stockNum][dataLength];
   this.lambda = lambda;
   this.portVal = new BigDecimal(0);
   this.portVal =investmentValues[0];
   //daily profit is calculated as a percent for each observation
   for(int x = 0; x < dataLength; x++) {
       this.profitP[x][0] = (new BigDecimal(data[x][4][0]).subtract(new BigDecimal(data[x][1][0]))).divide(new BigDecimal(data[x][1][0]),d );
   }
   //netprofit is calculated for each observation using profitP
   for(int x = 0; x < dataLength; x++) {
     if(netProfit[0][x]!=null)
       netProfit[0][x] = netProfit[x][0].add((profitP[x][0].multiply(investmentValues[0])));
        else 
          netProfit[0][x] = (profitP[x][0].multiply(investmentValues[0]));
        
   }
   BigDecimal[] weightedPortRet = new BigDecimal[dataLength];
   this.portAvg = new BigDecimal(0);
   for(int x = 0; x < dataLength; x++) {
       if(weightedPortRet[x]!=null) {
         weightedPortRet[x] = weightedPortRet[x].add(profitP[x][0].multiply(portPerc[0]));
       }
       else {
         weightedPortRet[x] = profitP[x][0].multiply(portPerc[0]);
       }
   }
   //the average portAvg is calculated using the weightedPortRet for each observation
   for(int x = 0; x < dataLength; x++) {
     portAvg = portAvg.add(weightedPortRet[x]);	
   }
   portAvg = portAvg.divide(new BigDecimal(dataLength),d);
 }
 
 /**
  *                 calculates VAR of the portfolio at the specified level of confidence
 * @param netProfit the monetary gain of each observation for each stock within the portfolio
 * @param dataBD    the number of observations in BigDecimal format   
 * @param conf      the confidence level used
 * @return          the VAR calculated using the historical method at a confidence level specified
 */
BigDecimal historicalMethod(BigDecimal[][] netProfit, BigDecimal dataBD, BigDecimal conf) {
   BigDecimal[] VAR = new BigDecimal[netProfit[0].length];
   BigDecimal totalVAR = new BigDecimal(0);
   for(int x = 0; x < netProfit.length; x++) {
     //sorts the returns of each observation using a merge sort in ascending order
     mergeSort(netProfit[x], dataBD.intValue());
     //finds the percentile of the data in line with the confidence level chosen
     BigDecimal tail = dataBD.multiply((new BigDecimal("100").subtract(conf)).divide(new BigDecimal("100"), d));
     // rounds to the closest data index
     int nthVal = Math.round(tail.floatValue())-1;
     //converts data index into a VAR value
     VAR[x] = new BigDecimal(-1).multiply(netProfit[x][nthVal],d);
     //adds var to the total VAR for historical method
     totalVAR = VAR[x].add(totalVAR);
   }
   //rounds to 4 decimal places
   totalVAR = totalVAR.setScale(4, RoundingMode.HALF_UP);
   //outputs the VAR to the terminal for analysis by the user
   System.out.println("Total Value At Risk using historical method for portfolio: " + totalVAR);
   //returns the VAR given over the entire portfolio calculated using the historical method
   return totalVAR;
 }
	
 /**
  *                     calculates the covariance between each stock in the portfolio using simple weighting and outputs the results as a matrix 
 * @param zScore        the value used in the exponential weighting
 * @param profitP       stores the average gain of each stock separately across each observation 
 * @param dataLength    the number of observations
 * @param stockNum      the number of stocks within the portfolio
 * @param investmentValues  the values invested within each stock held in the portfolio
 * @param portVal       the total value invested in the portfolio    
 * @return              a covariance matrix calculated using simple weighting
 */
BigDecimal[][] nonWeightedVolatilityMethod(BigDecimal zScore, BigDecimal[][] profitP, int dataLength, int stockNum, BigDecimal[] investmentValues, BigDecimal portVal) {
   BigDecimal[] avgP = new BigDecimal[stockNum];
   BigDecimal[] weightedPortRet = new BigDecimal[dataLength];
   portAvg = new BigDecimal(0);
   BigDecimal portPerc[] = new BigDecimal[stockNum];
   //the weighting of each stock in comparison to the total portfolio is calculated
   for(int x = 0; x < stockNum; x++) {
     portPerc[x] = investmentValues[x].divide(portVal,d);
   }
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
   //the average gain across the entire portfolio for any observation is calculated using the waited gain for each observation using each stock
   for(int x = 0; x < dataLength; x++) {
     portAvg = portAvg.add(weightedPortRet[x]);
   }
   portAvg = portAvg.divide(new BigDecimal(dataLength),d);
   for(int x = 0; x < stockNum; x++) {
     avgP[x] = (avgP[x].divide(new BigDecimal(dataLength),d).multiply(new BigDecimal(100)));			
   }	
   //covariance is calculated using the simple weighted covariance formula
   BigDecimal sum1 = new BigDecimal(0);
   BigDecimal sum2 = new BigDecimal(0);
   BigDecimal[][] Matrix = new BigDecimal[stockNum][stockNum];
   for(int x = 0; x < stockNum; x ++) {
     for(int y = x; y < stockNum;y++) {
       for(int z = 0; z < dataLength; z++) {
         sum1 = (profitP[z][x].subtract(avgP[x])).multiply(profitP[z][y].subtract(avgP[y]));
         sum2 = sum2.add(sum1);
       }
       Matrix[x][y] = sum2.divide((new BigDecimal(dataLength-1)),d);
       Matrix[y][x] = Matrix[x][y];
       sum2 = new BigDecimal(0);
     }
   }
   System.out.println(" ");
   System.out.println("Covariance Matrix:");
   for(int x = 0; x < stockNum; x++) {
     for(int y = 0; y < stockNum;y++) {
       System.out.print(" " + Matrix[x][y] + " ");
     }
     System.out.println(" ");			
   }
   //the covariance matrix calculated using no weighting is returned
   return Matrix;		
 }
 
 /**
  *                 calculates the covariance between each stock in the portfolio using exponential weighting and outputs the results as a matrix               
 * @param lambda    the value used in the exponential weighting
 * @param profitP   contains the average profit as a percentage for each observation for each stock stored separately in a 2D array
 * @param stockNum  the number of stocks in the portfolio
 * @param investmentValues  contains the amount invested in each stock within the portfolio
 * @param zScore    the value that associates the wanted confidence level to the normal distribution
 * @param dataLength    the number of observations
 * @param portVal   the total value of the portfolio    
 * @return          the covariance matrix for each stock within the portfolio calculated using exponential weighting using lambda
 */
public BigDecimal[][] EWMA(double lambda, BigDecimal[][] profitP, int stockNum, BigDecimal[] investmentValues, BigDecimal zScore, int dataLength, BigDecimal portVal) {    
   BigDecimal[] avgP = new BigDecimal[stockNum];
   BigDecimal[] weightedPortRet = new BigDecimal[dataLength];
   portAvg = new BigDecimal(0);	
   //the average return of each stock over each observation is calculated.
   for(int x = 0; x < dataLength; x++) {
     for(int y = 0; y < stockNum; y++) {
       if(avgP[y]!=null) 
         avgP[y] = avgP[y].add(profitP[x][y]);
       else
         avgP[y] = profitP[x][y];
     }
   }	
   // the weighting of each stock is calculated and applied to its average return across each observation
   BigDecimal portPerc[] = new BigDecimal[stockNum];
   for(int x = 0; x < stockNum; x++) {
     portPerc[x] = investmentValues[x].divide(portVal,d);
   }
   for(int x = 0; x < stockNum; x++) {
     avgP[x] = (avgP[x].divide(new BigDecimal(dataLength),d).multiply(new BigDecimal(100)));			
   }
   for(int x = 0; x < dataLength; x++) {
     for(int y = 0; y < stockNum; y++) {
       if(weightedPortRet[x]!=null) {
         weightedPortRet[x] = weightedPortRet[x].add(profitP[x][y].multiply(portPerc[y]));
       }
       else {
         weightedPortRet[x] = profitP[x][y].multiply(portPerc[y]);
       }
     }
   }	
   //the formula for exponentialy weighted covariance is performed upon the data.
   //sum3 contains the previous value of covariance in comparison to the next observation
   //sum1 calculates the daily covariance
   //sum2 adds sum1 with a weighting of 1 minus lambda
   BigDecimal sum1 = new BigDecimal(0);
   BigDecimal sum2 = new BigDecimal(0);
   BigDecimal sum3 = new BigDecimal(0);
   BigDecimal[][] Matrix = new BigDecimal[stockNum][stockNum];
   for(int x = 0; x < stockNum; x ++) {
     for(int y = x; y < stockNum;y++) {
       for(int z = 0; z < dataLength; z++) {
         if(dataLength!=0) {
           sum2 = new BigDecimal(1-lambda).multiply(sum1);
         }
         sum1 = (new BigDecimal(lambda)).multiply((profitP[z][x].subtract(avgP[x])).multiply(profitP[z][y].subtract(avgP[y])));
         sum3 = sum3.add(sum1).add(sum2);		
       }
       Matrix[x][y] = sum3.divide((new BigDecimal(dataLength-1)),d);
       Matrix[y][x] = Matrix[x][y];
       sum3 = new BigDecimal(0);
     }
   }
   System.out.println(" ");
   System.out.println("Covariance Matrix:");
   for(int x = 0; x < stockNum; x++) {
     for(int y = 0; y < stockNum;y++) {
       System.out.print(" " + Matrix[x][y] + " ");
     }
     System.out.println(" ");	
   }
   //the covariance matrix is returned
   return Matrix;
 }

 /**      calculates the standard deviation using the variance of a stock
 * @param Variance the variance of the stock being analysed
 * @return  the standard deviation of the stock
 */
BigDecimal calcSD(BigDecimal Variance){	    
   BigDecimal SD = new BigDecimal(Math.sqrt(Variance.doubleValue()));
   return (SD);
 }
 /**
 *  compares the VAR given with the historical data provided to calculate an accuracy percentage
 * @param VAR the VAR to be tested
 * @param method    the name of the VAR method used to find the VAR 
 */
public void backTest(BigDecimal VAR, String method) {
   int numSum = 0;
   //for each observation, the VAR is compared to see if the actual loss was greater, the losses are counted using the numSum variable
   for(int x = 0; x < data.length; x++) {
     if(netProfit[0][x].compareTo(VAR.multiply(new BigDecimal(-1),d))<0) {
       numSum++;
     }
   }
   System.out.println("Total observations exceeding VAR for stock " + fileNames.get(0) + ": " + numSum + " times out of " + dataLength);
   System.out.println("This means the VAR was acurate at a confidence level of: " + new BigDecimal(1).subtract(new BigDecimal(numSum).divide(new BigDecimal(dataLength),d)).round(new MathContext(4, RoundingMode.HALF_UP)));
 }
	
 /**
  *     performs a merge sort on the list to sort in ascending order
 * @param data  The list to be sorted
 * @param dataLength    the number of observations
 */
void mergeSort(BigDecimal[] data, int dataLength) {
   if (dataLength < 2) {
     return;
   }
   int midPoint = dataLength / 2;
   BigDecimal[] left = new BigDecimal[midPoint];
   BigDecimal[] right = new BigDecimal[dataLength - midPoint];  
   for (int i = 0; i < midPoint; i++) {
     left[i] = data[i];
   }
   for (int i = midPoint; i < dataLength; i++) {
     right[i - midPoint] = data[i];
   }
   //sorts each subsection of the list split during the merge sort process
   mergeSort(left, midPoint);
   mergeSort(right, dataLength - midPoint); 
   //merges the left branch, right branch and midpoint into one sorted list
   merge(data, left, right, midPoint, dataLength - midPoint);
 }
 /**
  *             merges the result of a merge sort into one list
 * @param A     the original list
 * @param B     the left tail of the list
 * @param C     the right tail of the list
 * @param left  midpoint of the merge
 * @param right right midpoint
 */
void merge(BigDecimal[] A, BigDecimal[] B, BigDecimal[] C, int left, int right) {
   int i = 0, j = 0, k = 0;
   while (i < left && j < right) {
     if (B[i].compareTo(C[j]) <= 0 ) {
       A[k++] = B[i++];
     }
     else {
       A[k++] = C[j++];
     }
   }
   while (i < left) {
     A[k++] = B[i++];
   }
   while (j < right) {
     A[k++] = C[j++];
   }
 }
 public type getId() {  
   return this.id;
   }
}