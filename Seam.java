

import java.awt.Color;


public class Seam {
	/**
	* Constructor
	*/
	public Seam() {

	}
	

	/**
	* Takes in an image, finds the vertical seam, then removes it
	* Nothing is returned since this will modify the given image
	* @param im CSBSJUImage that will be altered
	*/
	public void verticalSeamShrink(CSBSJUImage im) {

		// Use dynamic programming to find the vertical seam
		int[] vseam = findVerticalSeam(im);//we will write this together

		// So it looks good visually
		// 1. mark it
		// 2. pause a moment so it shows up on the screen
		markVerticalSeam(im, vseam);
		im.repaintCurrentDisplayWindow();
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		removeVerticalSeam(im, vseam);
	}
	/**
	 * Helper method for visualization
	 * @param im image to mark with purplish seam to show what will be removed
	 * @param vseam array of pixel indexes that represent x-coordiantes to remove
	 */
	public void markVerticalSeam(CSBSJUImage im, int[] vseam) {
		for (int y = 0; y < vseam.length; y++) {
			int x = vseam[y];
			im.setRGB(x, y, 255, 0, 255);//make them purplish
		}
	}

	/**
	* Takes in an image, flips it, finds the vertical seam, marks the seam, 
	* flips the image back, to display it, flip the image back, then removes the seam, then
	* reorientates it back to original orientation
	* Nothing is returned since this will modify the given image
	* @param im CSBSJUImage that will be altered
	*/
	public void horizontalSeamShrink(CSBSJUImage im) {

		// Transpose the image then find and remove the vertical seam
		im.transpose();

		// Note that I could just call verticalSeamResize at this point if...
		// I didn't want to display the marked seam. I need to display that in
		// the non-transposed version for it to look correct
		// So I'm doing all the steps from that function but with a few
		// modifications for display purposes

		// Use dynamic programming to find the vertical seam
		int[] vseam = findVerticalSeam(im);//we will write this together

		// So it looks good visually
		// 1. mark it
		// 2. pause a moment so it shows up on the screen
		markVerticalSeam(im, vseam);
		im.transpose(); // This is the extra that is needed for the display not
						// to be transposed

		im.repaintCurrentDisplayWindow();
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		im.transpose(); // This is the extra that is needed for the display

		removeVerticalSeam(im, vseam);

		// Transpose the image back to its original orientation
		im.transpose();
	}

	private int[] findVerticalSeam(CSBSJUImage im) {

		//make a matrix to store values
		double[][] energyMatrix = new double[im.getWidth()][im.getHeight()];
		// Calculate the energies of all the pixels
		// We could do this on the fly but we will need each energy 3 times so
		// might as well do each just once and save the results - we can implement dynamic programming!!!
		for (int x = 0; x < im.getWidth(); x++) {
			for (int y = 0; y < im.getHeight(); y++) {
				energyMatrix[x][y] = energy(x, y, im);
			}
		}

		// Matrix to store where you came from as you go down.
		// If "edgeTo[x][y] == z", it means that pixel (x, y)'s best
		// energy sum is achieved if you get there from pixel (z, y-1).
		// Note: z can only be x-1, x, or x+1
		// This is our traceback table
		int[][] edgeTo = new int[im.getWidth()][im.getHeight()];

		// Matrix to store the best energy sum for each pixel
		double[][] distTo = new double[im.getWidth()][im.getHeight()];

		
		
		// Initial the base conditions
		// This will be the top row - the energy to get to each one is just the
		// energy of the pixel itself
		// The top row's distTo[][] is each pixel's energy
		for (int x = 0; x < im.getWidth(); x++) {
			distTo[x][0] = energyMatrix[x][0];
			edgeTo[x][0] = -1; //Stopping value cause there is not next value since we are at top
		}

		for (int y = 1; y < im.getHeight(); y++) { //Already filled in row 0
			for (int x = 0; x < im. getWidth(); x++) {
				//initialize to high values so we can find the min of the possible options below
				distTo[x][y] = Double.POSITIVE_INFINITY;

				//Look at each of the 3 possible pixels (x-1, y-1), (x, y-1), (x+1, y-1)
				for (int k = x-1; k <= x+1; k++) {
					//Check for edge condition - which will mean not using the i for the edge
					if (k >= 0 && k < im.getWidth()) {
						double dist = distTo[k][y-1] + energyMatrix[x][y];
						if (dist < distTo[x][y]) {
							//found a better path
							distTo[x][y] = dist; // record the new best distance
							edgeTo[x][y] = k; //record where the new best distance came from
						}
					}
				}
			}
		}

		// Each position relys on the 3 above it (NW, N, NE)
		// This means we can fill it in row by row
		// Already filled in row 0 so can skip it
		

		// Now we start the traceback we could put this in a separate function... but I am lazy

		// Find the pixel from the bottom row with the smallest energy sum
		int bestBottomX = 0;
		for (int x = 0; x < im.getWidth(); x++) {
			if(distTo[x][im.getHeight()-1] < distTo[bestBottomX][im.getHeight()-1]){
				bestBottomX = x;
			}
		}

		// Trace back from pixel (bestBottomX, height()-1) to the top row
		// to recreate the lowest-energy path (i.e. vertical seam)
		
		int[] seam = new int[im.getHeight()];
		int currentX = bestBottomX;
		int currentY = im.getHeight() - 1;
		while (currentY >= 0) {
			seam[currentY] = currentX;
			currentX = edgeTo[currentX][currentY];
			currentY--;
		}

		return seam;
	}

