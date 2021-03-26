package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


public class BasicSeamsCarver extends ImageProcessor {


    // An enum describing the carving scheme used by the seams carver.
    // VERTICAL_HORIZONTAL means vertical seams are removed first.
    // HORIZONTAL_VERTICAL means horizontal seams are removed first.
    // INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
    public static enum CarvingScheme {
        VERTICAL_HORIZONTAL("Vertical seams first"),
        HORIZONTAL_VERTICAL("Horizontal seams first"),
        INTERMITTENT("Intermittent carving");

        public final String description;

        private CarvingScheme(String description) {
            this.description = description;
        }
    }

    private enum costPossibilities {
        left,
        Right,
        aboveLR,
        above,
        below,
        behind
    }

    // A simple coordinate class which assists the implementation.
    protected class Coordinate {
        public int X;
        public int Y;

        public Coordinate(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }
    }

    // TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework
    // instructions PDF to make an educated decision.
    private double[][] costs;
    private int[][] carvedImage;
    private int[][] costsBackTack;
    private int currentWidth;
    private int currentHeight;

    private BufferedImage greyscaledImage;
    private Coordinate[][] originalCoordinates;
    private ArrayList<Coordinate[]> horizontalSeamCoordinates;
    private ArrayList<Coordinate[]> verticalSeamCoordinates;
    private int numOfVerticalSeams;
    private int numOfHorizontalSeams;
    BufferedImage originalImage;

    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        // TODO : Include some additional initialization procedures.

        this.currentWidth = getForEachWidth();
        this.currentHeight = getForEachHeight();

//		this.costsHorizontal = this.computeCostsHorizontal();
        this.costs = new double[currentHeight][currentWidth];
        this.costsBackTack = new int[currentHeight][currentWidth];

        this.greyscaledImage = this.greyscale();
        this.originalImage = this.greyscale();
        this.numOfHorizontalSeams = workingImage.getWidth() - outWidth;
        this.numOfVerticalSeams = workingImage.getHeight() - outHeight;
        this.horizontalSeamCoordinates = new ArrayList<Coordinate[]>();
        this.verticalSeamCoordinates = new ArrayList<Coordinate[]>();
        this.originalCoordinates = new Coordinate[currentHeight][currentWidth];

