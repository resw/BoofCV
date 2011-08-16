/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.describe;

import gecv.struct.image.ImageBase;


/**
 * Estimates the orientation of a region.  Often used to create rotation invariant features.
 *
 * @author Peter Abeles
 */
public interface RegionOrientation<D extends ImageBase> {

	public void setImage( D derivX , D derivY );

	/**
	 * Computes the orientation of a region about its center.
	 *
	 * @param c_x Center of the region in image pixels.
	 * @param c_y Center of the region in image pixels.
	 *
	 * @return Orientation in radians.
	 */
	public double compute( int c_x , int c_y );
}