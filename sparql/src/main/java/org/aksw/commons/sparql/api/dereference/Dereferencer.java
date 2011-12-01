package org.aksw.commons.sparql.api.dereference;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.aksw.commons.collections.IClosable;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 11/30/11
 *         Time: 4:23 PM
 */
public interface Dereferencer
    extends IClosable
{
    Model dereference(String url);
}
