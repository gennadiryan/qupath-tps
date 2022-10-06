//package qupath.ext.imagecombinerwarpy.gui;

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
import qupath.ext.imagecombinerwarpy.gui.TPSTransformServer;



def pointsToArray(List points) {
    assert points.findAll({it.size() == 2}).size() == points.size()
    def pointsArray = new double[2][points.size()]
    points.withIndex().each({point, idx ->
        pointsArray[0][idx] = point[0]
        pointsArray[1][idx] = point[1]
    })
    return pointsArray
}

def pointPairsToArray(List pointPairs) {
    assert pointPairs.findAll({it.size() == 2}).size() == pointPairs.size()
    def srcPoints = pointPairs.collect({it.get(0)})
    def dstPoints = pointPairs.collect({it.get(1)})
    return [srcPoints, dstPoints].collect({pointsToArray(it)})
}



def gui = QPEx.getQuPath().getInstance()
def viewer = gui.getViewer()
def server = gui.getImageData().getServer()
//def newServer = server.getBuilder().build()
viewer.getCustomOverlayLayers().setAll([])

def points = [
    [[9462, 4711], [9328, 4680]],
    [[8000, 3370], [7860, 3340]],
    [[9265, 4101], [9129, 4067]],
    [[5350, 4755], [5211, 4734]],
    // [[1937, 5465], [2072, 5480]],
    [[2072, 5480], [1937, 5465]],
    [[645, 4275], [510, 4265]],
    [[2380, 15220], [2275, 15205]],
    [[7400, 4880], [7265, 4855]],
    [[3265, 2300], [3122, 2285]],
    [[3092, 2428], [2950, 2414]],
    [[4193, 15277], [4091, 15256]],
    [[3742, 11880], [3627, 11857]],
    [[5483, 7089], [5354, 7067]],
    [[5015, 5515], [4885, 5495]],
    [[2330, 4560], [2195, 4548]],
    [[1697, 4867], [1561, 4855]],
    [[3671, 7678], [3547, 7658]],
    [[647, 11225], [531, 11215]],
    // [[2277, 11847], [2764, 11827]],
    [[5811, 10442], [5697, 10416]],
    [[5635, 10540], [5520, 10515]],
    [[940, 13043], [830, 13031]],
    [[479, 11267], [363, 11257]],
    [[2572, 9917], [2451, 9903]],
    [[10481, 10953], [10365, 10919]],
    [[10006, 9737], [9886, 9705]],
    [[2630, 7307], [2503, 7290]],
    [[2333, 9551], [2213, 9541]],
    [[2210, 10255], [2093, 10240]],
    [[879, 9310], [757, 9299]],
    [[430, 10781], [312, 10771]],
    [[1629, 9592], [1510, 9580]],
    [[2058, 5854], [1926, 5840]],
    [[2878, 6021], [2745, 6005]],
    [[3054, 6151], [2924, 6134]],
    [[2891, 5363], [2759, 5349]],
    [[3240, 4818], [3105, 4803]],
    [[1572, 6416], [1444, 6404]],
    [[1332, 12550], [1221, 12538]],
    [[1058, 12758], [948, 12748]],
    [[1382, 12840], [1273, 12828]],
    [[3797, 14490], [3692, 14469]],
    [[2882, 14920], [2778, 14902]],
    [[2310, 11700], [2196, 11686]],
    [[7999, 13073], [7889, 13042]],
    [[9960, 11002], [9844, 10968]],
    [[8312, 8513], [8188, 8483]],
    [[5435, 3071], [5296, 3049]],
    [[5798, 2678], [5655, 2656]],
    // [[, ], [, ]],
]

def transform = new TPSTransform(*pointPairsToArray(points), 0.0)

def project = gui.getProject()

def entry = project.getImageList()[3]
def img = entry.readImageData()
def newServer = img.getServer()
def newOverlay = new ImageCombinerWarpyServerOverlay(viewer, newServer)
def transformServer = new TPSTransformServer(newServer, transform)
def transformOverlay = new ImageCombinerWarpyServerOverlay(viewer, transformServer)
//viewer.getCustomOverlayLayers().add(newOverlay)
viewer.getCustomOverlayLayers().add(transformOverlay)


//def transform = new TPSTransform((double[][]) src, (double[][]) dst)
//def transformServer = new TPSTransformServer(server.getBuilder().build(), transform)
//def transformOverlay = new ImageCombinerWarpyServerOverlay(viewer, transformServer)
//viewer.getCustomOverlayLayers().add(transformOverlay)


// def req = RegionRequest.createInstance(
//     transformServer.getPath(),
//     8.0,
//     1000, 1000, 2000, 2000,
//     0, 0
// )
// def img = transformServer.readBufferedImage(req)
//import javax.imageio.ImageIO
//ImageIO.write(img, 'PNG', new File('/Users/gennadiryan/Desktop/img.png'))
//x = new double[2]
//transform.apply((double[]) [12500, 10000], x)
//[x[0], x[1]]
