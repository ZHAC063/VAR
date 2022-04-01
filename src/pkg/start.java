package pkg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * contains the main method runnable at the start of the program
 * @author Dennis
 *
 */
public class start {
	
	/**
	 * 					the location of each stock csv file
	 */
	static List<String> filePaths = new LinkedList<String>();
	/**
	 * 					the names of each stock 
	 */
	static List<String> fileNames = new LinkedList<String>();
	/**
	 * 					stores the values invested in each stock
	 */
	static BigDecimal[] investmentValues;
	/**
	 * 					the confidence percent used for VAR 
	 */
	static BigDecimal conf;
	/**
	 * 					used to round the math values after division to 9 decimals to keep precision
	 */			
	static MathContext d = new MathContext(9);
	/**
	 * 					number of data observations
	 */
	static int dataLength;
	/**
	 * 					stores each observations gain for each stock as a percent 
	 */
	static BigDecimal[][] profitP = new BigDecimal[dataLength][filePaths.size()];
	/**
	 * 					stores total value invested in portfolio across all stocks
	 */
	static BigDecimal portVal = new BigDecimal(0);
	/**
	 * 					stores the weight of each stock as a decimal of total portfolio
	 */
	static BigDecimal[] portPerc;
	/**
	 * 					average gain of total portfolio each observation
	 */
	static BigDecimal portAvg;
	/**
	 * 					the value of normal distribution associated with the conf value
	 * 					<p>
	 * 					calculated using the error function
	 */
	static BigDecimal zScore;
	/**
	 * 					stores the linked list of the portfolio objects ready to be analysed
	 */
	static List<VARmethods> methods = new LinkedList<VARmethods>();
	/**
	 *                  used to read user input
	 */
	static Scanner sc = new Scanner(System.in);
	

	/**
	 * 						creates and runs the VAR object for each portfolio 
	 * @param args			input entered from the console when ran directly 
	 * @throws IOException	an error has occured when reading from the stock csv files 
	 */
	public static void main(String[] args) throws IOException {
	  //Creates a scanner to read input about the investments
	  Scanner sc = new Scanner(System.in);
      //warns the user about using VAR values 
	  System.out.println("All VARs are not guaranteed and are theoretical, "
	      + "do not invest more than you are willing to lose and these values are not to be taken as fact.");
	  List<File >folders = createObj();
	  //runs the loop once for each portfolio
	  for(int x = 0; x < methods.size(); x++) {
	    System.out.println("                  PORTFOLIO " + (x+1) + " ");
	    clearFiles();
	    run(methods.get(x), folders.get(x));
	  }
	  sc.close();
	}
	
	/**            
	 *                     calls the method required to read stock data from csv files,
	 *                     initializes each portfolio object using the read data
	 *                      then runs each portfolio objects var calculation method 
	 * @param VM           the object representing a portfolio
	 * @param folder       the folder the portfolio being analysed is taken from
	 * @throws IOException an error has occured when reading from the stock csv files
	 */
	public static void run(VARmethods VM, File folder) throws IOException{	
	  //converts stock data to a 3d String array
	  String[][][] data = readFiles(folder);
	  initialisation();
	  BigDecimal zScore = new BigDecimal(invNorm(1-(conf.doubleValue()/100)));
	  //adds data specific to portfolio to each portfolio object 
	  VM.initialise(data, dataLength, fileNames, investmentValues, portPerc, conf, zScore, 0.94);
	  //the VAR calculation method is ran using the interface VARmethods
	  VM.calcVAR();
	  System.out.println(" ");
	  System.out.println(" ");
	}
	
