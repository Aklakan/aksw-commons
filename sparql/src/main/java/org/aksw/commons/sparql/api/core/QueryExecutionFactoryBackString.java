package org.aksw.commons.sparql.api.core;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/23/11
 *         Time: 9:44 PM
 */
public abstract class QueryExecutionFactoryBackString
    implements QueryExecutionFactory
{
    @Override
    public QueryExecution createQueryExecution(Query query) {
        return createQueryExecution(query.toString());
    }
}
