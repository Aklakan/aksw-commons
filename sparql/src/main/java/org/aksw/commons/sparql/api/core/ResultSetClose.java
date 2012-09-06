package org.aksw.commons.sparql.api.core;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/23/11
 *         Time: 10:54 PM
 *
 * Unlike JCDB, Jena does not provide the close method on the ResultSet, but on the object that
 * created the result set. Therefore:
 *
 *
 */
public class ResultSetClose
        extends ResultSetDecorator
{
    private static final Logger logger = LoggerFactory.getLogger(ResultSetClose.class);

    private boolean isClosed = false;

    public ResultSetClose(ResultSet decoratee) {
        super(decoratee);
        checkClose();
    }

    public ResultSetClose(ResultSet decoratee, boolean isClosed) {
        super(decoratee);
        this.isClosed = isClosed;
        checkClose();
    }

    /*
    public ResultSetClose(ResultSet decoratee, IClosable closable) {
        super(decoratee);
        //this.closable = closable;
        checkClose();
    }
    */


    protected boolean checkClose() {
        if(!isClosed) {
            boolean hasNext;

            try {
                hasNext = decoratee.hasNext();
            } catch(Exception e) {
                hasNext = false;
            }

            if(!hasNext) {
                try {
                    isClosed = true ;
                    close();
                }
                catch(Exception e) {
                    logger.error("Error closing an object supposedly underlying a Jena ResultSet", e);
                }
            }
        }
        return isClosed;
    }

    @Override
    public boolean hasNext() {
        return !checkClose();
    }

    @Override
    public void remove() {
        decoratee.remove();
        checkClose();
    }


    @Override
    public QuerySolution nextSolution() {
        try {
            QuerySolution result = decoratee.nextSolution();
            checkClose();
            return result;
        } catch(Exception e) {
            close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Binding nextBinding() {
        try {
            Binding result = decoratee.nextBinding();
            checkClose();
            return result;
        } catch(Exception e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void close() {
    }
}