	/**
	 *     all user input required is taken using this method including the confidence level and
	 *     investment values stored in each stock within the portfolio
	 */
	static void initialisation() {
	  portVal = new BigDecimal(0);
	  //the length of investmentValues is set to the number of files within the portfolio
	  investmentValues = new BigDecimal[filePaths.size()];
	  //the value of conf is assigned to 0, before user input
	  conf = new BigDecimal("0.0");
	  //while loop to check for confidence level
	  boolean confCheck = true;
	  while(confCheck) {
	    try {
	      //the user is asked for a confidence level
	      System.out.println("Enter confidence level:");
	      //the next decimal value entered will be stored as conf
	      conf = sc.nextBigDecimal();
	      confCheck = false;
	      //if the input does not match the expected type, a warning will be shown and
	      //the user will be asked again
	    } catch (InputMismatchException e) {
	      System.out.println("Sorry please enter a valid number");
	      sc.next();
	      confCheck = true;
	    }
	  }
	  // a while loop to enter investment values
	  boolean invCheck = true;
	  while(invCheck) {
	    try {
	      for(int invNum = 0; invNum < fileNames.size(); invNum++) {
	        //the user is asked for the value invested in each stock
	        System.out.println("Enter value of " + fileNames.get(invNum).replaceAll(".csv","") + " investment");
	        //the value of stock invested in each stock is held in an array
	        investmentValues[invNum] = sc.nextBigDecimal();
	        //the total value of the portfolio increases by each investment
	        portVal = portVal.add(investmentValues[invNum]);
	        invCheck = false;
	      }
	      //if the input does not match the expected type, the user is asked again
	    } catch (InputMismatchException e) {
	      System.out.println("Sorry please enter a valid number");
	      sc.next();
	      invCheck = true;
	    }
	  }
	  //the weight of each stock within the portfolio is calculated 
	  for(int x = 0; x < fileNames.size(); x++) {
	    portPerc[x] = investmentValues[x].divide(portVal,d);	
	  }
	}
	
	/**
	 *     the linked list of the file locations and
	 *     the linked list of the file names are cleared
	 */
	static void clearFiles() {
		filePaths.clear();
		fileNames.clear();
	}
	
	/**
	 *                 adds all stock csv files within a folder to two array Lists to store the location and name of the file
	 * @param folder   the folder to search through for stock csv files, this represents a portfolio
	 */
	static void folderSearch(final File folder) {
	  for (final File fileEntry : folder.listFiles()) {
	    if(fileEntry.getName().contains(".csv")) {
	      //file location is added to List
	      filePaths.add(fileEntry.getPath());
	      //file name is added to List
	      fileNames.add(fileEntry.getName());
	    }
	  }
	  //size of portPerc is initialised using the fileNames List
	  portPerc = new BigDecimal[fileNames.size()];
	}
	
	/**
	 *                 counts the number of csv files within a folder,
	 *                  used to determine the amount of stocks within a portfolio
	 * @param folder   the folder which represents the portfolio, which csv files will be counted
	 * @return         the number of csv files in the folder parameter
	 */
	static int folderCount(final File folder) {
	  //the variable to store the number of csv files
	  int count = 0;
	  for (final File fileEntry : folder.listFiles()) {
	    if(fileEntry.getName().contains(".csv")) {
	      count++;
	    }
	  }
		return count;
	}

	/**
	 *                 finds all the folders within a folder and outputs in the form of a List of Files
	 * @param folder  the folder which the number of folders contained needs to be sorted
	 * @return        a list containing all the folders within the folder parameter 
	 */
	static List<File> folderSort(final File folder) {
	  //file List created to store the files inside Folder
	  List<File>  folders = new LinkedList<File>();
	  //iterates through file in folder
	  for (final File fileEntry : folder.listFiles()) {
	    //check to validate if the selected folder is a folder
	    if (fileEntry.isDirectory()) {
	      //this is needed to make sure the pkg folder is not included in this search
	      if(!(fileEntry.getName().contains("pkg"))) {
	        folders.add(fileEntry);
	      }
	    } 
	  }
	  return folders;
	}
	
