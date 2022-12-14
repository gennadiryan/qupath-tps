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







public class TPSTransform extends ThinplateSplineTransform {
    public TPSTransform(double[][] src, double[][] dst) {
        super(new ThinPlateR2LogRSplineKernelTransform(2, dst, src));
    }

    public TPSTransform(double[][] src, double[][] dst, double stiff) {
        super(new ThinPlateR2LogRSplineKernelTransform(2, dst, src) {
            protected double stiffness = stiff;
        });
    }

    public int[][][] getTransform(int[][] bounds) {
        return this.getTransformDownsampled(bounds, (double) 1);
        // if (bounds[0].length != bounds[1].length || bounds[0].length * bounds[1].length == 0)
        // 		return null;
        // if (step == null) {
        // 		step = new int[2];
        // 		step[0] = 1;
        // 		step[1] = 1;
        // }
        //
        // int[][][] transform = new int[bounds[0][1] - bounds[0][0]][bounds[1][1] - bounds[1][0]][2];
        //
        // double[] src = new double[2];
        // double[] dst = new double[2];
        //
        // int[] min = new double[2];
        // int[] max = new double[2];
        // int[][] boundsT = new int[2][2];
        // boolean fst = true;
        //
        // for (int i = bounds[0][0]; i < bounds[0][0] + bounds[0][1]; i += step[0]) {
        //     for (int j = bounds[1][0]; j < bounds[1][0] + bounds[1][1]; j += step[1]) {
        //         dst[0] = (double) i;
        //         dst[1] = (double) j;
        //         this.apply(dst, src);
        //         transform[i][j] = (int[]) src;
        //
        //         for (int k = 0; k < 2; ++k) {
        //             boundsT[k][0] = transform[i][j][k] < boundsT[k][0] || fst ? transform[i][j][k] : boundsT[k][0];
        //             boundsT[k][1] = transform[i][j][k] > boundsT[k][1] || fst ? transform[i][j][k] : boundsT[k][1];
        //         }
        //
        //         fst = false;
        //     }
        // }
        //
        // for (int k = 0; k < 2; ++k) {
        //     boundsT[k][1] -= boundsT[k][0] - 1;
        //     bounds[k] = boundsT[k];
        // }
        //
        // return transform;
    }

    public int[][][] getTransformDownsampled(int[][] bounds, double downsample) {
        if (bounds.length != 2 || bounds[0].length != bounds[1].length || bounds[0].length * bounds[1].length == 0)
            return null;

        int w = (int) Math.ceil(bounds[0][1] / downsample);
        int h = (int) Math.ceil(bounds[1][1] / downsample);

        double[] src = new double[2];
        double[] dst = new double[2];

        int[][][] transform = new int[w][h][2];
        int[][] newBounds = new int[2][2];

        boolean fst = true;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                dst[0] = bounds[0][0] + (i * downsample);
                dst[1] = bounds[1][0] + (j * downsample);
                this.apply(dst, src);

                for (int k = 0; k < 2; ++k) {
                    transform[i][j][k] = (int) Math.floor(src[k]);
                    newBounds[k][0] = transform[i][j][k] < newBounds[k][0] || fst ? transform[i][j][k] : newBounds[k][0];
                    newBounds[k][1] = transform[i][j][k] > newBounds[k][1] || fst ? transform[i][j][k] : newBounds[k][1];
                }

                fst = false;
            }
        }

        for (int k = 0; k < 2; ++k) {
            newBounds[k][1] -= newBounds[k][0] - 1;
            bounds[k] = newBounds[k];
        }

        return transform;
    }
}
