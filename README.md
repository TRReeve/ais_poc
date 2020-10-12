# AIS POC

A poc pipeline demonstrating streaming information
using akka streams to a postgres database.

### Requirements
Docker + Docker-compose
Gradle
Java 8
Makefile

# Instructions to run
    make runapp

Or 
1. make startinfra (or look at Makefile to get the commands being issued)
2. Once the python app and the database are running then


    gradle run 


will compile and start the streaming application

3. You can access the database at localhost:5430 using psql


    psql -d ais_poc -U postgres -h localhost -p 5430


password is 'goodsecurity'

### Components
- Main Stream scaffold (Scala + Akka): Extracts, Validates and pushes data to components, also capable of doing its 
own analytics and rendering them given more dev time.
- Enrichment Application (Python): Small Enrichment App written using FastApi and uvicorn
this performs a reverse lookup on point information and returns that information to the main stream
pipeline. This is mainly for demonstrating the capabilities of how we can link together multiple data sources, as 
well as being a test of FastAPI and uvicorn as opposed to Flask. 

There are automatically generated documentations for the endpoint at localhost:8000/docs when the 
infrastructure is running

within the framework of streaming as well as giving us a valuable insight into the spatial data.
- Database (Postgis): Postgres equipped with more geospatial functions. Acts as main 
sink for data. 

### Deployment
The original plan was to deploy all components in the same docker-compose template
simulating a kubernetes deployment. There are some intermittent issue with FTP connections 
and docker that seems to happen on windows so for now the deployment is the enrichment application
and the database are run as an infrastructure and the application is run directly using gradle. The stream_etl
section can be uncommented, this will give an error without updating of the hosts and paths for the
enrich application and the database

### Output
The main output of this report is a minute by minute summary of different vessels
and the countries waters they are associated with using the enriched data set.


### Features
- Cats functional programming framework used to validate data in parallel and 
push it to a "good path" and "bad path"
- Enrichment application integrated with streaming data using batching to aim for 
thousands of events a second throughput
- Dockerised deployments mean relative confidence in cross-platform functionality. 
- Using Akka Streams means we could have the exact same application hooked up to a 
Kafka broker or other databases instead of reading from FTP files and know the stream will operate the same 
way. Akka Streams DSL Also means it is relatively trivial to understand the flow 
of the main business logic sections. 

e.g 


        broadcast.out(0) ~> map_to_request ~> enrich_data ~> sink_enrichments_to_db ~> Sink.ignore
        broadcast.out(1) ~> sink_ais_messages_to_db ~> Sink.ignore
        
- In addition to being able to stream off data to other message brokers without 
too much integration work, we could also hold data in collection 
(e.g a web interface in JSON listing all the cargo vessels latest locations)
on the application locally, especially in a distributed context this can be very powerful without requiring the 
amount of infrastructure and work as spark or flink or other stream processors. 