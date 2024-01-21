package edu.cg.menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.cg.RGBWeights;
import edu.cg.AdvancedSeamsCarver;
import edu.cg.ImageProcessor;
import edu.cg.Logger;
import edu.cg.BasicSeamsCarver;
import edu.cg.BasicSeamsCarver.CarvingScheme;
import edu.cg.menu.components.ActionsController;
import edu.cg.menu.components.CarvingSchemeSelector;
import edu.cg.menu.components.ColorMixer;
import edu.cg.menu.components.ImagePicker;
import edu.cg.menu.components.LogField;
import edu.cg.menu.components.ScaleSelector;
import edu.cg.menu.components.ScaleSelector.ResizingOperation;

@SuppressWarnings("serial")
public class MenuWindow extends JFrame implements Logger {
	//MARK: fields
	private static final boolean SHOWVERTICALSEAMS = true;
	private static final boolean SHOWHORIZONTALSEAMS = false;
	private BufferedImage workingImage;
	private String imageTitle;
	
	//MARK: GUI fields
	private ImagePicker imagePicker;
	private ColorMixer colorMixer;
	private ScaleSelector scaleSelector;
	private ActionsController actionsController;
	private LogField logField;
	private CarvingSchemeSelector schemeSelector;
	
	public MenuWindow() {
		super();
		
		setTitle("Ex1: Image Processing Application");
		//The following line makes sure that all application threads are terminated when this window is closed.
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		imagePicker = new ImagePicker(this);
		colorMixer = new ColorMixer();
		scaleSelector = new ScaleSelector();
		actionsController = new ActionsController(this);
		logField = new LogField();
		schemeSelector = new CarvingSchemeSelector();
		
		contentPane.add(imagePicker, BorderLayout.NORTH);
		
		JPanel panel1 = new JPanel();
		contentPane.add(panel1, BorderLayout.CENTER);
		panel1.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel2 = new JPanel();
		panel1.add(panel2, BorderLayout.CENTER);
		panel2.setLayout(new GridLayout(0, 1, 0, 0));
		
		
		JPanel panel3 = new JPanel();
		panel2.add(panel3, BorderLayout.CENTER);
		panel3.setLayout(new GridLayout(0, 1, 0, 0));
		
		panel3.add(colorMixer);
		panel3.add(scaleSelector);
		panel3.add(schemeSelector);
		panel2.add(actionsController);
		panel1.add(logField);
		
		workingImage = null;
		imageTitle = null;
		
		pack();
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		log("Application started.");
	}
	
	public void changeHue() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BufferedImage img = new ImageProcessor(this,
				duplicateImage(),
				rgbWeights,
				outWidth,
				outHeight).changeHue();
		present(img, "Change hue");
	}
	
	public void greyscale() {
		BufferedImage img = new ImageProcessor(this,
				duplicateImage(),
				colorMixer.getRGBWeights()).greyscale();
		present(img, "Grey scale");
	}
	
	public void gradientMagnitude() {
		BufferedImage img = new ImageProcessor(this,
				duplicateImage(),
				colorMixer.getRGBWeights()).gradientMagnitude();
		present(img, "Gradient magnitude");
	}
	
	public void resize() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		ResizingOperation op = scaleSelector.resizingOperation();
		edu.cg.BasicSeamsCarver.CarvingScheme scheme = schemeSelector.carvingScheme();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BufferedImage img = null;
		String presentMessage = "Resize: " + op.title;
		

		switch(op) {
		case NEAREST_NEIGHBOR:
			img = new ImageProcessor(this,
					duplicateImage(),
					rgbWeights,
					outWidth,
					outHeight).nearestNeighbor();
			break;
			
		case BILINEAR:
			img = new ImageProcessor(this,
					duplicateImage(),
					rgbWeights,
					outWidth,
					outHeight).bilinear();
			break;
			
		default: //seam carving
			AdvancedSeamsCarver carver = new AdvancedSeamsCarver(this, duplicateImage(), outWidth, outHeight, rgbWeights);
			img = carver.resizeWithSeamCarving(scheme);
			presentMessage += ", " + scheme.description + ",";
		}
		
		presentMessage += " [" + outWidth + "][" + outHeight + "]";
		present(img, presentMessage);

	}
	
	public void showSeamsVertical() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BasicSeamsCarver carver = new BasicSeamsCarver(this, duplicateImage(), outWidth, outHeight, rgbWeights);
		BufferedImage verticalSeamImage = carver.showSeams(SHOWVERTICALSEAMS, Color.red.getRGB());
		present(verticalSeamImage, "Show seams vertical");
	}

	public void showSeamsHorizontal() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BasicSeamsCarver carver = new BasicSeamsCarver(this, duplicateImage(), outWidth, outHeight, rgbWeights);
		BufferedImage horizontalSeamImage = carver.showSeams(SHOWHORIZONTALSEAMS, Color.BLACK.getRGB());
		present(horizontalSeamImage, "Show seams horizontal");
	}
	
	private void present(BufferedImage img, String title) {
		if(img == null)
			throw new NullPointerException("Can not present a null image.");
		
		new ImageWindow(img, imageTitle + "; " + title, this).setVisible(true);
	}
	
	private static BufferedImage duplicateImage(BufferedImage img) {
		BufferedImage dup = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		for(int y = 0; y < dup.getHeight(); ++y)
			for(int x = 0; x < dup.getWidth(); ++x)
				dup.setRGB(x, y, img.getRGB(x, y));
		
		return dup;
	}
	
	private BufferedImage duplicateImage() {
		return duplicateImage(workingImage);
	}
	
	public void setWorkingImage(BufferedImage workingImage, String imageTitle) {
		this.imageTitle = imageTitle;
		this.workingImage = workingImage;
		log("Image: " + imageTitle + " has been selected as working image.");
		scaleSelector.setWidth(workingImage.getWidth());
		scaleSelector.setHeight(workingImage.getHeight());
		actionsController.activateButtons();
	}
	
	public void present() {
		new ImageWindow(workingImage, imageTitle, this).setVisible(true);
	}
	
	//MARK: Logger
	@Override
	public void log(String s) {
		logField.log(s);
	}
}
