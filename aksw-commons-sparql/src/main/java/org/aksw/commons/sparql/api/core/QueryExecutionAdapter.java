package org.aksw.commons.sparql.api.core;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;

/**
 * 
 *
 * @author Claus Stadler
 *         <p/>
 *         Date: 11/29/11
 *         Time: 12:01 AM
 */
public class QueryExecutionAdapter
    implements QueryExecutionStreaming
{
    protected QueryExecutionTimeoutHelper timeoutHelper = new QueryExecutionTimeoutHelper(this);

    @Override
    public void setFileManager(FileManager fm) {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public void setInitialBinding(QuerySolution binding) {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Dataset getDataset() {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Context getContext() {
        throw new RuntimeException("Not Implemented.");
    }

    /**
     * The query associated with a query execution.
     * May be null (QueryExecution may have been created by other means)
     */
    @Override
    public Query getQuery() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultSet execSelect() {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Model execConstruct() {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Model execConstruct(Model model) {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Model execDescribe() {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public Model execDescribe(Model model) {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public boolean execAsk() {
        throw new RuntimeException("Not Implemented.");
    }

    @Override
    public void abort() {
    }

    @Override
    public void close() {
    }

    @Override
    public void setTimeout(long timeout, TimeUnit timeoutUnits) {
        timeoutHelper.setTimeout(timeout, timeoutUnits);
    }

    @Override
    public void setTimeout(long timeout) {
        timeoutHelper.setTimeout(timeout);
    }

    @Override
    public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
        timeoutHelper.setTimeout(timeout1, timeUnit1, timeout2, timeUnit2);
    }

    @Override
    public void setTimeout(long timeout1, long timeout2) {
        timeoutHelper.setTimeout(timeout1, timeout2);
    }

	@Override
	public Iterator<Triple> execConstructStreaming() {
        throw new RuntimeException("Not Implemented.");
	}

	@Override
	public Iterator<Triple> execDescribeStreaming() {
        throw new RuntimeException("Not Implemented.");
	}
}
