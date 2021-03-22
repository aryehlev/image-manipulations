package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

    //MARK: Fields
    public final Logger logger;
    public final BufferedImage workingImage;
    public final RGBWeights rgbWeights;
    public final int inWidth;
    public final int inHeight;
    public final int workingImageType;
    public final int outWidth;
    public final int outHeight;

    //MARK: Constructors
    public ImageProcessor(Logger logger, BufferedImage workingImage,
                          RGBWeights rgbWeights, int outWidth, int outHeight) {
        super(); //Initializing for each loops...

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

    //MARK: Change picture hue - example
    public BufferedImage changeHue() {
        logger.log("Prepareing for hue changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed() / max;
            int green = g * c.getGreen() / max;
            int blue = b * c.getBlue() / max;
            Color color = new Color(red, green, blue);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("Changing hue done!");

        return ans;
    }

    //MARK: Nearest neighbor - example
    public BufferedImage nearestNeighbor() {
        logger.log("applies nearest neighbor interpolation.");
        BufferedImage ans = newEmptyOutputSizedImage();

        pushForEachParameters();
        setForEachOutputParameters();

        forEach((y, x) -> {
            int imgX = (int) Math.round((x * inWidth) / ((float) outWidth));
            int imgY = (int) Math.round((y * inHeight) / ((float) outHeight));
            imgX = Math.min(imgX, inWidth - 1);
            imgY = Math.min(imgY, inHeight - 1);
            ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
        });

        popForEachParameters();

        return ans;
    }

    //MARK: Unimplemented methods
    public BufferedImage greyscale() {
        logger.log("Prepareing for grey scaling the picture...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed();
            int green = g * c.getGreen();
            int blue = b * c.getBlue();
            int gray = (red + green + blue) / (r + g + b);
            Color color = new Color(gray, gray, gray);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("GrayScale done!");

        return ans;
    }

    public BufferedImage gradientMagnitude() {
        logger.log("calculating the gradient magnitude");
        if (this.workingImage.getWidth() < 2) {
            throw new BadDimentionsException(this.workingImage.getWidth());
        }
        if (this.workingImage.getHeight() < 2) {
            throw new BadDimentionsException(this.workingImage.getHeight());
        }

        BufferedImage greyScaledImage = this.greyscale();
		BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
        	int gradient = gradientCalc(y, x, greyScaledImage);
            Color color = new Color(gradient,  gradient,  gradient);
            ans.setRGB(x, y, color.getRGB());
        });
        logger.log("gradient magnitude done!");

        return ans;
    }

    private int gradientCalc(int y, int x, BufferedImage greyScaledImage){
		int dy = 0;
		int dx = 0;
		int colour = (new Color(greyScaledImage.getRGB(x, y))).getBlue();

		if (y == greyScaledImage.getHeight() - 1) {
			dy =   colour- (new Color(greyScaledImage.getRGB(x, y - 1)).getBlue());
		} else {
			dy = colour - (new Color(greyScaledImage.getRGB(x, y + 1)).getBlue());
		}
		if (x == greyScaledImage.getWidth() - 1) {
			dx = colour - (new Color(greyScaledImage.getRGB(x - 1, y)).getBlue());
		} else {
			dx = colour - (new Color(greyScaledImage.getRGB(x + 1, y)).getBlue());

		}

		int gradient = (int) Math.min(Math.sqrt((Math.pow(dx, 2) + Math.pow(dy, 2)) / 2), 255);
		return gradient;
	}


    public BufferedImage bilinear() {
        logger.log("applying bilinear interpolation.");
        BufferedImage ans = newEmptyOutputSizedImage();

        pushForEachParameters();
        setForEachOutputParameters();

        forEach((y, x) -> {
            double relativeX = (double)(x * inWidth) / outWidth;
            double relativeY = (double)(y * inHeight) / outHeight;

            int x1 = Math.min((int)relativeX, inWidth - 1);
            int y1 = Math.min((int)relativeY, inHeight - 1);
            int x2 = Math.min(x1 + 1, inWidth - 1);
            int y2 = Math.min(y1 + 1, inHeight - 1);

            Color color = this.bilinearCalc(x1, y1, x2, y2, relativeX, relativeY);

            ans.setRGB(x, y, color.getRGB());
        });

        popForEachParameters();

        return ans;
    }

    private Color bilinearCalc(int x1, int y1, int x2, int y2, double x, double y) {
        Color x1y1 = new Color(this.workingImage.getRGB(x1, y1));
        Color x1y2 = new Color(this.workingImage.getRGB(x1, y2));
        Color x2y1 = new Color(this.workingImage.getRGB(x2, y1));
        Color x2y2 = new Color(this.workingImage.getRGB(x2, y2));

        double t = (x - x1) / (x2 - x1);

        Color linearUpper = this.linearCalc(x1y1, x2y1, t);
        Color linearLower= this.linearCalc(x1y2, x2y2, t);

        double s = (y - y2) / (y2 - y1);

        Color newColor = this.linearCalc(linearUpper, linearLower, s);

        return newColor;
    }

    private Color linearCalc(Color c1, Color c2, double weight){
        Color linear = new Color(this.calcWithWeight(c1.getRed(), c2.getRed(), weight), this.calcWithWeight(c1.getGreen(), c2.getGreen(), weight), this.calcWithWeight(c1.getBlue(), c2.getBlue(), weight));
        return linear;
    }

    private int calcWithWeight(int c1, int c2,  double weight){
        return Math.min(Math.max((int)((1 - weight) * c1 + weight * c2), 0), 255);
    }
    //MARK: Utilities
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

    public final BufferedImage duplicateWorkingImage() {
        BufferedImage output = newEmptyInputSizedImage();

        forEach((y, x) ->
                output.setRGB(x, y, workingImage.getRGB(x, y))
        );

        return output;
    }
}
