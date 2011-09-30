package org.aksw.commons.sparql.api.cache.extra;


import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import org.aksw.commons.collections.IClosable;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/26/11
 *         Time: 2:42 PM
 */
public interface CacheResource
    extends IClosable
{
    boolean isOutdated();

    //InputStream open();

    Model asModel(Model result);
    ResultSet asResultSet();
    boolean asBoolean();
}
