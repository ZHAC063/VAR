package pkg;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.MathContext;

public class tradingAlgo {
	static List<String> filePaths = new LinkedList<String>();
	static List<String> fileNames = new LinkedList<String>();
	static BigDecimal[] investmentValues;
	static BigDecimal conf;
	static MathContext d = new MathContext(9);
	static int dataLength;
	
	public static void main(String args[]) throws IOException{
		
		dataLength = 1258;
		String[][][] data = readFiles();
		initialisation();
		BigDecimal dataBD = new BigDecimal(dataLength);

		BigDecimal[][] profitP = new BigDecimal[dataLength][filePaths.size()];
		BigDecimal[] netProfit = new BigDecimal[dataLength];
		BigDecimal mean = new BigDecimal("0.0");

		BigDecimal[][] logList = new BigDecimal[dataLength][filePaths.size()];
		BigDecimal[] meanLog = new BigDecimal[filePaths.size()];
		for(int x = 0; x < dataLength; x++) {
			for(int y = 0; y < filePaths.size(); y++) {
				profitP[x][y] = (new BigDecimal(data[x][4][y])).divide(new BigDecimal(data[x][1][y]),d );
				logList[x][y] = new BigDecimal( Math.log(profitP[x][y].doubleValue()) );
				if(netProfit[x]!=null) {
					netProfit[x] = netProfit[x].add((profitP[x][y].multiply(investmentValues[y])).subtract(investmentValues[y]));
				}
				else {
					netProfit[x] = (profitP[x][y].multiply(investmentValues[y])).subtract(investmentValues[y]);
				}
				
				if(meanLog[y]!=null) {
					meanLog[y] = meanLog[y].add(logList[x][y]);
				}
				else {
					meanLog[y] = logList[x][y];
				}
				
				
				
			}
			mean = mean.add(netProfit[x]);
		}
		
		for (int x = 0; x < filePaths.size(); x++){
			meanLog[x] = meanLog[x].divide(new BigDecimal(dataLength) ,d);
			System.out.println(meanLog[x]);
		}
		mean = mean.divide(dataBD, d);	
		
		
		mergeSort(netProfit, dataLength);
		BigDecimal tail = dataBD.multiply((new BigDecimal("100").subtract(conf)).divide(new BigDecimal("100"), d));
		int nthVal = Math.round(tail.floatValue())-1;
		BigDecimal valueAtRisk = new BigDecimal(-1).multiply(netProfit[nthVal],d);
		System.out.println(" ");
		System.out.println("Confidence percent: " + conf);
		System.out.println("Value at risk:" + valueAtRisk);
		System.out.println("Daily profit mean: " + mean);
		backTest(valueAtRisk, netProfit);

		
	}
	
	static void initialisation() {
		investmentValues = new BigDecimal[filePaths.size()];
		conf = new BigDecimal("0.0");
		Scanner sc = new Scanner(System.in);
		boolean confCheck = true;
		while(confCheck) {
			try {
				System.out.println("Enter confidence level:");
				conf = sc.nextBigDecimal();
				confCheck = false;
			} catch (InputMismatchException e) {
				System.out.println("Sorry please enter a valid number");
				sc.next();
				confCheck = true;
			}
		}
		boolean invCheck = true;
		while(invCheck) {
			try {
				for(int invNum = 0; invNum < fileNames.size(); invNum++) {
					System.out.println("Enter value of " + fileNames.get(invNum).replaceAll(".csv","") + " investment");
					investmentValues[invNum] = sc.nextBigDecimal();
					invCheck = false;
				}
			} catch (InputMismatchException e) {
				System.out.println("Sorry please enter a valid number");
				sc.next();
				invCheck = true;
			}
		}
		sc.close();	
	
	}
	
	static String[][][] readFiles() throws IOException{
		final File folder = new File("src\\");
		folderSearch(folder);
		String Data[][][] = new String[dataLength][7][filePaths.size()];
		for(int x = 0; x < filePaths.size(); x++) {
			BufferedReader br = new BufferedReader(new FileReader(filePaths.get(x)));
			int lineNum = -1;
			String line = "";
			try {
				while ((line = br.readLine()) != null) {
					if(lineNum!=-1) {
						String[] count = line.split(",");
						Data[lineNum][0][x] = count[1];
						Data[lineNum][1][x] = count[1];
						Data[lineNum][3][x] = count[3];
						Data[lineNum][4][x] = count[4];
						Data[lineNum][5][x] = count[5];
						Data[lineNum][6][x] = count[6];
						lineNum++;
					}
					else {
						lineNum++;
					}
				}
			}	catch (FileNotFoundException e) {
					e.printStackTrace();
			}
			br.close();
		}
		return Data;
	}
	
	static void folderSearch(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            folderSearch(fileEntry);
	        } else {
	            if(fileEntry.getName().contains(".csv")) {
	                filePaths.add(fileEntry.getPath());
	            	fileNames.add(fileEntry.getName());
	            }
	        }
	    }
	}

	static void mergeSort(BigDecimal[] data, int dataLength) {
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
	    mergeSort(left, midPoint);
	    mergeSort(right, dataLength - midPoint);

	    merge(data, left, right, midPoint, dataLength - midPoint);
	}
	static void merge(BigDecimal[] bA, BigDecimal[] lA, BigDecimal[] rA, int left, int right) {
		int i = 0, j = 0, k = 0;
		while (i < left && j < right) {
	        if (lA[i].compareTo(rA[j]) <= 0 ) {
	        	bA[k++] = lA[i++];
		    	}
	        else {
	        	bA[k++] = rA[j++];
			    }
				}
		while (i < left) {
			bA[k++] = lA[i++];
			}
		while (j < right) {
	        bA[k++] = rA[j++];
	        }
	}
	
	static void backTest(BigDecimal VAR, BigDecimal[] data) {
		int numFail = 0;
		for(int x = 0; x < data.length; x++) {	
			if(data[x].compareTo(VAR.multiply(new BigDecimal(-1),d))<0) {
				numFail++;
			}
		}
		
		System.out.println(numFail+""+ " " + dataLength+"");
		System.out.print(new BigDecimal(1).subtract(new BigDecimal(numFail).divide(new BigDecimal(dataLength),d)));
	}
}