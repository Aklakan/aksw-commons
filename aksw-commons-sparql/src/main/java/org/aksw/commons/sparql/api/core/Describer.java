package org.aksw.commons.sparql.api.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.commons.collections.PrefetchIterator;
import org.aksw.commons.sparql.api.util.CannedQueryUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 *
 *
 * TODO What about Limit and Offset?
 *
 */
public class Describer
    extends PrefetchIterator<Triple> {

    private Iterator<Node> openNodes;
    private Collection<Var> resultVars;

    //private QueryExe
    // TODO Keep track of the involved resources so we can close them properly

    private ResultSetClosable rs;
    private Binding currentBinding = null;
    private Iterator<Var> currentVar = null;
    private QueryExecutionFactory qef;

    private QueryExecutionStreaming currentQe = null;

    public Describer(Iterator<Node> openNodes, ResultSetClosable rs, Collection<Var> resultVars, QueryExecutionFactory qef)
    {
        this.openNodes = openNodes;
        this.resultVars = resultVars;
        this.rs = rs;
        this.qef = qef;
    }

    public static Describer create(List<Node> resultUris, List<String> resultVars, ResultSetClosable rs, QueryExecutionFactory qef) {

        Set<Var> vars = null;
        if(rs != null) {
            if(!rs.hasNext()) {
                rs = null;
                resultVars = null;
            } else {
                vars = new HashSet<Var>();

                for(String var : resultVars) {
                    vars.add(Var.alloc(var));
                }
            }
        }

        Iterator<Node> it = (resultUris == null)
                ? null
                : resultUris.iterator();

        Describer result = new Describer(it, rs, vars, qef);
        return result;
    }


    public Iterator<Triple> describeNodeStreaming(Node node) {
        Query query = CannedQueryUtils.constructBySubject(node);

        QueryExecutionStreaming qe = qef.createQueryExecution(query);

        return qe.execConstructStreaming();
    }

    @Override
    protected Iterator<Triple> prefetch() throws Exception {
        Set<Node> batch = new HashSet<Node>();

        int n = 10;
        while(openNodes != null && openNodes.hasNext() && batch.size() < n) {
            Node node = openNodes.next();
            batch.add(node);
        }


        while(batch.size() < n) {

            if(rs == null) {
                break;
            }


            if(currentVar == null || !currentVar.hasNext()) {

                if(!rs.hasNext()) {
                    break;
                }

                currentBinding = rs.nextBinding();

                currentVar = resultVars.iterator();
            }

            Var var = currentVar.next();
            Node node = currentBinding.get(var);

            if(node == null || !node.isURI()) {
                continue;
            }

            batch.add(node);
        }

        if(batch.isEmpty()) {
        	currentQe = null;
            return null;
        }

        Query q = CannedQueryUtils.constructBySubjects(batch);

        // NOTE: No need to close: If the previous qe finishes or encounters an exception it closes itself
        // Close previous query execution
//        if(currentQe != null) {
//    		currentQe.close();
//    	}
        currentQe = qef.createQueryExecution(q);

        Iterator<Triple> result = currentQe.execConstructStreaming();
        return result;
    }
    
    @Override
    public void close() {
    	if(rs != null) {
    		rs.close();
    	}
    	if(currentQe != null) {
    		currentQe.close();
    	}
    }
}