	/** 
	 * Returns energy of pixel (x, y)
	 * @param x x-coordinate of pixel
	 * @param y y-coordinate of pixel
	 * @param im given image
	 * @return energy of pixel (x, y)
	 */
	public double energy(int x, int y, CSBSJUImage im) {
		if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight())
			throw new IndexOutOfBoundsException("Invalid (x, y)");

		Color x1 = colorOfPixel(x + 1, y, im);
		Color x2 = colorOfPixel(x - 1, y, im);
		Color y1 = colorOfPixel(x, y + 1, im);
		Color y2 = colorOfPixel(x, y - 1, im);

		return calculateGradient(x1, x2) + calculateGradient(y1, y2);
	}

	/**
	* Return the color of pixel at (x, y)
	* if x or y are not too far beyond the image bounds, wrap around
	* The default of CSBSJUImage is to just return 0 rather than wrap around
	* @param x x-coordinate of pixel in given image
	* @param y y-coordinate of pixel in given image
	* @return the color of pixel (x, y)
	*/
	private Color colorOfPixel(int x, int y, CSBSJUImage im) {
		int W = im.getWidth();
		int H = im.getHeight();
		return new Color(im.getRed((x + W) % W, (y + H) % H), im.getRed((x + W) % W, (y + H) % H),
				im.getRed((x + W) % W, (y + H) % H));

	}

	/**
	* Calculates the gradient between to two pixel colors
	* @param p1 Color of pixel 1
	* @param p2 Color of pixel 2
	* @return square of the (R,G,B) gradient between two pixels p1 and p2
	*/
	private double calculateGradient(Color p1, Color p2) {
		double r = Math.abs(p1.getRed() - p2.getRed());
		double g = Math.abs(p1.getGreen() - p2.getGreen());
		double b = Math.abs(p1.getBlue() - p2.getBlue());
		return r * r + g * g + b * b;
	}

	/**
	* Removes the pixels along the given seam of given image
	* Get rid of the pixels with coordinates (seam[y], y)
	* @param im image that seam will be removed from
	* @param vseam array of index locations to remove from image
	*/
	public void removeVerticalSeam(CSBSJUImage im, int[] vseam) {
		//Make a new image that is one less pixel wider than given
		CSBSJUImage resized = new CSBSJUImage(im.getWidth() - 1, im.getHeight());
		// Get rid of the pixels with coordinates (seam[y], y)
		for (int y = 0; y < im.getHeight(); y++) {
			int k = 0;
			for (int x = 0; x < im.getWidth(); x++) {
				// Skip the one that was the seam
				if (x != vseam[y]) {
					resized.setRGB(k++, y, im.getRed(x, y), im.getGreen(x, y), im.getBlue(x, y));
				}
			}
		}

		im.switchImage(resized);
	}

	
	
}
