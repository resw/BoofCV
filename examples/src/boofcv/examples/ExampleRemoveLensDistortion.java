/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.examples;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;

/**
 * All real camera lens have distortion.  This distortion causes large errors when attempting to recover the
 * scene's structure and camera's motion.  The following example demonstrates how the lens distortion can be
 * removed from an image after the camera has been calibrated.
 *
 * After lens distortion has been removed the new image will not be properly contained inside the original
 * image side.  Several methods are provided for scaling and translating the image to maximize the view area
 * using different metrics.  After this adjustment has been done the new image is equivalent to one being
 * generated by a virtual camera with a different set of intrinsic parameters.
 *
 * @author Peter Abeles
 */
public class ExampleRemoveLensDistortion {

	public static void main( String args[] ) {
		String calibDir = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";
		String imageDir = "../data/evaluation/structure/";

		// load calibration parameters from the previously calibrated camera
		IntrinsicParameters param = BoofMiscOps.loadXML(calibDir + "intrinsic.xml");

		// load images and convert the image into a color BoofCV format
		BufferedImage orig = UtilImageIO.loadImage(imageDir + "dist_cyto_01.jpg");
		MultiSpectral<ImageFloat32> distortedImg = ConvertBufferedImage.convertFromMulti(orig, null, ImageFloat32.class);

		// compute the transform to remove lens distortion
		// The inverse transformation (adds distortion) is used when apply adjusting an image.
		// In other application the forward transformation (removes distortion) is required.
		PointTransform_F32 tran = LensDistortionOps.transformPixelToRadial_F32(param);

		// create new transforms to optimize view area
		// After distortion the adjusted intrinsic camera parameters should be used.
		// Since they are not being used in this example null is passed in.
		PointTransform_F32 fullView = LensDistortionOps.fullView(param, null);
		PointTransform_F32 allInside = LensDistortionOps.allInside(param, null);

		// Set up image distort
		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		ImageDistort<ImageFloat32> distort = FactoryDistort.distort(interp,null,ImageFloat32.class);

		// render and display the different types of views in a window
		displayResults(orig, distortedImg, tran, fullView, allInside, distort);
	}

	/**
	 * Displays results in a window for easy comparison..
	 */
	private static void displayResults(BufferedImage orig,
									   MultiSpectral<ImageFloat32> distortedImg,
									   PointTransform_F32 tran,
									   PointTransform_F32 fullView,
									   PointTransform_F32 allInside,
									   ImageDistort<ImageFloat32> distort) {
		// render the results
		MultiSpectral<ImageFloat32> undistortedImg = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distortedImg.getWidth(),distortedImg.getHeight(),distortedImg.getNumBands());

		distort.setModel(new PointToPixelTransform_F32(tran));
		GImageMiscOps.fill(undistortedImg, 0);
		DistortImageOps.distortMS(distortedImg, undistortedImg, distort);
		BufferedImage out1 = ConvertBufferedImage.convertTo(undistortedImg, null);

		distort.setModel(new PointToPixelTransform_F32(fullView));
		GImageMiscOps.fill(undistortedImg,0);
		DistortImageOps.distortMS(distortedImg,undistortedImg,distort);
		BufferedImage out2 = ConvertBufferedImage.convertTo(undistortedImg,null);

		distort.setModel(new PointToPixelTransform_F32(allInside));
		GImageMiscOps.fill(undistortedImg,0);
		DistortImageOps.distortMS(distortedImg,undistortedImg,distort);
		BufferedImage out3 = ConvertBufferedImage.convertTo(undistortedImg,null);

		// display in a single window where the user can easily switch between images
		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addItem(new ImagePanel(orig), "Original");
		panel.addItem(new ImagePanel(out1), "Undistorted");
		panel.addItem(new ImagePanel(out3), "Undistorted All Inside");
		panel.addItem(new ImagePanel(out2), "Undistorted Full View");

		ShowImages.showWindow(panel, "Removing Lens Distortion");
	}
}
