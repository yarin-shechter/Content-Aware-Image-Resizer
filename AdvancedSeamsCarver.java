package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class AdvancedSeamsCarver extends BasicSeamsCarver {
	private CarvingScheme carveScheme;
	private int numOfVerticalSeamsToCarve;
	private int numOfHorizontalSeamsToCarve;
	private int numOfSeamsToCarveInFirstDimension;
	private int numOfSeamsToCarveInSecondDimension;
	private int firstMode;
	private int secondMode;
	private boolean firstDimensionScalesUp;
	private boolean secondDimensionScalesUp;
	
	public AdvancedSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super(logger, workingImage, outWidth, outHeight, rgbWeights);
		this.numOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		this.numOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
	}
	
	public BufferedImage resizeWithSeamCarving(CarvingScheme carveScheme) {
		if (Math.abs(this.outWidth - this.inWidth) > this.inWidth / 2 || Math.abs(this.outHeight - this.inHeight) > this.inHeight / 2) {
			throw new RuntimeException("Can not apply seam carving: too many seams.");
		}
		logger.log("Scaling image width to " + this.outWidth + " pixels, and height to " + this.outHeight + " pixels.");
		if (this.outWidth <= this.inWidth && this.outHeight <= this.inHeight) {
			return carveImage(carveScheme);
		}
		else if (carveScheme == CarvingScheme.INTERMITTENT) {
			throw new IllegalArgumentException("Intermittent carving is not supported in upscaling.");
		}
		else {
			return scaleImageUpAndOrDown(carveScheme);
		}
	}
	
	private BufferedImage scaleImageUpAndOrDown(CarvingScheme carveScheme) {
		int outWidth = this.outWidth;
		int outHeight = this.outHeight;
		BufferedImage firstStepResult;
		
		this.carveScheme = carveScheme;
		prepareForRequiredScheme();
		this.currentMode = this.firstMode;
		firstStepResult = scaleInOneDimension(this.numOfSeamsToCarveInFirstDimension, this.firstDimensionScalesUp);
		this.swapWorkingImage(firstStepResult);
		this.setOutWidth(outWidth);
		this.setOutHeight(outHeight);
		this.prepareForSeamCarving();
		this.currentMode = this.secondMode;
		return scaleInOneDimension(this.numOfSeamsToCarveInSecondDimension, this.secondDimensionScalesUp);	
	}
	
	private void prepareForRequiredScheme() {
		if (carveScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
			firstMode = VERTICAL;
			secondMode = HORIZONTAL;
			numOfSeamsToCarveInFirstDimension = numOfVerticalSeamsToCarve;
			numOfSeamsToCarveInSecondDimension = numOfHorizontalSeamsToCarve;
			firstDimensionScalesUp = this.outWidth > this.inWidth;
			secondDimensionScalesUp = this.outHeight > this.inHeight;
			this.setOutHeight(inHeight);
		}
		else {
			firstMode = HORIZONTAL;
			secondMode = VERTICAL;
			numOfSeamsToCarveInFirstDimension = numOfHorizontalSeamsToCarve;
			numOfSeamsToCarveInSecondDimension = numOfVerticalSeamsToCarve;
			firstDimensionScalesUp = this.outHeight > this.inHeight;
			secondDimensionScalesUp = this.outWidth > this.inWidth;
			this.setOutWidth(inWidth);
		}
	}
	
	private BufferedImage scaleInOneDimension(int numOfSeamsToCarve, boolean scaleUp) {
		BufferedImage scalingResult;
		this.carveSeams(numOfSeamsToCarve);
		if (scaleUp) {
			scalingResult = scaleImageWidthOrHeightUp();
		}
		else {
			scalingResult = this.generateCarvedImage();
		}
		this.swapWorkingImage(scalingResult);
		return scalingResult;
	}
	
	private BufferedImage scaleImageWidthOrHeightUp() {
		int[][] workingImage = duplicateWorkingImageAs2DArray();
		int[][] result = new int[outHeight][outWidth];
		int[][] transposedSeams;
		
		transposedSeams = transposeAndSortSeams();
		if (this.currentMode == VERTICAL) {
			for (int y = 0 ; y < inHeight; y++) {
				integrateSeamPixelsIntoRow(workingImage, result, y, transposedSeams[y]);
			}
		}
		else {
			for (int x = 0 ; x < inWidth; x++) {
				integrateSeamPixelsIntoColumn(workingImage, result, x, transposedSeams[x]);
			}
		}
		
		BufferedImage resultImage = convertRGBMatrixToBufferedImage(result);
		
		return resultImage;
	}
	
	private int[][] transposeAndSortSeams(){
		Coordinate[][] relevantSeamMatrix;
		if (this.currentMode == VERTICAL) {
			relevantSeamMatrix = this.verticalSeams;
		}
		else {
			relevantSeamMatrix = this.horizontalSeams;
		}
		
		int[][] seamMatrix = extractSignificantCoordinatesFromSeamMatrix(relevantSeamMatrix);
		seamMatrix = transposeMatrix(seamMatrix);
		sortMatrixRows(seamMatrix);
		
		return seamMatrix;
	}
	
	private int[][] extractSignificantCoordinatesFromSeamMatrix(Coordinate[][] seamMatrix){
		int [][] significantCoordinatesSeamMatrix = new int[seamMatrix.length][seamMatrix[0].length];
		for (int i = 0 ; i < seamMatrix.length; i++) {
			for (int j = 0 ; j < seamMatrix[0].length; j++) {
				int significantCoordinate;
				if (this.currentMode == VERTICAL) {
					significantCoordinate = seamMatrix[i][j].X;
				}
				else {
					significantCoordinate = seamMatrix[i][j].Y;
				}
				significantCoordinatesSeamMatrix[i][j] = significantCoordinate;
			}
		}
		return significantCoordinatesSeamMatrix;
	}
	
	private static int[][] transposeMatrix(int[][] matrix){
	    int height = matrix.length;
	    int width = matrix[0].length;

	    int[][] transposedMatrix = new int[width][height];
	    for(int y = 0; y < height; y++) {
	        for(int x = 0; x < width; x++) {
	        	transposedMatrix[x][y] = matrix[y][x];
	        }
	    }
	    return transposedMatrix;
	}
	
	private static void sortMatrixRows(int[][] matrix){
		for(int[] arr : matrix)
			Arrays.sort(arr);
	}
	
	private void integrateSeamPixelsIntoRow(int[][] workingImage, int[][] outImage, int y, int[] xCoordinatesToDuplicate) {
		int x = 0;
		int i = 0;
		int numberOfPixelsDuplicatedSoFar = 0;
		while (x < workingImage[0].length) {
			outImage[y][x + numberOfPixelsDuplicatedSoFar] = workingImage[y][x];
			if (i < xCoordinatesToDuplicate.length && x == xCoordinatesToDuplicate[i]) {
				numberOfPixelsDuplicatedSoFar++;
				outImage[y][x + numberOfPixelsDuplicatedSoFar] = workingImage[y][x];
				i++;
			}
			x++;
		}
	}
	
	private void integrateSeamPixelsIntoColumn(int[][] workingImage, int[][] outImage, int x, int[] yCoordinatesToDuplicate) {
		int y = 0;
		int i = 0;
		int numberOfPixelsDuplicatedSoFar = 0;
		while (y < workingImage.length) {
			outImage[y + numberOfPixelsDuplicatedSoFar][x] = workingImage[y][x];
			if (i < yCoordinatesToDuplicate.length && y == yCoordinatesToDuplicate[i]) {
				numberOfPixelsDuplicatedSoFar++;
				outImage[y + numberOfPixelsDuplicatedSoFar][x] = workingImage[y][x];
				i++;
			}
			y++;
		}
	}
	
	private BufferedImage convertRGBMatrixToBufferedImage(int [][] RGBMatrix) {
		BufferedImage resultImage = newEmptyOutputSizedImage();
		for (int y = 0; y < RGBMatrix.length; y++) {
			for (int x = 0; x < RGBMatrix[0].length; x++) {
				int rgb = RGBMatrix[y][x];
				Color c = new Color(getRed(rgb), getGreen(rgb), getBlue(rgb));
				resultImage.setRGB(x, y, c.getRGB());
			}
		}
		
		return resultImage;
	}
	
	private int[][] duplicateWorkingImageAs2DArray() {
		int[][] image = new int[inHeight][inWidth];
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			image[y][x] = getRGB(r, g, b);
		});
		
		return image;
	}
	
	private static int getRGB(int r, int g, int b) {
		return (r << 16) | (g << 8) | b;
	}
	
	private static int getRed(int rgb) {
		return rgb >> 16;
	}

	private static int getGreen(int rgb) {
		return (rgb >> 8) & 0xFF;
	}
	
	private static int getBlue(int rgb) {
		return rgb & 0xFF;
	}
}