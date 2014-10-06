package org.aksw.commons.sparql.api.cache.extra;

import org.aksw.commons.collections.IClosable;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 11/28/11
 *         Time: 11:54 PM
 */
public class ClosableCacheSql
    implements IClosable
{
    private CacheResource resource;
    private InputStream in;

    public ClosableCacheSql(CacheResource resource, InputStream in) {
        this.resource = resource;
        this.in = in;
    }


    @Override
    public void close() {
        //SqlUtils.close(rs);
        resource.close();
        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
