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



public class TPSTransformServer extends AbstractTransformServer<TPSTransform> {
    private ImageRegion region;
    private TPSTransform transform;

    public TPSTransformServer(final ImageServer<BufferedImage> server, TPSTransform transform) {
        this(server, transform, ImageRegion.createInstance((int) 0, (int) 0, (int) Math.ceil(server.getWidth()), (int) Math.ceil(server.getHeight()), 0, 0));
    }

    public TPSTransformServer(final ImageServer<BufferedImage> server, TPSTransform transform, ImageRegion region) {
        super(server, transform);
        this.region = region;

      	var levelBuilder = new ImageServerMetadata.ImageResolutionLevel.Builder(region.getWidth(), region.getHeight());
      	int i = 0;
      	do {
            // levelBuilder.addLevel(server.getMetadata().getLevel(i));
            levelBuilder.addLevelByDownsample(server.getMetadata().getLevel(i).getDownsample());
            ++i;
      	} while (i < server.nResolutions() && region.getWidth() >= server.getMetadata().getPreferredTileWidth() && region.getHeight() >= server.getMetadata().getPreferredTileHeight());

      	var metadata = new ImageServerMetadata.Builder(server.getMetadata())
  			.width(this.region.getWidth())
  			.height(this.region.getHeight())
  			.name(String.format("%s (%s)", server.getMetadata().getName(), this.getTransform().toString()))
  			.levels(levelBuilder.build())
            .pixelSizeMicrons(1, 1).zSpacingMicrons(1) // optional
            .build();

        this.setMetadata(metadata);
    }

    public ImageRegion getRegion() {
        return this.region;
    }


    @Override public BufferedImage readBufferedImage(final RegionRequest request) throws IOException {
        double downsample = request.getDownsample();
        int[][] bounds = new int[2][2];
        bounds[0][0] = request.getX();
        bounds[0][1] = request.getWidth();
        bounds[1][0] = request.getY();
        bounds[1][1] = request.getHeight();

        int[][][] transform = this.getTransform().getTransformDownsampled(bounds, downsample);
        for (int k = 0; k < 2; ++k) {
            bounds[k][0] -= (int) downsample;
            bounds[k][1] += (int) (2 * downsample);
        }

        var transformServer = this.getWrappedServer();
        var transformRequest = RegionRequest.createInstance(
            transformServer.getPath(),
            downsample,
            bounds[0][0], bounds[1][0], bounds[0][1], bounds[1][1],
            request.getZ(),
            request.getT()
        );
        var transformImg = transformServer.readBufferedImage(transformRequest);
        var transformRaster = transformImg.getRaster();
        var raster = transformRaster.createCompatibleWritableRaster(transform.length, transform[0].length);

        int x, y;
        Object pixel = null;
        for (int i = 0; i < transform.length; ++i) {
            for (int j = 0; j < transform[i].length; ++j) {
                x = (int) Math.floor((transform[i][j][0] - transformRequest.getX()) / downsample);
                y = (int) Math.floor((transform[i][j][1] - transformRequest.getY()) / downsample);
                if (0 <= x && x < transformRaster.getWidth() && 0 <= y && y < transformRaster.getHeight()) {
                    pixel = transformRaster.getDataElements(x, y, pixel);
                    raster.setDataElements(i, j, pixel);
                }
            }
        }

        return new BufferedImage(transformImg.getColorModel(), raster, transformImg.isAlphaPremultiplied(), null);
    }

    @Override protected TPSTransformServerBuilder createServerBuilder() {
        return new TPSTransformServerBuilder(this.getMetadata(), this.getWrappedServer().getBuilder());
    }
}

static class TPSTransformServerBuilder extends AbstractTransformServerBuilder<TPSTransform> {
    @Override protected TPSTransformServer buildOriginal() throws Exception {
        // return new TPSTransformServer(this.builder.build(), this.transform, this.region);
        return null;
    }
}
