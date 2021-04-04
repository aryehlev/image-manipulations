package src;

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
                ans.setRGB(x,y,this.workingImage.getRGB(this.originalCoordinates[y][x].X,this.originalCoordinates[y][x].Y));
            }
        }
        return ans;
    }

    private void deleteAllSeams(int numOfVerticalSeams, int numOfHorizontalSeams, CarvingScheme carvingScheme){
        if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            deleteVerticalSeams(numOfVerticalSeams);
            deleteHorizontalSeams(numOfHorizontalSeams);
        }
        else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            deleteHorizontalSeams(numOfHorizontalSeams);
            deleteVerticalSeams(numOfVerticalSeams);

        }else{
            this.deleteSeamsIntermidiatly(numOfVerticalSeams, numOfHorizontalSeams);
        }
    }

    private void deleteVerticalSeams(int numOfVerticalSeams){
        for (int i = 0; i < numOfVerticalSeams; i++) {
            deleteMinimalVerticalSeam();
        }
    }

    private void deleteHorizontalSeams(int numOfHorizontalSeams){
        for (int i = 0; i < numOfHorizontalSeams; i++) {
            deleteMinimalHorizontalSeam();
        }
    }
    private void deleteSeamsIntermidiatly(int numOfVerticalSeams, int numOfHorizontalSeams) {

        while(numOfVerticalSeams > 0 || numOfHorizontalSeams > 0){
            if (numOfVerticalSeams > 0 ) {
                deleteMinimalVerticalSeam();
                numOfVerticalSeams--;
            }

            if (numOfHorizontalSeams > 0){
                deleteMinimalHorizontalSeam();
                numOfHorizontalSeams--;
            }
        }
    }

    private void deleteMinimalVerticalSeam() {

    	this.computeCosts(true);

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
    		this.verticalShift(y, index);
    		index = index + this.costsBackTack[y][index];
		}

    	this.verticalSeamCoordinates.add(seamToRemove);
    	this.currentWidth--;
    }

    private void deleteMinimalHorizontalSeam() {

        this.computeCosts(false);

        double minCost = Double.MAX_VALUE;
        int index = -1;

        for(int y = 0; y < this.currentHeight; y++){
            if(this.costs[y][this.currentWidth-1] < minCost){
                minCost = costs[y][this.currentWidth-1];
                index = y;
            }
        }


        Coordinate[] seamToRemove = new Coordinate[this.currentWidth];
        for(int x = this.currentWidth - 1; x >= 0; x--){


            seamToRemove[x] = this.originalCoordinates[index][x];
            this.horizontalShift(index, x);
            index = index + this.costsBackTack[index][x];
        }
        this.horizontalSeamCoordinates.add(seamToRemove);
        this.currentHeight--;
    }

	private void verticalShift(int y, int index) {
		this.originalCoordinates[y][index] = null;
    	for(int x = index; x < currentWidth - 1; x++){
    		this.originalCoordinates[y][x] = this.originalCoordinates[y][x+1];
            this.carvedImage[y][x] = carvedImage[y][x+1];
		}
	}

	private  void horizontalShift(int index, int x){
        this.originalCoordinates[index][x] = null;
        for(int y = index; y < currentHeight - 1; y++){
            this.originalCoordinates[y][x] = this.originalCoordinates[y+1][x];
            this.carvedImage[y][x] = carvedImage[y+1][x];
        }
    }


	private void computeCosts(boolean vertical) {
        if (vertical) {
            for (int y = 0; y < this.currentHeight; y++) {
                for (int x = 0; x < this.currentWidth; x++) {
                    this.computeMinimumCostForSeamsVertical(y, x);
                }
            }
        }else{
            for (int x = 0; x < this.currentWidth; x++){
                for (int y = 0; y < this.currentHeight; y++)  {
                    this.computeMinimumCostForSeamsHorizontal(y, x);
                }
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

            double minCost = 0;

            if (costRight < costUp && costRight < costLeft  && x < this.currentWidth - 1) {
                camefrom = 1;
                minCost = costRight;
            }
            else if (costLeft < costUp && costLeft < costRight && x > 0) {
                camefrom = - 1;
                minCost = costLeft;
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

    private void computeMinimumCostForSeamsHorizontal(int y, int x) {
        int camefrom = 0;
        int cBehind = 255;
        int cAbove = 255;
        int cBelow = 255;


        if (x > 0) {
            double mBehind = this.costs[y][x - 1];
            double mAbove = Double.MAX_VALUE / 2;
            double mBelow = Double.MAX_VALUE / 2;

            if (y > 0 && y < this.currentHeight - 1) {
                cBehind = Math.abs(this.carvedImage[y-1][x] - this.carvedImage[y + 1][x]);
                cAbove = cBehind;
                cBelow = cAbove;
            }

            if (y > 0) {
                cAbove += Math.abs(this.carvedImage[y-1][x] - this.carvedImage[y][x-1]);
                mAbove = this.costs[y - 1][x - 1];
            }

            if (y  < this.currentHeight - 1) {
                cBelow += Math.abs(this.carvedImage[y][x-1] - this.carvedImage[y+1][x]);
                mBelow = this.costs[y + 1][x - 1];
            }

            double costAbove = mAbove + cAbove;
            double costBehind = mBehind + cBehind;
            double costBelow = mBelow + cBelow;

            double minCost = 0;

            if (costBelow < costBehind && costBelow < costAbove  && y < this.currentWidth - 1) {
                camefrom = 1;
                minCost = costBelow;
            }
            else if (costAbove < costBehind && costAbove < costBelow && y > 0) {
                camefrom = - 1;
                minCost = costAbove;
            }else{
                minCost = costBehind;
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

        this.deleteAllSeams(numberOfVerticalSeamsToCarve,  numberOfHorizontalSeamsToCarve, carvingScheme);
        return this.reconstructImage();
    }

    private BufferedImage showHorizontalSeams(int numOfHorizontalSeams, int seamColorRGB) {
        deleteHorizontalSeams(numOfHorizontalSeams);
        BufferedImage outputImage = this.duplicateWorkingImage();

        for (Coordinate[] seam : this.horizontalSeamCoordinates){
            for(int i = 0; i < seam.length; i++){
                outputImage.setRGB(seam[i].X, seam[i].Y,seamColorRGB);
            }
        }

        return outputImage;
    }

    private BufferedImage showVerticalSeams(int numOfVerticalSeams, int seamColorRGB) {
        deleteVerticalSeams(numOfVerticalSeams);
        BufferedImage outputImage = this.duplicateWorkingImage();

        for (Coordinate[] seam : this.verticalSeamCoordinates){
            for(int i = 0; i < seam.length; i++){
                outputImage.setRGB(seam[i].X, seam[i].Y,seamColorRGB);
            }
        }
        return outputImage;
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
        BufferedImage shownSeamImage;
        if (showVerticalSeams){
            shownSeamImage = this.showVerticalSeams(numberOfVerticalSeamsToCarve, seamColorRGB);
        }else{
            shownSeamImage = this.showHorizontalSeams(numberOfHorizontalSeamsToCarve, seamColorRGB);
        }
        return shownSeamImage;

    }


}
