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
    BufferedImage carvedImage;
    private int[][] costsBackTack;
    private int currentWidth;
    private int currentHeight;

    private Coordinate[][] originalCoordinates;
    private ArrayList<Coordinate[]> horizontalSeamCoordinates;
    private ArrayList<Coordinate[]> verticalSeamCoordinates;
    private int numOfVerticalSeams;
    private int numOfHorizontalSeams;

    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        // TODO : Include some additional initialization procedures.

        this.currentWidth = getForEachWidth();
        this.currentHeight = getForEachHeight();

//		this.costsHorizontal = this.computeCostsHorizontal();
        this.costs = new double[currentHeight][currentWidth];
        this.costsBackTack = new int[currentHeight][currentWidth];

        this.carvedImage = this.greyscale();
        this.numOfHorizontalSeams = workingImage.getWidth() - outWidth;
        this.numOfVerticalSeams = workingImage.getHeight() - outHeight;
        this.horizontalSeamCoordinates = new ArrayList<Coordinate[]>();
        this.verticalSeamCoordinates = new ArrayList<Coordinate[]>();
//		this.computeCosts();
    }

    private BufferedImage reconstructImage(){
        BufferedImage ans = newEmptyOutputSizedImage();
        System.out.println("current width = " + currentWidth +"current hieght =" + this.currentHeight);
        System.out.println("output width = " + this.outWidth + "output height = " + outHeight);
        System.out.println("num of vertical seams = " + this.numOfVerticalSeams);
        for(int x = 0; x < currentWidth; x++){
            for(int y = 0; y < currentHeight; y++){
                ans.setRGB(x,y,this.carvedImage.getRGB(x,y));
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
    	double minCost = Double.MIN_VALUE;
    	int index = -1;
    	for(int x = 0; x < this.currentWidth; x++){
    		if(this.costs[this.currentHeight-1][x] < minCost){
    			minCost = costs[currentHeight-1][x];
    			index = x;
			}
		}
    	Coordinate[] seamToRemove = new Coordinate[this.currentHeight - 1];
    	for(int y = this.currentHeight - 1; y > 0; y--){
    		seamToRemove[y] = this.originalCoordinates[y][index];
    		this.shiftLeft(y, index);
    		index = index + this.costsBackTack[y][index];
		}
    	this.currentWidth--;
        System.out.println("aaaaaaaaaaa");


    }

	private void shiftLeft(int y, int index) {
		this.originalCoordinates[y][index] = null;
    	for(int x = index; x < currentWidth - 1; x++){
    		this.originalCoordinates[y][x] = this.originalCoordinates[y][x+1];
            this.carvedImage.setRGB(index,y,carvedImage.getRGB(index+1,y));
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
        double cost = 0;
        if (y == 0) {
            cost = this.pixelEnergy(y, x);
            this.costs[y][x] = cost;
        } else {

            cost = this.pixelEnergy(y, x);

            double minCost = 0;

            double costLeft = this.costs[y - 1][x - 1] + this.computeCostSeams(y, x, costPossibilities.left);
            double costAboveLR = this.costs[y - 1][x] + this.computeCostSeams(y, x, costPossibilities.aboveLR);
            double costRight = this.costs[y - 1][Math.min(this.currentWidth, x + 1)] + this.computeCostSeams(y, x, costPossibilities.Right);


            int direction = -2;


            if (costLeft < costRight && costLeft < costAboveLR) {
                minCost = costLeft;
                direction = -1;
            } else if (costRight < costLeft && costRight < costAboveLR) {
                minCost = costRight;
                direction = 1;
            } else {
                minCost = costAboveLR;
                direction = 0;
            }
            this.costsBackTack[y][x] = direction;
            this.costs[y][x] = minCost + cost;
        }
    }

    private double computeCostSeams(int y, int x, costPossibilities direction) {
        double cost = 0;
        double cost2 = 0;
        double cost1 = 0;
        switch (direction) {
            case left:
                cost1 = Math.abs((new Color(this.carvedImage.getRGB(Math.max(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.max(y - 1, 0))).getRed()));
                cost2 = Math.abs((new Color(this.carvedImage.getRGB(x, Math.max(y - 1, 0)))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.min(y + 1, this.currentHeight))).getRed()));
                cost = cost1 + cost2;
                break;
            case Right:
                cost1 = Math.abs((new Color(this.carvedImage.getRGB(Math.max(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.min(y + 1, this.currentHeight))).getRed()));
                cost2 = Math.abs((new Color(this.carvedImage.getRGB(x, Math.max(y - 1, 0)))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.min(y + 1, this.currentHeight))).getRed()));
                cost = cost1 + cost2;
                break;
            case aboveLR:
                cost = Math.abs((new Color(this.carvedImage.getRGB(x, Math.max(y - 1, 0)))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.min(y + 1, this.currentHeight))).getRed()));
                break;
            case above:
                cost1 = Math.abs((new Color(this.carvedImage.getRGB(Math.max(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(Math.min(x + 1, this.currentWidth), y))).getRed());
                cost2 = Math.abs((new Color(this.carvedImage.getRGB(Math.max(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(x, Math.max(y - 1, 0))).getRed()));

                cost = cost1 + cost2;
                break;
            case below:
                cost1 = Math.abs(new Color(this.carvedImage.getRGB(x, Math.min(y - 1, 0))).getRed() - (new Color(this.carvedImage.getRGB(Math.max(x + 1, getForEachWidth()), y))).getRed());
                cost2 = Math.abs((new Color(this.carvedImage.getRGB(Math.min(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(Math.max(x + 1, getForEachWidth()), y))).getRed());

                cost = cost1 + cost2;
                break;
            case behind:
                cost = Math.abs((new Color(this.carvedImage.getRGB(Math.min(x - 1, 0), y))).getRed() - (new Color(this.carvedImage.getRGB(Math.max(x + 1, getForEachWidth()), y))).getRed());
                break;
        }
        return cost;
    }

    private double pixelEnergy(Integer y, Integer x) {
        Color currentColour = new Color(this.carvedImage.getRGB(x, y));
        Color verticalColour = null;
        Color horizontalColour = null;


        if (y == getForEachHeight() - 1) {
            verticalColour = new Color(this.carvedImage.getRGB(x, y - 1));
        } else {
            verticalColour = new Color(this.carvedImage.getRGB(x, y + 1));
        }

        if (x == getForEachWidth() - 1) {
            horizontalColour = new Color(this.carvedImage.getRGB(x - 1, y));
        } else {
            horizontalColour = new Color(this.carvedImage.getRGB(x + 1, y));
        }

        double horizontal = Math.abs(currentColour.getBlue() - horizontalColour.getBlue());
        double vertical = Math.abs(currentColour.getBlue() - verticalColour.getBlue());

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
