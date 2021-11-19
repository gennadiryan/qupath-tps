/*********************************
 * This file is part of ImageCombinerWarpy ...
 * 
 * .. a QuPath extension based on the QuPath 'Interactive Image Alignment' tool
 *  
 * The ImageCombinerWarpy is thought as an experimental˛ tool.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/


package qupath.ext.imagecombinerwarpy.gui;


import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;


public class RealTransformInterpolationSequence {

	public static final int[] interpolationsModes = InterpolationModes.getOrdinalNumbers();
	public static final String[] interpolationsModeNames = InterpolationModes.getInterpolationTypeName();
	
	private int interpolation = 0;
	
	private RealTransform transform = null;
	
	public RealTransformInterpolationSequence() {
		transform = new RealTransformSequence();
	}
	
	public RealTransformInterpolationSequence(RealTransformInterpolationSequence transformInterpolateSequence) {
		int interpolation = transformInterpolateSequence.getInterpolation();
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;
		this.transform = transformInterpolateSequence.getTransform();
	}

	public RealTransformInterpolationSequence(RealTransform transform, int interpolation) {
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;
		if (transform instanceof RealTransformSequence)
			this.transform = (RealTransformSequence)transform;
		else if (transform instanceof InvertibleRealTransformSequence)
			this.transform = (InvertibleRealTransformSequence)transform;

	}

	public RealTransform getTransform() {
		return transform;
	}
	
	public void setTransform(RealTransformSequence transform) {
		this.transform = transform;
	}
	
	public int getInterpolation() {
		return interpolation;
	}

	public String getInterpolationName() {
		return interpolationsModeNames[interpolation];
	}
	
	public void setInterpolation(int interpolation) {
		this.interpolation = interpolation;
	}

	private boolean isValidInterpolation(int interpolation) {
		for (int i=0; i<interpolationsModes.length; i++) {
			if (interpolationsModes[i] == interpolation)
				return true;
		}
		return false;
	}
}
