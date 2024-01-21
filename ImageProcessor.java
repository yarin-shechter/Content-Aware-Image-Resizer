package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	private static final double sqrt2 = Math.sqrt(2.0);
	
	//MARK: fields
	public BufferedImage workingImage;
	public int inWidth;
	public int inHeight;
	public int workingImageType;
	public int outWidth;
	public int outHeight;
	public final Logger logger;
	public final RGBWeights rgbWeights;
	
	
	//MARK: constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final void swapWorkingImage(BufferedImage workingImage) {
		this.workingImage = workingImage;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.setForEachParameters(inWidth, inHeight);
	}
	
	public final void setOutWidth(int width) {
		this.outWidth = width;
	}
	
	public final void setOutHeight(int height) {
		this.outHeight = height;
	}
	
	public BufferedImage greyscale() {
		logger.log("creates a greyscale image.");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int rgb = rgbWeights.weightsSum;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int mean = (r*c.getRed() + g*c.getGreen() + b*c.getBlue()) / rgb;
			Color grey = new Color(mean, mean, mean);
			ans.setRGB(x, y, grey.getRGB());
		});
		
		return ans;
	}

	public BufferedImage gradientMagnitude() {
		logger.log("calculates the gradient magnitude.");
		if(inHeight < 2 | inWidth < 2)
			throw new RuntimeException("Image is too small for calculating gradient magnitude.");
		BufferedImage greyScaledImage = greyscale();
		BufferedImage ans = newEmptyInputSizedImage();
		forEach((y, x) -> {
			int rgb = calculatePixelGradientMagnitude(greyScaledImage, x, y);
			ans.setRGB(x, y, rgb);
		});
		return ans;
	}
	
	private static int calculatePixelGradientMagnitude(BufferedImage greyScaledImage, int x1, int y1) {
		int dx = calculateDx(greyScaledImage, x1, y1);
		int dy = calculateDy(greyScaledImage, x1, y1);
		int magnitude = Math.min((int)(Math.sqrt(dx*dx + dy*dy)), 255);
		return new Color(magnitude, magnitude, magnitude).getRGB();
	}
	
	private static int calculateDx(BufferedImage greyScaledImage, int x1, int y1) {
		int xDifferenceFactor = 1;
		int x2;
		int inWidth = greyScaledImage.getWidth();
		if (x1 == inWidth - 1) {
			xDifferenceFactor = -1;
		}
		x2 = x1 + xDifferenceFactor;
		int x1y1Color = new Color(greyScaledImage.getRGB(x1, y1)).getRed();
		int x2y1Color = new Color(greyScaledImage.getRGB(x2, y1)).getRed();
		return x1y1Color - x2y1Color;
	}
	
	private static int calculateDy(BufferedImage greyScaledImage, int x1, int y1) {
		int yDifferenceFactor = 1;
		int y2;
		int inHeight = greyScaledImage.getHeight();
		if (y1 == inHeight - 1) {
			yDifferenceFactor = -1;
		}
		y2 = y1 + yDifferenceFactor;
		int x1y1Color = new Color(greyScaledImage.getRGB(x1, y1)).getRed();
		int x1y2Color = new Color(greyScaledImage.getRGB(x1, y2)).getRed();
		return x1y1Color- x1y2Color;
	}
	
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	public BufferedImage bilinear() {
		logger.log("applies bilinear interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			float imgX = (x*inWidth) / ((float)outWidth);
			float imgY = (y*inHeight) / ((float)outHeight);
			int x0 = (int)imgX;
			int y0 = (int)imgY;
			int x1 = Math.min(x0+1, inWidth-1);
			int y1 = Math.min(y0+1, inHeight-1);
			float dx = x1 - imgX;
			float dy = y1 - imgY;
			Color c1 = linearX(workingImage, y0, x0, x1, dx);
			Color c2 = linearX(workingImage, y1, x0, x1, dx);
			Color c = weightedMean(c1, c2, dy);
			ans.setRGB(x, y, c.getRGB());
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	private static Color linearX(BufferedImage img, int y, int x0, int x1, float dx) {
		Color c1 = new Color(img.getRGB(x0, y));
		Color c2 = new Color(img.getRGB(x1, y));
		return weightedMean(c1, c2, dx);
	}
	
	private static Color weightedMean(Color c1, Color c2, float delta) {
		float r1 = c1.getRed();
		float g1 = c1.getGreen();
		float b1 = c1.getBlue();
		
		float r2 = c2.getRed();
		float g2 = c2.getGreen();
		float b2 = c2.getBlue();
		
		int r = weightedMean(r1, r2, delta);
		int g = weightedMean(g1, g2, delta);
		int b = weightedMean(b1, b2, delta);
		
		return new Color(r, g, b);
	}
	
	private static int weightedMean(float c1, float c2, float delta) {
		int ans = (int)(c1*delta + (1.0f - delta)*c2);
		return Math.min(Math.max(ans, 0), 255);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
