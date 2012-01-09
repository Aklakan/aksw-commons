package org.aksw.commons.sparql.api.util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.*;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.Template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 12/1/11
 *         Time: 1:12 AM
 */
public class CannedQueryUtils {
    public static Query spoTemplate() {
        return spoTemplate(Node.createVariable("s"), Node.createVariable("p"), Node.createVariable("o"));
    }

    public static Query spoTemplate(Node s, Node p, Node o)
    {
        Query query = QueryFactory.create();
        query.setQuerySelectType();

        Triple triple = new Triple(s, p, o);
        ElementGroup group = new ElementGroup();
        group.addTriplePattern(triple);
        query.setQueryPattern(group);

        if(s.isVariable()) {
            query.getProject().add(Var.alloc(s.getName()));
        }
        if(p.isVariable()) {
            query.getProject().add(Var.alloc(p.getName()));
        }
        if(o.isVariable()) {
            query.getProject().add(Var.alloc(o.getName()));
        }

        return query;
    }



    public static Query constructBySubjects(Collection<Node> ss) {
        Var s = Var.alloc("s");
        Var p = Var.alloc("p");
        Var o = Var.alloc("o");

        ExprVar vs = new ExprVar(s);

        Query query = QueryFactory.create();
        query.setQueryConstructType();
        query.setDistinct(true);
        Triple triple = new Triple(s, p, o);
        ElementGroup group = new ElementGroup();
        group.addTriplePattern(triple);

        List<Expr> exprs = new ArrayList<Expr>();
        for(Node item : ss) {
            if(!item.isURI()) {
                continue;
            }

            exprs.add(new E_Equals(vs, NodeValue.makeNode(item)));
        }

        if(exprs.isEmpty()) {
            return null;
        }

        Expr or = org.aksw.commons.jena.util.ExprUtils.orifyBalanced(exprs);
        group.addElementFilter(new ElementFilter(or));

        BasicPattern bgp = new BasicPattern();
        bgp.add(triple);
        query.setConstructTemplate(new Template(bgp));
        query.setQueryPattern(group);

        return query;
    }

    public static Query constructBySubject(Node s) {
        Var p = Var.alloc("p");
        Var o = Var.alloc("o");
        Triple triple = new Triple(s, p, o);

        BasicPattern basicPattern = new BasicPattern();
        basicPattern.add(triple);

        Template template = new Template(basicPattern);

        ElementGroup elementGroup = new ElementGroup();
        ElementPathBlock pathBlock = new ElementPathBlock();
        elementGroup.addElement(pathBlock);

        pathBlock.addTriple(triple);

        Query query = new Query();
        query.setQueryConstructType();
        query.setConstructTemplate(template);
        query.setQueryPattern(elementGroup);

        return query;
    }


    public static Query describe(Node node) {
        Query query = QueryFactory.create();
        query.setQueryDescribeType();
        query.getResultURIs().add(node);

        return query;
    }

    public static Query incoming(Node object) {
        return incoming("s", "p", object);
    }

    public static Query incoming(String varNameS, String varNameP, Node object) {
        Node s = Node.createVariable(varNameS);
        Node p = Node.createVariable(varNameP);

        return inOutTemplate(s, p, object);
    }

    public static Query outgoing(Node subject) {
        return outgoing(subject, "p", "o");
    }

    public static Query outgoing(Node subject, String varNameP, String varNameO)
    {
        Node p = Node.createVariable(varNameP);
        Node o = Node.createVariable(varNameO);

        return inOutTemplate(subject, p, o);
    }

    public static Query inOutTemplate(Node s, Node p, Node o)
    {
        Query query = QueryFactory.create();
        query.setQueryConstructType();
        query.setDistinct(true);
        Triple triple = new Triple(s, p, o);
        ElementGroup group = new ElementGroup();
        group.addTriplePattern(triple);

        // Avoid non-uris as objects
        if(o.isVariable()) {
            group.addElementFilter(new ElementFilter(new E_IsURI(new ExprVar(o))));
            group.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(o)))));
        }

        BasicPattern bgp = new BasicPattern();
        bgp.add(triple);
        query.setConstructTemplate(new Template(bgp));
        query.setQueryPattern(group);

        return query;
    }
}
