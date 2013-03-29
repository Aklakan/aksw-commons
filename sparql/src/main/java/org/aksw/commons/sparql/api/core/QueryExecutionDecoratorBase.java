package org.aksw.commons.sparql.api.core;

import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;


/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/26/11
 *         Time: 10:28 AM
 */
public class QueryExecutionDecoratorBase<T extends QueryExecution>
    implements QueryExecution
{
    protected T decoratee;

    public QueryExecutionDecoratorBase(T decoratee) {
    	//this.decoratee = new QueryExecutionStreamingWrapper(decoratee);
    	this.setDecoratee(decoratee);
    }

    /*
    public QueryExecutionDecoratorBase(QueryExecutionStreaming decoratee)
    {
    	this.setDecoratee(decoratee);
        //this.decoratee = decoratee;
    }*/
    
    protected QueryExecution getDecoratee()
    {
        return decoratee;
    }

    protected void setDecoratee(T decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public void setFileManager(FileManager fm) {
        decoratee.setFileManager(fm);
    }

    @Override
    public void setInitialBinding(QuerySolution binding) {
        decoratee.setInitialBinding(binding);
    }

    @Override
    public Dataset getDataset() {
        return decoratee.getDataset();
    }

    @Override
    public Context getContext() {
        return decoratee.getContext();
    }

    /**
     * The query associated with a query execution.
     * May be null (QueryExecution may have been created by other means)
     */
    @Override
    public Query getQuery() {
        return decoratee.getQuery();
    }

    @Override
    public ResultSet execSelect() {
        return decoratee.execSelect();
    }

    @Override
    public Model execConstruct() {
        return decoratee.execConstruct();
    }

    @Override
    public Model execConstruct(Model model) {
        return decoratee.execConstruct(model);
    }

    @Override
    public Model execDescribe() {
        return decoratee.execDescribe();
    }

    @Override
    public Model execDescribe(Model model) {
        return decoratee.execDescribe(model);
    }

    @Override
    public boolean execAsk() {
        return decoratee.execAsk();
    }

    @Override
    public void abort() {
        decoratee.abort();
    }

    @Override
    public void close() {
    	if(decoratee != null) {
    		decoratee.close();
    	}
    }

    @Override
    public void setTimeout(long timeout, TimeUnit timeoutUnits) {
        decoratee.setTimeout(timeout, timeoutUnits);
    }

    @Override
    public void setTimeout(long timeout) {
        decoratee.setTimeout(timeout);
    }

    @Override
    public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
        decoratee.setTimeout(timeout1, timeUnit1, timeout2, timeUnit2);
    }

    @Override
    public void setTimeout(long timeout1, long timeout2) {
        decoratee.setTimeout(timeout1, timeout2);
    }
}
