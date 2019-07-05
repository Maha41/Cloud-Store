# CSYE 6225 - Spring 2019
Web application based on Spring Boot, RESTful API and MySQL.
## Team Information

| Name | NEU ID | Email Address |
| --- | --- | --- |
| Kinjal Patel| 001831447 | patel.kin@husky.neu.edu|
| Siddhesh Kuvalekar| 001238765| kuvalekar.s@husky.neu.edu|
| Mahalakshmi Arunachalam| 001810136 | arunachalam.m@husky.neu.edu|
| Karan Mahaddalkar| 001821603 | mahaddalkar.k@husky.neu.edu |

## Technology Stack

Spring Boot - The web framework used
Gradle - Dependency Management
MySQL - Relational App Database
IntelliJ - IDE used to develop the web app

## Build Instructions
Make the below changes in application.properties file
`
spring.datasource.url=jdbc:mysql://localhost:3306/your database name
spring.datasource.username=your db user name
spring.datasource.password=your db password
`


## Deploy Instructions


## Running Tests
 

## CI/CD

#RESTful API Endpoints Implemented


| HTTP method | URI path | Description|
| GET | / | Get the home page|
| POST | /user/register | Create user account |
| GET | /note | Get all notes of the user |
| POST |/note |	Create a note of the user |
| PUT | /note/{id} | Update a note of the user |
| DELETE | /note/{id} | Delete a note of the user |
| GET | /note/{id} | Get a note of the user |