	/**    
	 *                     the stock market data is read and processed into a String array ready to be analysed
	 * @param folder       the folder representing a portfolio containing csv files holding the stock return data
	 * @return             the 3 dimensional array holding all the stock data for the entire portfolio in String format
	 * @throws IOException
	 */
	static String[][][] readFiles(File folder) throws IOException{
	  //dataLength is initialised
	  dataLength = 0;
	  //adds files within folder to main File array lists to be analysed
	  folderSearch(folder);
	  //first file is read using buffered reader, all stock files are assumed same length
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filePaths.get(0)));
		int lineHold = 0;
		//while the current line is not the last in the file
		while((bufferedReader.readLine()) != null)
		{
		    //line number being held increases
			lineHold++;
		}
		//reader is closed to save memory 
		bufferedReader.close();
		//dataLength is set to the total number of lines minus 1, taking into account column headers
		dataLength = lineHold-1;
		//dimensions of data is initialised according to size of stock data files
		String Data[][][] = new String[dataLength][7][filePaths.size()];
		//loop through each file in folder
		for(int x = 0; x < filePaths.size(); x++) {
			BufferedReader br = new BufferedReader(new FileReader(filePaths.get(x)));
			int lineNum = -1;
			String line = "";
			try {
			  //if line read is the end of a file
				while ((line = br.readLine()) != null) {
					if(lineNum!=-1) {
					   //csv row of stock data is separated by commas into an array
						String[] count = line.split(",");
						//each variable stored in the array is entered into the first dimension of Data
						Data[lineNum][0][x] = count[0];
						Data[lineNum][1][x] = count[1];
						Data[lineNum][2][x] = count[2];
						Data[lineNum][3][x] = count[3];
						Data[lineNum][4][x] = count[4];
						Data[lineNum][5][x] = count[5];
						Data[lineNum][6][x] = count[6];
						//line number read is incremented
						lineNum++;
					}
					else {
						lineNum++;
					}
				}
			}	catch (FileNotFoundException e) {
					e.printStackTrace();
			}
			//reader is closed to reduce memory used
			br.close();
		}
		//the String array holding the stock data for the entire portfolio is returned.
		return Data;
	}
	
	/**
	 *                         counts the number of portfolios 
	 *                         and determines if they contain 1 or more stocks
	 *                         adds a empty varObject to the methods List to represent each portfolio
	 *                         the type of varObject is found by its number of stocks
	 * @return                 a list of each folder that contains the stock information
	 * @throws IOException     an error has occured when reading from the stock csv files 
	 */
	static List<File> createObj() throws IOException{
	  //points to the folder that contains all portfolio folders
	  final File folder = new File("src\\");
	  //creates a List of folders within the source folder, these hold the stocks within each portfolio
	  final List<File> folders = folderSort(folder);
	  //adds a varObject to the methods List for each portfolio
	  for(int x = 0; x < folders.size(); x++) {
	    System.out.println(folders.get(x));
	    if(folderCount(folders.get(x))>1) {
	      methods.add(new multiAsset());
	    }
	    else {
	      methods.add(new singleAsset());
	    }
	  }
	  // returns the list of Files, holding the portfolio folders
	  return folders;  
	} 
	
	//used to calculate zScore values
	//this was used from a stack overflow source as java does not contain similar invNorm functions languages such as python contain
	//https://stackoverflow.com/questions/9242907/how-do-i-generate-normal-cumulative-distribution-in-java-its-inverse-cdf-how
	public static double invErf(double z)
	{
	    int nTerms = 315;
	    double runningSum = 0;
	    double[] a = new double[nTerms + 1];
	    double[] c = new double[nTerms + 1];
	    c[0]=1;
	    for(int n = 1; n < nTerms; n++){
	        double runningSum2=0;
	        for (int k = 0; k <= n-1; k++){
	            runningSum2 += c[k]*c[n-1-k]/((k+1)*(2*k+1));
	        }
	        c[n] = runningSum2;
	        runningSum2 = 0;
	    }
	    for(int n = 0; n < nTerms; n++){
	        a[n] = c[n]/(2*n+1);
	        runningSum += a[n]*Math.pow((0.5)*Math.sqrt(Math.PI)*z,2*n+1);
	    }
	    return runningSum;
	}

	/**
	 *             find the zScore value needed to use a confidence level of a normal distribution
	 * @param tail the double value representing the tail of the normal distribution used at the confidence level specified
	 * @return     the zScore value associated with the confidence level
	 */
	public static double invNorm(double tail){
	    return (2/Math.sqrt(2))*invErf(2*tail-1);
	}
}