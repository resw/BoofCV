/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.feature.detect.line;


import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.edge.GradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughTransformLineFootOfNorm;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt8;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LineParametric2D_F32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects lines inside the image by breaking it up into subimages for improved precision.  Inside
 * each subimage a hough transform is independently computed. See [1] for more details.
 * </p>
 *
 * <p>
 * USAGE NOTES: Blurring the image prior to processing can often improve performance.
 * Results will not be perfect and to detect all the obvious lines in the image several false
 * positives might be returned.
 * </p>
 *
 * <p>
 * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
 * </p>
 *
 * @see boofcv.alg.feature.detect.line.HoughTransformLineFootOfNorm
 *
 * @author Peter Abeles
 */
public class DetectLineHoughFootSubimage<I extends ImageBase, D extends ImageBase>
		implements DetectLine<I>
{
	int totalHorizontalDivisions;
	int totalVerticalDivisions;

	// transform algorithm
	HoughTransformLineFootOfNorm alg;

	// computes image gradient
	ImageGradient<I,D> gradient;

	// used to create binary edge image
	float thresholdEdge;

	// image gradient
	D derivX;
	D derivY;

	// edge intensity image
	ImageFloat32 intensity = new ImageFloat32(1,1);

	// detected edge image
	ImageUInt8 binary = new ImageUInt8(1,1);

	// used for suppressing all but the most intense edge pixels
	ImageFloat32 suppressed = new ImageFloat32(1,1);
	ImageFloat32 angle = new ImageFloat32(1,1);
	ImageSInt8 direction = new ImageSInt8(1,1);

	/**
	 * Specifies detection parameters.  The suggested parameters should be used as a starting point and will
	 * likely need to be tuned significantly for each different scene.
	 *
	 * @param localMaxRadius Lines in transform space must be a local max in a region with this radius. Try 5;
	 * @param minCounts Minimum number of counts/votes inside the transformed image. Try 5.
	 * @param minDistanceFromOrigin Lines which are this close to the origin of the transformed image are ignored.  Try 5.
	 * @param thresholdEdge Threshold for classifying pixels as edge or not.  Try 30.
	 * @param gradient Computes the image gradient.
	 */
	public DetectLineHoughFootSubimage(int localMaxRadius,
									   int minCounts,
									   int minDistanceFromOrigin,
									   float thresholdEdge,
									   int totalHorizontalDivisions ,
									   int totalVerticalDivisions ,
									   ImageGradient<I, D> gradient)
	{
		this.gradient = gradient;
		this.thresholdEdge = thresholdEdge;
		this.totalHorizontalDivisions = totalHorizontalDivisions;
		this.totalVerticalDivisions = totalVerticalDivisions;
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmaxCandidate(localMaxRadius, minCounts, 0, true);
		alg = new HoughTransformLineFootOfNorm(extractor,minDistanceFromOrigin);
		derivX = GeneralizedImageOps.createImage(gradient.getDerivType(),1,1);
		derivY = GeneralizedImageOps.createImage(gradient.getDerivType(),1,1);
	}

	@Override
	public List<LineParametric2D_F32> detect(I input) {
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		intensity.reshape(input.width,input.height);
		binary.reshape(input.width,input.height);
		angle.reshape(input.width,input.height);
		direction.reshape(input.width,input.height);
		suppressed.reshape(input.width,input.height);

		gradient.process(input,derivX,derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, intensity);

		GGradientToEdgeFeatures.direction(derivX, derivY, angle);
		GradientToEdgeFeatures.discretizeDirection4(angle, direction);

		GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction, suppressed);

		ThresholdImageOps.threshold(suppressed, binary, thresholdEdge, false);

		List<LineParametric2D_F32> ret = new ArrayList<LineParametric2D_F32>();

		for( int i = 0; i < totalVerticalDivisions; i++ ) {
			int y0 = input.height*i/totalVerticalDivisions;
			int y1 = input.height*(i+1)/totalVerticalDivisions;

			for( int j = 0; j < totalHorizontalDivisions; j++ ) {
				int x0 = input.width*j/totalVerticalDivisions;
				int x1 = input.width*(j+1)/totalVerticalDivisions;

				processSubimage(x0,y0,x1,y1,ret);
			}
		}

		// todo remove duplicate lines

		return ret;
	}

	private void processSubimage( int x0 , int y0 , int x1 , int y1 ,
								  List<LineParametric2D_F32> found ) {
		D derivX = (D)this.derivX.subimage(x0,y0,x1,y1);
		D derivY = (D)this.derivY.subimage(x0,y0,x1,y1);
		ImageUInt8 binary = this.binary.subimage(x0,y0,x1,y1);

		alg.transform(derivX,derivY,binary);
		FastQueue<LineParametric2D_F32> lines = alg.extractLines();

		for( int i = 0; i < lines.size; i++ ) {
		    // convert from the sub-image coordinate system to original image coordinate system
			LineParametric2D_F32 l = lines.get(i).copy();
			l.p.x += x0;
			l.p.y += y0;
			found.add(l);
		}
	}

	public HoughTransformLineFootOfNorm getTransform() {
		return alg;
	}

	public D getDerivX() {
		return derivX;
	}

	public D getDerivY() {
		return derivY;
	}

	public ImageFloat32 getEdgeIntensity() {
		return intensity;
	}

	public ImageUInt8 getBinary() {
		return binary;
	}
}