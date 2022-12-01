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


// TODO: fix this
class TPSTransformServerBuilder extends AbstractTransformServerBuilder<TPSTransform> {
    public TPSTransformServerBuilder(ImageServerMetadata metadata, ServerBuilder<BufferedImage> builder) {
        super(metadata, builder);
    }
}
