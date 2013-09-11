package br.org.accamargo.cipe.gqe;

import java.util.Arrays;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpAsQuery;
import com.hp.hpl.jena.sparql.algebra.Transformer;

public class SPARQLFederator {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
        String ontocloudOntology = args[0];
        String ocNS = args[1];
        String domainOntology = args[2];
        String dmNS = args[3];
        String class_name = args[4];
        
        System.out.println(ontocloudOntology);
        
        GrumpyQueryExpander gqe = new GrumpyQueryExpander(ontocloudOntology, ocNS, domainOntology, dmNS); 
        GrumpyOptimizer go = new GrumpyOptimizer();
        GrumpyPlanner gp = new GrumpyPlanner();
        
        // plan statistics
        StaticCosts.setCosts(gp,dmNS);

        // create query
        String query2 = gqe.createQueryFromClasses(Arrays.asList(class_name));
        System.out.println(query2);
        Op op = Algebra.compile(QueryFactory.create(query2));
        
        // optimize 
        // TODO how many times are enough? 
        for(int i=0; i<1000; i ++ ) 
                op = Transformer.transform(go, op );
        
        String optimized_query = OpAsQuery.asQuery(op).toString(); 
        System.out.println(optimized_query);
        
        // plan execution
        // TODO how many times are enough? 
        for (int i=0; i<1000;i ++)
        	op = Transformer.transform(gp, op);
        
        String planned_query= OpAsQuery.asQuery(op).toString(); 
        System.out.println(planned_query);
        
        
        System.out.println("Original query:"+query2.length());
        System.out.println("Optimized query:"+optimized_query.length());
        System.out.println("Planned query:"+planned_query.length());
        
        Pattern p = Pattern.compile("SERVICE");
        java.util.regex.Matcher m_query = p.matcher(query2);
        java.util.regex.Matcher m_query_opt = p.matcher(optimized_query);
        
        int ct=0;
        while( m_query.find()) ct++;
        System.out.println("Query original SERVICES: " + ct);
        
        ct=0;
        while( m_query_opt.find()) ct++;
        System.out.println("Query optimizada SERVICES: " + ct);
        

	}

}
