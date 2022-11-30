/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2020 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.ext.imagecombinerwarpy.gui;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;

import qupath.lib.awt.common.AwtTools;
import qupath.lib.awt.common.BufferedImageTools;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.io.GsonTools;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

import qupath.ext.imagecombinerwarpy.gui.*;
import qupath.ext.imagecombinerwarpy.gui.AbstractTransformServer;
import qupath.ext.imagecombinerwarpy.gui.TPSTransform;


public class RealTransformServer extends AbstractTransformServer<TPSTransform> {
    private ImageRegion region;
    private TPSTransform transform;

    public RealTransformServer(final ImageServer<BufferedImage> server, TPSTransform transform) {
      	super(server, transform);
      	// this.transformInverse = this.getTransform().inverse();

      	// var boundsTransformed = this.getTransform().createTransformedShape(new Rectangle2D.Double(0, 0, server.getWidth(), server.getHeight())).getBounds2D();
				var boundsTransformed = new Rectangle2D.Double(0, 0, server.getWidth(), server.getHeight()).getBounds2D();

    		// int minX = Math.max(0, (int)boundsTransformed.getMinX());
    		// int maxX = Math.min(server.getWidth(), (int)Math.ceil(boundsTransformed.getMaxX()));
    		// int minY = Math.max(0, (int)boundsTransformed.getMinY());
    		// int maxY = Math.min(server.getHeight(), (int)Math.ceil(boundsTransformed.getMaxY()));
    		// this.region = ImageRegion.createInstance(
    		// 		minX, minY, maxX-minX, maxY-minY, 0, 0);

      	this.region = ImageRegion.createInstance(
      			(int)boundsTransformed.getMinX(),
      			(int)boundsTransformed.getMinY(),
      			(int)Math.ceil(boundsTransformed.getWidth()),
      			(int)Math.ceil(boundsTransformed.getHeight()), 0, 0);

      	var levelBuilder = new ImageServerMetadata.ImageResolutionLevel.Builder(region.getWidth(), region.getHeight());
      	boolean fullServer = server.getWidth() == region.getWidth() && server.getHeight() == region.getHeight();
      	int i = 0;
      	do {
        		// var originalLevel = server.getMetadata().getLevel(i);
        		// if (fullServer)
        		//     levelBuilder.addLevel(originalLevel);
        		// else
        		//     levelBuilder.addLevelByDownsample(originalLevel.getDownsample());
        		// i++;
            levelBuilder.addLevel(server.getMetadata().getLevel(i));
            ++i;
      	} while (i < server.nResolutions() && region.getWidth() >= server.getMetadata().getPreferredTileWidth() && region.getHeight() >= server.getMetadata().getPreferredTileHeight());

      	// TODO: Apply AffineTransform to pixel sizes! Perhaps create a Shape or point and transform that?
      	var metadata = new ImageServerMetadata.Builder(server.getMetadata())
      			.width(this.region.getWidth())
      			.height(this.region.getHeight())
      			.name(String.format("%s (%s)", server.getMetadata().getName(), this.getTransform().toString()))
      			.levels(levelBuilder.build())
            .pixelSizeMicrons(1, 1).zSpacingMicrons(1) // optional
            .build();

        // this.getWrappedServer().setMetadata(metadata);
        this.setMetadata(metadata);

    }