        this.initCoordinatesMatrix();
		this.initCarvedImage();
    }

    private void initCarvedImage() {
        this.carvedImage = new int[this.currentHeight][this.currentWidth];
        for(int x = 0; x < currentWidth; x++){
            for(int y = 0; y < currentHeight; y++){
                this.carvedImage[y][x] = (new Color(this.greyscaledImage.getRGB(x,y))).getRed();
            }
        }
    }

    private void initCoordinatesMatrix(){
        for(int x = 0; x < currentWidth; x++){
            for(int y = 0; y < currentHeight; y++){
                this.originalCoordinates[y][x] = new Coordinate(x, y);
            }
        }
    }


    private BufferedImage reconstructImage(){
        BufferedImage ans = newEmptyOutputSizedImage();
        System.out.println("current width = " + currentWidth +"current hieght =" + this.currentHeight);
        System.out.println("output width = " + this.outWidth + "output height = " + outHeight);
        System.out.println("num of vertical seams = " + this.numOfVerticalSeams);
        for(int x = 0; x < currentWidth; x++){
            for(int y = 0; y < currentHeight; y++){
                ans.setRGB(x,y,this.originalImage.getRGB(this.originalCoordinates[y][x].X,this.originalCoordinates[y][x].Y));
            }
        }
        return ans;
    }

    private void deleteAllSeams(int numOfVerticalSeams){
        for(int i = 0; i < numOfVerticalSeams; i++){
            deleteMinimalSeam();
        }
    }

    private void deleteMinimalSeam() {

    	this.computeCosts();




        double minCost = Double.MAX_VALUE;
    	int index = -1;
    	for(int x = 0; x < this.currentWidth; x++){
    		if(this.costs[this.currentHeight-1][x] < minCost){
    			minCost = costs[currentHeight-1][x];
    			index = x;
			}
		}


    	Coordinate[] seamToRemove = new Coordinate[this.currentHeight];
    	for(int y = this.currentHeight - 1; y >= 0; y--){


    		seamToRemove[y] = this.originalCoordinates[y][index];
    		this.shiftLeft(y, index);
    		index = index + this.costsBackTack[y][index];
		}

    	this.currentWidth--;

    }

	private void shiftLeft(int y, int index) {
		this.originalCoordinates[y][index] = null;
    	for(int x = index; x < currentWidth - 1; x++){
    		this.originalCoordinates[y][x] = this.originalCoordinates[y][x+1];
            this.carvedImage[y][x] = carvedImage[y][x+1];
		}
	}


	private void computeCosts() {

        for (int y = 0; y < this.currentHeight; y++) {
            for (int x = 0; x < this.currentWidth; x++) {
                this.computeMinimumCostForSeamsVertical(y, x);
            }
        }
    }

    private void computeMinimumCostForSeamsVertical(int y, int x) {
        int camefrom = 0;
        int cLeft = 255;
        int cUp = 255;
        int cRight = 255;


        if (y > 0) {
            double mUp = this.costs[y - 1][x];
            double mRight = Double.MAX_VALUE / 2;
            double mLeft = Double.MAX_VALUE / 2;

            if (x > 0 && x < this.currentWidth - 1) {
                cRight = Math.abs(this.carvedImage[y][x - 1] - this.carvedImage[y][x + 1]);
                cLeft = cRight;
                cUp = cRight;
            }

            if (x > 0) {
                cLeft += Math.abs(this.carvedImage[y][x - 1] - this.carvedImage[y - 1][x]);
                mLeft = this.costs[y - 1][x - 1];
            }

            if (x  < this.currentWidth - 1) {
                cRight += Math.abs(this.carvedImage[y - 1][x] - this.carvedImage[y][x + 1]);
                mRight = this.costs[y - 1][x + 1];
            }

            double costRight = mRight + cRight;
            double costLeft = mLeft + cLeft;
            double costUp = mUp + cUp;

            double minCost = Double.MAX_VALUE;


            if (costRight < minCost && costRight <  costLeft && x < this.currentWidth - 1) {
                camefrom = 1;
                minCost = costRight;
            } else if (costLeft < costUp && costLeft < costRight && x > 0) {
                camefrom = - 1;
                minCost = costRight;
            }else{
                minCost = costUp;
                camefrom = 0;
            }
            this.costs[y][x] = this.pixelEnergy(y, x) + minCost;

        }else{
            this.costs[y][x] = this.pixelEnergy(y, x);
        }

        this.costsBackTack[y][x] = camefrom;
    }

    private double pixelEnergy(int y, int x) {
        int currentColour = carvedImage[y][x];
        int verticalColour = -1;
        int horizontalColour = -1;


        if (y == this.currentHeight - 1) {
            verticalColour = this.carvedImage[y-1][x];
        } else {
            verticalColour = this.carvedImage[y+1][x];
        }

        if (x == this.currentWidth - 1) {
            horizontalColour = this.carvedImage[y][x-1];
        } else {
            horizontalColour = this.carvedImage[y][x+1];
        }

        double horizontal = Math.abs(currentColour - horizontalColour);
        double vertical = Math.abs(currentColour - verticalColour);

        double energy = Math.sqrt(Math.pow(vertical, 2) + Math.pow(horizontal, 2));

        return energy;


    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        // TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
        // and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
        // Note you must consider the 'carvingScheme' parameter in your procedure.
        // Return the resulting image.
        try{
            this.deleteAllSeams(numberOfVerticalSeamsToCarve);
            return this.reconstructImage();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        // TODO :  Present either vertical or horizontal seams on the input image.
        // If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
        // Then, generate a new image from the input image in which you mark all of the vertical seams that
        // were chosen in the Seam Carving process.
        // This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value).
        // Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
        // from the image.
        // Then, generate a new image from the input image in which you mark all of the horizontal seams that
        // were chosen in the Seam Carving process.
        throw new UnimplementedMethodException("showSeams");
    }
}
