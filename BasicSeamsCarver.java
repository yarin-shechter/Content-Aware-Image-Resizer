package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;


public class BasicSeamsCarver extends ImageProcessor {
	public static final int VERTICAL = 0;
	public static final int HORIZONTAL = 1;
	
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");
		
		public final String description;
		
		private CarvingScheme(String description) {
			this.description = description;
		}
	}
	
	protected class Coordinate{
		public int X;
		public int Y;
		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}
	
	int[][] carvedGreyScaleImage;
	double[][] DPTable;
	int[][] minCoordinatesDPTable;
	Coordinate[][] originalCoordinatesTable;
	Coordinate[][] verticalSeams;
	Coordinate[][] horizontalSeams;
	int currentCarvedHeight;
	int currentCarvedWidth;
	int currentMode;
	
	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		this.logger.log("Performing initialization procedures for seam carving.");
		prepareForSeamCarving();
		this.logger.log("Initialization complete.");
	}
	
	protected void prepareForSeamCarving() {
		this.currentCarvedHeight = inHeight;
		this.currentCarvedWidth = inWidth;
		this.verticalSeams = new Coordinate[Math.abs(this.outWidth - this.inWidth)][];
		this.horizontalSeams = new Coordinate[Math.abs(this.outHeight - this.inHeight)][];
		initGreyScaleImage();
		initCoordinatesTable();
		initDPResources();
	}
	private void initGreyScaleImage() {
		this.carvedGreyScaleImage = new int[inHeight][inWidth];
		BufferedImage grey = greyscale();
		for (int y = 0; y < inHeight; y++) {
			for (int x = 0; x < inWidth; x++) {
				carvedGreyScaleImage[y][x] = new Color(grey.getRGB(x, y)).getRed();
			}
		}
	}
	
	private void initCoordinatesTable() {
		this.originalCoordinatesTable = new Coordinate[inHeight][inWidth];
		for (int y = 0; y < inHeight; y++) {
			for (int x = 0; x < inWidth; x++) {
				originalCoordinatesTable[y][x] = new Coordinate(x,y);
			}
		}
	}
	
	protected void initDPResources() {
		this.DPTable = new double[this.currentCarvedHeight][this.currentCarvedWidth];
		this.minCoordinatesDPTable = new int[this.currentCarvedHeight][this.currentCarvedWidth];
	}
	
	public BufferedImage carveImage(CarvingScheme carvingMode) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		
		if (carvingMode == CarvingScheme.VERTICAL_HORIZONTAL) {
			this.currentMode = VERTICAL;
			carveSeams(numberOfVerticalSeamsToCarve);
			this.currentMode = HORIZONTAL;
			carveSeams(numberOfHorizontalSeamsToCarve);
		}
		else if (carvingMode == CarvingScheme.HORIZONTAL_VERTICAL) {
			this.currentMode = HORIZONTAL;
			carveSeams(numberOfHorizontalSeamsToCarve);
			this.currentMode = VERTICAL;
			carveSeams(numberOfVerticalSeamsToCarve);
		}
		else {
			carveSeamsIntermittently(numberOfVerticalSeamsToCarve, numberOfHorizontalSeamsToCarve);
		}
		BufferedImage carvedImage = generateCarvedImage();
		
		return carvedImage;
	}
	
	private void carveSeamsIntermittently(int numberOfVerticalSeamsToCarve, int numberOfHorizontalSeamsToCarve) {
		boolean carveVertical = numberOfVerticalSeamsToCarve > 0;
		int numberOfVerticalSeamsCarved = 0;
		int numberOfHorizontalSeamsCarved = 0;
		while (numberOfVerticalSeamsCarved + numberOfHorizontalSeamsCarved < numberOfVerticalSeamsToCarve + numberOfHorizontalSeamsToCarve) {
			if (carveVertical) {
				this.currentMode = VERTICAL;
				carveSeams(1);
				if (numberOfHorizontalSeamsCarved < numberOfHorizontalSeamsToCarve) {
					carveVertical = false;
				}
				numberOfVerticalSeamsCarved++;
			}
			else {
				this.currentMode = HORIZONTAL;
				carveSeams(1);
				if (numberOfVerticalSeamsCarved < numberOfVerticalSeamsToCarve) {
					carveVertical = true;
				}
				numberOfHorizontalSeamsCarved++;
			}
		}
	}
	
	protected void carveSeams(int numOfSeamsToCarve) {
		logger.log("Carving " + numOfSeamsToCarve + " seams from the image.");
		for (int i = 0; i < numOfSeamsToCarve; i++) {
			logger.log("Carving seam no. " + (i + 1));
			findAndRemoveSeam();
		}
	}
	
	private void findAndRemoveSeam() {
		populateDPTableWithPixelEnergy();
		populateDPTableWithMinimalCosts();
		Coordinate[] optimalSeam = reconstructOptimalSeam();
		storeOptimalSeam(optimalSeam);
		carveSeamFromGreyscaleAndCoordinatesTable(optimalSeam);
		updateCarvedProportions();
	}
	
	private void populateDPTableWithPixelEnergy() {
		for (int y = 0; y < this.currentCarvedHeight; y++) {
			for (int x = 0; x < this.currentCarvedWidth; x++) {
				this.DPTable[y][x] = calcPixelEnergy(y,x); 
			}
		}
	}
	
	private double calcPixelEnergy(int y, int x) {
		double eVertical, eHorizontal;
		int nextX = x, nextY = y;
		
		if (x + 1 < this.currentCarvedWidth) {
			nextX++;
		}
		else {
			nextX--;
		}
		if (y + 1 < this.currentCarvedHeight) {
			nextY++;
		}
		else {
			nextY--;
		}
		eVertical = Math.abs(this.carvedGreyScaleImage[y][nextX] - this.carvedGreyScaleImage[y][x]);
		eHorizontal = Math.abs(this.carvedGreyScaleImage[nextY][x] - this.carvedGreyScaleImage[y][x]);
		eVertical = Math.pow(eVertical, 2);
		eHorizontal = Math.pow(eHorizontal, 2);
		return Math.sqrt(eVertical + eHorizontal);
	}
	
	private void populateDPTableWithMinimalCosts() {
		logger.log("Calculating the costs matrix \"m\".");
		if (this.currentMode == VERTICAL) {
			for (int y = 0; y < this.currentCarvedHeight; y++) {
				for (int x = 0; x < this.currentCarvedWidth; x++) {
					calcAndSetMinCostAtPixel(y,x);
				}
			}
		}
		else {
			for (int x = 0; x < this.currentCarvedWidth; x++) {
				for (int y = 0; y < this.currentCarvedHeight; y++) {
					calcAndSetMinCostAtPixel(y,x);
				}
			}
		}
	}
	
	private void calcAndSetMinCostAtPixel(int y, int x) {
		if (this.currentMode == VERTICAL) {
			calcAndSetVerticalMinCostAtPixel(y,x);
		}
		else {
			calcAndSetHorizontalMinCostAtPixel(y,x);
		}
	}
	
	private void calcAndSetVerticalMinCostAtPixel(int y, int x) {
		double min = 0;
		int minX = x;
		long cl = 0, cv = 0, cr = 0;
		double ml, mv, mr;
		if(y > 0) {
			mv = this.DPTable[y-1][x];
			
			if(x > 0 & x+1 < this.currentCarvedWidth)
				cl = cv = cr = Math.abs(this.carvedGreyScaleImage[y][x-1] - this.carvedGreyScaleImage[y][x+1]);
			//else 
				//cl = cv = cr = 255; // This part discourages selecting corner pixels.
			
			if(x > 0) {
				cl += Math.abs(this.carvedGreyScaleImage[y-1][x] - this.carvedGreyScaleImage[y][x-1]);
				ml = this.DPTable[y-1][x-1];
			} else {
				cl = 0;
				ml = Integer.MAX_VALUE;
			}
			
			if(x+1 < currentCarvedWidth) {
				cr += Math.abs(this.carvedGreyScaleImage[y-1][x] - this.carvedGreyScaleImage[y][x+1]);
				mr = this.DPTable[y-1][x+1];
			} else {
				cr = 0;
				mr = Integer.MAX_VALUE;
			}
			
			double sumL = ml+cl;
			double sumV = mv+cv;
			double sumR = mr+cr;
			
			min = Math.min(Math.min(sumL, sumV), sumR);
			
			if(min == sumR & x+1 < this.currentCarvedWidth)
				minX = x+1;
			else if(min == sumL & x > 0)
				minX = x-1;
		}
		
		this.DPTable[y][x] += min;
		this.minCoordinatesDPTable[y][x] = minX;
	}
	
	private void calcAndSetHorizontalMinCostAtPixel(int y, int x) {
		double min = 0;
		int minY = y;
		long cd = 0, ch = 0, cu = 0;
		double md, mh, mu;
		if(x > 0) {
			mh = this.DPTable[y][x-1];
			
			if(y > 0 & y+1 < this.currentCarvedHeight)
				cd = ch = cu = Math.abs(this.carvedGreyScaleImage[y-1][x] - this.carvedGreyScaleImage[y+1][x]);
			//else // This part discourages selecting corner pixels.
				//cd = ch = cu = 255;
			
			if(y + 1 < this.currentCarvedHeight) {
				cd += Math.abs(this.carvedGreyScaleImage[y+1][x] - this.carvedGreyScaleImage[y][x-1]);
				md = this.DPTable[y+1][x-1];
			} else {
				cd = 0;
				md = Integer.MAX_VALUE;
			}
			
			if(y > 0) {
				cu += Math.abs(this.carvedGreyScaleImage[y-1][x] - this.carvedGreyScaleImage[y][x-1]);
				mu = this.DPTable[y-1][x-1];
			} else {
				cu = 0;
				mu = Integer.MAX_VALUE;
			}
			
			double sumD = md+cd;
			double sumH = mh+ch;
			double sumU = mu+cu;
			
			min = Math.min(Math.min(sumD, sumH), sumU);
			
			if(min == sumU & y > 0)
				minY = y-1;
			else if(min == sumD & y + 1 < this.currentCarvedHeight)
				minY = y+1;
		}
		
		this.DPTable[y][x] += min;
		this.minCoordinatesDPTable[y][x] = minY;
	}
	
	private Coordinate[] reconstructOptimalSeam(){
		Coordinate[] optimalSeam;
		
		logger.log("Reconstructing the optimal seam from the costs matrix.");
		if (this.currentMode == VERTICAL) {
			optimalSeam = reconstructOptimalVerticalSeam();
		}
		else {
			optimalSeam = reconstructOptimalHorizontalSeam();
		}
		return optimalSeam;
	}
	
	private Coordinate[] reconstructOptimalVerticalSeam() {
		Coordinate[] optimalSeam = new Coordinate[this.currentCarvedHeight];
		int minX = 0;
		for(int x = 0; x < this.currentCarvedWidth; x++)
		{
			if(this.DPTable[this.currentCarvedHeight-1][x] < this.DPTable[this.currentCarvedHeight-1][minX])
			{
				minX = x;
			}
		}
		
		for(int y = this.currentCarvedHeight-1; y > -1; --y) {
			optimalSeam[y] = new Coordinate(minX, y);
			minX = this.minCoordinatesDPTable[y][minX];
		}
		
		return optimalSeam;
	}
	
	private Coordinate[] reconstructOptimalHorizontalSeam() {
		Coordinate[] optimalSeam = new Coordinate[this.currentCarvedWidth];
		int minY = 0;
		for(int y = 0; y < this.currentCarvedHeight; y++) {
			if(this.DPTable[y][this.currentCarvedWidth - 1] < this.DPTable[minY][this.currentCarvedWidth - 1]) {
				minY=y;
			}
		}
		
		for(int x = this.currentCarvedWidth-1; x > -1; --x) {
			optimalSeam[x] = new Coordinate(x, minY);
			minY = this.minCoordinatesDPTable[minY][x];
		}
		
		return optimalSeam;
	}
	
	private void storeOptimalSeam(Coordinate[] optimalSeam) {
		int numOfSeamsFoundSoFar;
		Coordinate[][] seamStorage;
		
		logger.log("Storing the optimal seam.");
		if (this.currentMode == VERTICAL) {
			seamStorage = this.verticalSeams;
			numOfSeamsFoundSoFar = this.inWidth - this.currentCarvedWidth;
		}
		else {
			numOfSeamsFoundSoFar = this.inHeight - this.currentCarvedHeight;
			seamStorage = this.horizontalSeams;
		}
		seamStorage[numOfSeamsFoundSoFar] = new Coordinate[optimalSeam.length];
		for (int i = 0; i < optimalSeam.length; i++) {
			seamStorage[numOfSeamsFoundSoFar][i] = this.originalCoordinatesTable[optimalSeam[i].Y][optimalSeam[i].X];
		}
	}
	
	private void carveSeamFromGreyscaleAndCoordinatesTable(Coordinate[] seam) {
		this.logger.log("Removing the optimal seam from the image");
		for (Coordinate c : seam) {
			shiftAtCoordinate(c);
		}
	}
	
	private void shiftAtCoordinate(Coordinate c) {
		if (this.currentMode == VERTICAL) {
			shiftLeft(c);
		}
		else {
			shiftUp(c);
		}
	}
	
	private void shiftLeft(Coordinate c){
		int y = c.Y;
		for (int x = c.X+1; x < this.currentCarvedWidth; x++) {
			this.originalCoordinatesTable[y][x-1] = this.originalCoordinatesTable[y][x];
			this.carvedGreyScaleImage[y][x-1] = this.carvedGreyScaleImage[y][x];
		}
	}
	
	private void shiftUp(Coordinate c) {
		int x = c.X;
		for (int y = c.Y+1; y < this.currentCarvedHeight; y++) {
			this.originalCoordinatesTable[y-1][x] = this.originalCoordinatesTable[y][x];
			this.carvedGreyScaleImage[y-1][x] = this.carvedGreyScaleImage[y][x];
		}
	}
	
	private void updateCarvedProportions() {
		if (this.currentMode == VERTICAL) {
			this.currentCarvedWidth--;
		}
		else {
			this.currentCarvedHeight--;
		}
	}
	
	protected BufferedImage generateCarvedImage() {
		BufferedImage carvedImage = newEmptyOutputSizedImage();
		Coordinate originalPixelCoordinate;
		
		for (int y = 0; y < carvedImage.getHeight(); y++) {
			for (int x = 0; x < carvedImage.getWidth(); x++) {
				originalPixelCoordinate = this.originalCoordinatesTable[y][x];
				int pixelRGB = this.workingImage.getRGB(originalPixelCoordinate.X, originalPixelCoordinate.Y);
				carvedImage.setRGB(x, y, pixelRGB);
			}
		}
		
		return carvedImage;
	}
	
	public BufferedImage showSeams(boolean showVerticalSeams , int seamColorRGB) {
		int numOfSeamsToRemove;
		BufferedImage seamImage;
		
		this.initDPResources();
		if (showVerticalSeams) {
			this.currentMode = VERTICAL;
			numOfSeamsToRemove = Math.abs(this.outWidth - this.inWidth);
		}
		else {
			this.currentMode = HORIZONTAL;
			numOfSeamsToRemove = Math.abs(this.outHeight - this.inHeight);
		}
		carveSeams(numOfSeamsToRemove);
		seamImage = generateSeamImage(seamColorRGB);
		return seamImage;
	}
	
	private BufferedImage generateSeamImage(int seamColorRGB) {
		BufferedImage seamImage = duplicateWorkingImage();
		Coordinate[][] seamList;
		if (this.currentMode == VERTICAL) {
			seamList = this.verticalSeams;
		}
		else {
			seamList = this.horizontalSeams;
		}
		for (Coordinate[] seam : seamList) {
			for (Coordinate XY : seam) {
				seamImage.setRGB(XY.X, XY.Y, seamColorRGB);
			}
		}
		
		return seamImage;
	}
}