    @Override public BufferedImage readBufferedImage(final RegionRequest request) throws IOException {
        // return this.getWrappedServer().readBufferedImage(request);


				int x = request.getX();
				int y = request.getY();
				int w = request.getWidth();
				int h = request.getHeight();
				double downsample = request.getDownsample();

				int[][] bounds = new int[2][2];
				bounds[0][0] = x;
				bounds[0][1] = w;
				bounds[1][0] = y;
				bounds[1][1] = h;

				var wrappedServer = this.getWrappedServer();
				int[][][] transform = this.getTransform().getTransformDownsampled(bounds, downsample);
				int W = transform.length;
				int H = transform[0].length;

				for (int k = 0; k < 2; ++k) {
						bounds[k][0] = Math.max(bounds[k][0], 0);
						bounds[k][1] = Math.min(bounds[k][1], k == 0 ? wrappedServer.getWidth() : wrappedServer.getHeight());
				}
				var transformRequest = RegionRequest.createInstance(
						wrappedServer.getPath(),
						downsample,
						bounds[0][0], bounds[1][0], bounds[0][1], bounds[1][1],
						request.getZ(),
						request.getT()
				);
				var imgOrig = wrappedServer.readBufferedImage(transformRequest);
				var rasterOrig = imgOrig.getRaster();
				var raster = rasterOrig.createCompatibleWritableRaster(w, h);

				int X, Y;
				Object pixel = null;
				for (int i = 0; i < W; ++i) {
						for (int j = 0; j < H; ++j) {
								X = (int) Math.floor((transform[i][j][0] - transformRequest.getX()) / downsample);
								Y = (int) Math.floor((transform[i][j][1] - transformRequest.getY()) / downsample);
								pixel = rasterOrig.getDataElements(X, Y, pixel);
								raster.setDataElements(i, j, pixel);
						}
				}

				return new BufferedImage(imgOrig.getColorModel(), raster, imgOrig.isAlphaPremultiplied(), null);

				// double downsample = request.getDownsample();
				//
        // var bounds = AwtTools.getBounds(request);
        // var boundsTransformed = this.transformInverse.createTransformedShape(bounds).getBounds();
				//
        // var wrappedServer = this.getWrappedServer();
				//
        // int minX = Math.max(0, (int)boundsTransformed.getMinX()-1);
        // int maxX = Math.min(wrappedServer.getWidth(), (int)Math.ceil(boundsTransformed.getMaxX()+1));
        // int minY = Math.max(0, (int)boundsTransformed.getMinY()-1);
        // int maxY = Math.min(wrappedServer.getHeight(), (int)Math.ceil(boundsTransformed.getMaxY()+1));
        // var requestTransformed = RegionRequest.createInstance(
        // 		wrappedServer.getPath(),
        // 		downsample,
        // 		minX, minY, maxX - minX, maxY - minY,
        // 		request.getZ(),
        // 		request.getT()
        // );
				//
				//
        // BufferedImage img = wrappedServer.readBufferedImage(requestTransformed);
        // if (img == null)
        // 	  return img;
				//
        // int w = (int)(request.getWidth() / downsample);
        // int h = (int)(request.getHeight() / downsample);
				//
        // AffineTransform transform2 = new AffineTransform();
        // transform2.scale(1.0 / downsample, 1.0 / downsample);
        // transform2.translate(-request.getX(), -request.getY());
        // transform2.concatenate(this.getTransform());
				//
        // if (BufferedImageTools.is8bitColorType(img.getType()) || img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
        //   	var img2 = new BufferedImage(w, h, img.getType());
        //   	var g2d = img2.createGraphics();
        //   	g2d.transform(transform2);
        //   	g2d.drawImage(img, requestTransformed.getX(), requestTransformed.getY(), requestTransformed.getWidth(), requestTransformed.getHeight(), null);
        //   	g2d.dispose();
        //   	return img2;
        // }
				//
        // var rasterOrig = img.getRaster();
        // var raster = rasterOrig.createCompatibleWritableRaster(w, h);
				//
        // double[] row = new double[w*2];
        // double[] row2 = new double[w*2];
        // double[] t = new double[2];
        // double[] t2 = new double[2];
        // try {
        //     transform2 = transform2.createInverse();
        // } catch (NoninvertibleTransformException e) {
        //     throw new IOException(e);
        // }
        // Object elements = null;
        // for (int y = 0; y < h; y++) {
        //   	// for (int x = 0; x < w; x++) {
        //     // 		row[x*2] = x;
        //     // 		row[x*2+1] = y;
        //   	// }
        //   	// transform2.transform(row, 0, row2, 0, w);
				//
        //   	for (int x = 0; x < w; x++) {
        //     		// int xx = (int)((row2[x*2]-requestTransformed.getX())/downsample);
        //     		// int yy = (int)((row2[x*2+1]-requestTransformed.getY())/downsample);
				//
        //         t[0] = x;
        //         t[1] = y;
        //         transform2.transform(t, 0, t2, 0, 1);
        //         int xx = (int)((t2[0] - requestTransformed.getX()) / downsample);
        //         int yy = (int)((t2[1] - requestTransformed.getY()) / downsample);
				//
        //         if (xx >= 0 && yy >= 0 && xx < img.getWidth() && yy < img.getHeight()) {
        //       			elements = rasterOrig.getDataElements(xx, yy, elements);
        //       			raster.setDataElements(x, y, elements);
        //     		}
        //   	}
        // }
        // return new BufferedImage(img.getColorModel(), raster, img.isAlphaPremultiplied(), null);
    }


    @Override protected AbstractTransformServerBuilder<TPSTransform> createServerBuilder() {
				return new AbstractTransformServerBuilder<TPSTransform>(this.getMetadata(), this.getWrappedServer().getBuilder()) {
						@Override protected AbstractTransformServer<TPSTransform> buildOriginal() throws Exception {
								return new RealTransformServer(this.builder.build(), RealTransformServer.this.getTransform());
						}
				};
		}

}
