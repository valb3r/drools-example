# What is this

This is Drools Business Rule management system example


# What are the modules

There is [GUI module](drools-gui) - more complicated example
There is [Simple module](drools-simple) - more simple example


# What to read

1. Docs on drools:
https://docs.drools.org/8.35.0.Final/drools-docs/docs-website/drools/language-reference/index.html#decision-tables-con_decision-tables
2. Language used (MVEL):
http://mvel.documentnode.com

# Useful stuff

Get drools code from XLSX file:
```
new DecisionTableProviderImpl().loadFromResource(new FileSystemResource("/Users/valentynberezin/IdeaProjects/FinTegra/fintegra-platform/data-ingestion/psplus-mapping-application/test/transform.xlsx"), new DecisionTableConfigurationImpl())
```