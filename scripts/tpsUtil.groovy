import groovy.transform.InheritConstructors

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import qupath.ext.imagecombinerwarpy.gui.ImageCombinerWarpyServerOverlay;
import qupath.ext.imagecombinerwarpy.gui.TPSTransform;
import qupath.ext.imagecombinerwarpy.gui.TPSTransformServer;

import qupath.lib.awt.common.AwtTools;
import qupath.lib.awt.common.BufferedImageTools;
import qupath.lib.color.ColorToolsAwt;
import qupath.lib.common.ColorTools;
import qupath.lib.common.ThreadTools;
import qupath.lib.display.ChannelDisplayInfo;
import qupath.lib.display.ImageDisplay;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.images.stores.AbstractImageRenderer;
import qupath.lib.gui.images.stores.DefaultImageRegionStore;
import qupath.lib.gui.images.stores.ImageRegionStoreFactory;
import qupath.lib.gui.images.stores.ImageRenderer;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.scripting.QPEx
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.overlays.PathOverlay;
import qupath.lib.images.servers.AbstractImageServer
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.hierarchy.PathObjectHierarchy
import qupath.lib.projects.Project
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.LineROI
import qupath.lib.roi.PointsROI


def pointsToArray(List points) {
    assert points.findAll({it.size() == 2}).size() == points.size()
    def pointsArray = new double[2][points.size()]
    points.withIndex().each({point, idx ->
        pointsArray[0][idx] = point[0]
        pointsArray[1][idx] = point[1]
    })
    return pointsArray
}

def pointPairsToArrays(List pointPairs) {
    assert pointPairs.findAll({it.size() == 2}).size() == pointPairs.size()
    def srcPoints = pointPairs.collect({it.get(0)})
    def dstPoints = pointPairs.collect({it.get(1)})

    return [srcPoints, dstPoints].collect({pointsToArray(it)})
}

def pointsFromHierarchy(PathObjectHierarchy hier) {
    return hier.getAnnotationObjects()
        .findAll({it.getROI() instanceof PointsROI})
        .findAll({it.getROI().getNumPoints() == 1})
        .groupBy({it.getName()})
        .findAll({it.value.size() == 1})
        .collectEntries({[it.key, it.value.get(0).getROI().getAllPoints().get(0)]})
        .collectEntries({[it.key, [it.value.getX(), it.value.getY()]]})
}

def pointArraysFromHierarchies(PathObjectHierarchy hier1, PathObjectHierarchy hier2) {
    def (points1, points2) = [hier1, hier2].collect({pointsFromHierarchy(it)})
    def keys = points1.keySet().findAll({it in points2.keySet()})
    return pointPairsToArrays(keys.collect({[points1.get(it), points2.get(it)]}).toList())
}

def transformFromHierarchies(PathObjectHierarchy hier1, PathObjectHierarchy hier2, double stiffness=0.0) {
    return new TPSTransform(*pointArraysFromHierarchies(hier1, hier2), stiffness)
}

def transformServerFromHierarchies(ImageServer server, PathObjectHierarchy hier1, PathObjectHierarchy hier2, double stiffness=0.0) {
    return new TPSTransformServer(server, transformFromHierarchies(hier1, hier2, stiffness))
}

def transformOverlayFromHierarchies(QuPathViewer viewer, ImageServer server, PathObjectHierarchy hier1, PathObjectHierarchy hier2, double stiffness=0.0) {
    return new ImageCombinerWarpyServerOverlay(viewer, transformServerFromHierarchies(server, hier1, hier2, stiffness))
}


def imageDataFromProject(Project project, ImageData imageData) {
    def matches = project.getImageList().findAll({it.getFullProjectEntryID() == imageData.getProperty("PROJECT_ENTRY_ID")})
    if (matches.size() != 1)
        return null
    def newImageData = matches.get(0).readImageData()
    newImageData.getHierarchy().clearAll()
    return newImageData
}

def getDialogCallable(Stage owner, String title) {
    Dialog<ButtonType> dialog = new Dialog<>()
    dialog.initOwner(owner)
    dialog.initModality(Modality.NONE)

    dialog.setTitle(title)
    dialog.setDialogPane(new DialogPane())
    dialog.getDialogPane().setContent(new GridPane())
    dialog.getDialogPane().getButtonTypes().setAll([ButtonType.APPLY, ButtonType.CLOSE])

    return new Callable<Boolean>() {
        @Override Boolean call() {
            Optional<ButtonType> resp = dialog.showAndWait()
            return resp.isPresent() && resp.get() == ButtonType.APPLY
        }
    }
}


def gui = QPEx.getQuPath().getInstance()
def viewers = gui.getViewers()
def (viewer1, viewer2, overlayViewer) = [0, 1, 2].collect({viewers.get(it)})
assert viewer1.getImageData() != null && viewer2.getImageData() != null && overlayViewer.getImageData() == null

Platform.runLater({
    overlayViewer.setImageData(imageDataFromProject(gui.getProject(), viewer1.getImageData()))
    overlayViewer.getCustomOverlayLayers().setAll()

    def dialogCallable = getDialogCallable(overlayViewer.getView().getScene().getWindow(), "TPS Transform")
    while (dialogCallable.call()) {
        overlayViewer.getCustomOverlayLayers().setAll(transformOverlayFromHierarchies(overlayViewer, viewer2.getServer(), viewer2.getHierarchy(), viewer1.getHierarchy()))
    }

    overlayViewer.getCustomOverlayLayers().setAll()
})
