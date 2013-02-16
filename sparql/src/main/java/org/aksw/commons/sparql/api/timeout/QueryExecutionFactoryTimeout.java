package org.aksw.commons.sparql.api.timeout;


import java.util.concurrent.TimeUnit;

import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.core.QueryExecutionFactoryDecorator;
import org.aksw.commons.sparql.api.core.QueryExecutionStreaming;
import org.aksw.commons.sparql.api.core.Time;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;

/**
 * A query execution factory, which sets a given timeout
 * on all created query executions
 *
 *
 * @author Claus Stadler
 *
 *
 *         Date: 7/26/11
 *         Time: 10:27 AM
 */
public class QueryExecutionFactoryTimeout
        extends QueryExecutionFactoryDecorator {

    private Time maxExecutionTime = null;
    private Time maxRetrievalTime = null;

    public static QueryExecutionFactoryTimeout decorate(QueryExecutionFactory decoratee, long timeout) {
        return new QueryExecutionFactoryTimeout(decoratee, timeout);
    }


    public QueryExecutionFactoryTimeout(QueryExecutionFactory decoratee) {
        super(decoratee);
    }

    public QueryExecutionFactoryTimeout(QueryExecutionFactory decoratee, long timeout) {
        this(decoratee, timeout, TimeUnit.MILLISECONDS);
    }

    public QueryExecutionFactoryTimeout(QueryExecutionFactory decoratee, long timeout, TimeUnit timeUnit) {
        super(decoratee);
        this.maxExecutionTime = new Time(timeout, timeUnit);
    }

    public QueryExecutionFactoryTimeout(QueryExecutionFactory decoratee, long timeout1, long timeout2) {
        this(decoratee, timeout1, TimeUnit.MILLISECONDS, timeout2, TimeUnit.MILLISECONDS);
    }

    public QueryExecutionFactoryTimeout(QueryExecutionFactory decoratee, long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
        super(decoratee);
        this.maxExecutionTime = new Time(timeout1, timeUnit1);
        this.maxRetrievalTime = new Time(timeout2, timeUnit2);
    }


    private void configureWithTimeout(QueryExecution qe) {

        // Note maxRetrieval time implies a setting of max execution time
        if(maxExecutionTime != null) {
            if(maxRetrievalTime != null) {
                qe.setTimeout(maxExecutionTime.getTime(), maxExecutionTime.getTimeUnit(), maxRetrievalTime.getTime(), maxRetrievalTime.getTimeUnit());
            } else {
                qe.setTimeout(maxExecutionTime.getTime(), maxExecutionTime.getTimeUnit());
            }
        }
    }

    @Override
    public QueryExecutionStreaming createQueryExecution(Query query) {
        QueryExecutionStreaming result = super.createQueryExecution(query);

        configureWithTimeout(result);

        return result;
    }

    @Override
    public QueryExecutionStreaming createQueryExecution(String queryString) {
        QueryExecutionStreaming result = super.createQueryExecution(queryString);

        configureWithTimeout(result);

        return result;
    }
}
