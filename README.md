SPARQLFederator
===============

Expand SPARQL queries to perform inference on multiple endpoints.


Main documentation
==================

Find more information on our wiki page:

https://github.com/djogopatrao/SPARQLFederator/wiki


DEBUG compiling and running:
==========

mvn clean package 


mvn exec:java -Dexec.mainClass="br.org.accamargo.cipe.gqe.SPARQLFederator" -Dexec.args="federation_ontology.owl federation_namespace# domain_ontology.owl  domainNamespace# class1 [,classn]"

- SPARQLFederator class will create a federated query.
- SPARQLFederatorRun class will create the query and run it.


(sort of) PRODUCTION compiling and running( jar with all dependencies included)
====================

mvn clean compile assembly:single

java -jar jar-with-dependencies.jar federation_ontology.owl federation_namespace# domain_ontology.owl domainNamespace# class1[ classn]


teste
