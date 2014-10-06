package org.aksw.commons.sparql.api.cache.extra;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 11/28/11
 *         Time: 11:05 PM
 */

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/23/11
 *         Time: 10:22 PM
 */
public interface CacheEx
{
    void write(String service, String queryString, ResultSet resultSet);
    void write(String service, Query query, ResultSet resultSet);

    void write(String service, String queryString, Model model);
    void write(String service, Query query, Model model);

    void write(String service, String queryString, boolean value);
    void write(String service, Query query, boolean value);

    CacheResource lookup(String service, String queryString);
    CacheResource lookup(String service, Query query);
}