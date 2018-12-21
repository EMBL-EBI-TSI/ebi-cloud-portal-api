# EBI Cloud Portal API

The EBI Cloud Portal API web services as seen by external clients (e.g. Portal web applications).  

## HOW-TO run  

This project uses Spring-Boot and Maven. In order to run it, just run  

    mvn spring-boot:run  

 The API entry point is the root [http://localhost:8080](http://localhost:8080).  

Right now the API endpoints are only accessible by `jwt` tokens.  

### Properties  

For the application to be setup, follow the comments in application.properties.

I. The file 'application.properties' has been divided into two sections. 
-----------------------------------------------------------------------

1. Settings to be modified 
   Users please follow the comments under this heading, minimal setup is required to get portal running.
   
2. Settings which can be used a boilerplate 	
   Users can use it as it is, no modification is required

### Dependencies  

The portal backend needs a DB and an ElasticSearch index. Both of them can be provided using Docker containers. The properties file contains comments about how to run them.  

#### ElasticSearch index

The ElasticSearch instance has to have an index defined, that matches the `elasticsearch.index` 
specified in the `src/main/resources/application.properties` file. If you use the above mentioned `docker-elk` setup,
you visit the Kibana GUI at http://localhost:5601 with the `elasticsearch.*` credentials, 
and use Kibana console (in the devtools tab) to send the index creation command to its API:  

```
PUT ecp-deployments
{
    "settings" : {
        "index" : {
            "number_of_shards" : 1, 
            "number_of_replicas" : 1 
        }
    }
}
```
Production environments must change the credentials, and should tweak the settings depending on the workload.

## Example usage

Once the Portal API web services are up and running, clients can make REST calls. The client authorisation 
is performed with JSON Web Tokens (https://jwt.io/) which are issued by the EBI's Authentication and Authorization Profile (AAP) infrastructure. You can manually log into https://aai.ebi.ac.uk/login with the Elixir Single-Sign-On,
and copy the token displayed on https://api.aai.ebi.ac.uk/sso. Web clients can use libraries like https://www.npmjs.com/package/angular-aap-auth to handle the process. An example call to the API returns the list of deployments, 
which is initially empty `{}`:
```
curl -H 'Accept: application/json' -H 'Content-Type: application/json' -H 'Authorization: Bearer eyJh...K0GA' -X GET http://localhost:8080/deployment
```

## Design  

The RESTful API is designed under the [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) constraint in order to allow clients to be as decoupled as possible from its specification.

## References  

- A good intro to [Building REST services with Spring](http://spring.io/guides/tutorials/bookmarks/)  
- One about to [Accessing Data with JPA and Spring](http://spring.io/guides/gs/accessing-data-jpa/)  
- Wikipedia about [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS).    
- [Spring Boot](http://projects.spring.io/spring-boot/).  
- [Spring Boot starter](http://start.spring.io/).  






