package qupath.ext.imagecombinerwarpy.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerBuilder.AbstractServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.TransformingImageServer;
import qupath.lib.regions.RegionRequest;


abstract class AbstractTransformServerBuilder<T> implements ServerBuilder<BufferedImage> {
    private ImageServerMetadata metadata;
    protected ServerBuilder<BufferedImage> builder;

    public AbstractTransformServerBuilder(ImageServerMetadata metadata, ServerBuilder<BufferedImage> builder) {
        this.metadata = metadata;
        this.builder = builder;
    }

    protected ImageServerMetadata getMetadata() {
        return this.metadata;
    }

    @Override public Collection<URI> getURIs() {
        return this.builder.getURIs();
    }

    @Override public ServerBuilder<BufferedImage> updateURIs(Map<URI, URI> updateMap) {
        this.builder = this.builder.updateURIs(updateMap);
        return this;
    }

    @Override public ImageServer<BufferedImage> build() throws Exception {
        var server = this.buildOriginal();
        if (server == null)
            return null;
        if (this.metadata != null)
            server.setMetadata(metadata);
        return server;
    }

    protected abstract AbstractTransformServer<T> buildOriginal() throws Exception;
}
