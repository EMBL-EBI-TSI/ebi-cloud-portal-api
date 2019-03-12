# cloud-portal-public-repo

The Portal API web services as seen by external clients (e.g. Portal web applications).  

## HOW-TO run  

This project uses Spring-Boot and Maven. In order to run it, after installing all the software dependencies as mentioned below just run  

    mvn spring-boot:run  

 The API entry point is the root [http://localhost:8080](http://localhost:8080).  

Right now the API endpoints are only accessible by `jwt` tokens. 

For the application to be setup, follow the comments in application.properties. The file can be divide into 2 sections 
as follows

1. Settings to be modified 
   Users please follow the comments under this heading, minimal setup is required to get portal running.
   
2. Settings which can be used a boilerplate 	
   Users can use it as it is, no modification is required
   

### Software Dependencies  

The portal backend needs a DB and an ElasticSearch index. Both of them can be provided using Docker containers. The properties file contains comments about how to run them.  

### Java and Maven

Portal API is a java based maven project. 

Install java on your machine following instructions from https://www.java.com/en/download/manual.jsp

To compile and run the application you would require Maven. Detailed instructions on how to setup and run Maven
can be found at https://maven.apache.org/install.html.

### Docker

Portal API application uses docker for database setup (more information on how to do this, can be found at src/main/resources/application.properties), for running integration tests and elastic instance. 

Setup docker on your machine, by downloading appropriate software based on your OS from https://docs.docker.com/.

#### ElasticSearch index

Users please download https://github.com/deviantony/docker-elk and using the master branch, spin up the elk instance in docker using docker-compose up (more information can be found in the docker-compose.yml file in the project itself.)

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

### Running intergration tests

In order to run integration test following prerequisites are needed. Test user credentials of AAP dev server needed to be set as environmental variables Variables : AAP_TEST_USERNAME,AAP_TEST_PASSWORD Docker host should be set or docker from localhost can be used.Port 5432 must be free. Execute:-

mvn clean verify

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


## Default Teams

We wanted to group people in teams namely EMBL-EBI (UK based) and EMBL (whole EMBL except UK) so that sharing and un-sharing resources can be done organization wide, which would help people to adopt the Cloud Portal easily.

This is the information found in /default_teams.json file.

People who sign in and having email id’s ending in ‘ebi.ac.uk’ would be automatically be put in team EBI and 
those having email id’s ending in ‘embl.de’ would be in team EMBL.

Developers can adopt this idea and customize it to suit their organization.

Before that bear in mind, ‘Team’ in ECP application is called a ‘Domain’ in AAP and that
team name is universal.


But firstly, a developer has to do the following:

1.	Create teams from the ECP application. These are the teams to which, people logging in would be added to.
    In the given scenario, as per the /default_teams.json the teams would be 'EBI' and 'EMBL-EBI'. 
    
2.	Go to AAP application (https://aai.ebi.ac.uk) and create an AAP account. (more like a system account)

3.	The user who created the team, sign in into https://aai.ebi.ac.uk  and list the domains in AAP from under the ‘My Domains’     tab. Look for the domain in AAP. It would be in the following format: 

                TEAM_{team_name_from_ECP}_PORTAL_{username-of-the-person-who-created-team}
                
    e.g. Team named ‘EBI’ in ECP would be like TEAM_EBI_PORTAL_USR-XXXXXXXX-XXXX-XXXX-XXXXXXXXXXXXXX  in AAP
    
4.	Once you have located the Domain, select it and add a Manager by supplying the username of the AAP account you created in     Step 2

5.	Supply the AAP account credentials (created in Step 2), via the following 2 fields found in application.properties

    => ecp.aap.username=your-aap-local-account-for-ecp
    => ecp.aap.password=changeme
