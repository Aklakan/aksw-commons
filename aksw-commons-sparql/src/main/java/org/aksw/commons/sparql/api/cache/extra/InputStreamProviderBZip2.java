package org.aksw.commons.sparql.api.cache.extra;

import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.InputStream;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 11/28/11
 *         Time: 11:53 PM
 */
public class InputStreamProviderBZip2
    implements InputStreamProvider
{
    private InputStreamProvider decoratee;
    private CompressorStreamFactory streamFactory;
    private String compression;

    public InputStreamProviderBZip2(InputStreamProvider decoratee, CompressorStreamFactory streamFactory, String compression)
    {
        this.decoratee = decoratee;
        this.streamFactory = streamFactory;
        this.compression = compression;
    }

    @Override
    public InputStream open() {
        try {
        return streamFactory.createCompressorInputStream(compression, decoratee.open());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        decoratee.close();
    }
}
