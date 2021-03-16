package org.aksw.commons.io.endpoint;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * An active process that creates a file
 *
 * @author raven
 *
 */
public interface FileCreation {
    /**
     * A completable future that fires when the file creation is complete
     * or an exception occurred
     *
     * @return
     */
    CompletableFuture<Path> future();
    //Single<Path> whenReady();

    /**
     * Optional method to cancel the creation
     * @throws Exception
     *
     */
    void abort() throws Exception;
}
