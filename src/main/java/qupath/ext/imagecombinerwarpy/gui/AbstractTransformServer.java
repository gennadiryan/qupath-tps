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


public abstract class AbstractTransformServer<T> extends AbstractImageServer<BufferedImage> {
    abstract class AbstractTransformServerBuilder<T> implements ServerBuilder<BufferedImage> {
        private ImageServerMetadata metadata;
        protected ServerBuilder<BufferedImage> builder;
    		protected T transform;

    		public AbstractTransformServerBuilder(ImageServerMetadata metadata, ServerBuilder<BufferedImage> builder, T transform) {
    				this.metadata = metadata;
    				this.builder = builder;
    				this.transform = transform;
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

    private ImageServer<BufferedImage> server;
		private T transform;

		public AbstractTransformServer(ImageServer<BufferedImage> server, T transform) {
				super(server.getImageClass());
        this.server = server;
				this.transform = transform;
		}

    @Override protected String createID() {
        return UUID.randomUUID().toString();
    }

    @Override protected abstract AbstractTransformServerBuilder<T> createServerBuilder();

    @Override public ImageServerMetadata getOriginalMetadata() {
        return this.getWrappedServer().getMetadata();
    }

    @Override public String getPath() {
        return this.getWrappedServer().getPath() + " (" + this.transform.toString() + ")";
    }

		@Override public String getServerType() {
        return this.getWrappedServer().getServerType() + " (" + this.transform.toString() + ")";
    }

    public T getTransform() {
				return this.transform;
		}

    @Override public Collection<URI> getURIs() {
        return this.getWrappedServer().getURIs();
    }

    protected ImageServer<BufferedImage> getWrappedServer() {
        return this.server;
    }

		@Override public abstract BufferedImage readBufferedImage(RegionRequest request) throws IOException;
}
