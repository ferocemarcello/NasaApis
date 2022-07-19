# NasaAPIs #

## Solution ##


To run the server, you can either use the jar available in the root folder or use the app deployed online.


Local jar:


run it from the terminal with the following command:

java -jar spondAssignment-1.0-SNAPSHOT-jar-with-dependencies.jar {nasa-api-key} {port-number} [cache-size-for-asteroids-in-date-range] [cache-size-for-largest-asteroid-description-in-year] [path-to-file-with-db-config].

The parameter path-to-file-with-db-config is the path to a .yml file, that should look like like this:


```
host : "your_host"
port : "your_ort"
user : "your_username"
pwd : "your_password"
db : "your_database_name"
```

for a postgres Database


Once the server(localhost) is up and running, go to the host and port displayed by the program(E.g http://localhost:8080/)


Online


It is deployed on Heroku. It is available here https://asteroidspark.herokuapp.com.

The port is 8080. The number of days in the cache for the dates request is 20 while the cache holds at max 5 years. These parameters are customizable in the jar version.

Be aware that for the largest asteroid of the year(ex: "https://asteroidspark.herokuapp.com/asteroids/largest?year=2021") you might have to wait about one minute or might encouter error and you'll have to try again. This is because NASA allows only requests for date ranges no longer than 8 days, so one has to do about 50 requests to NASA and it takes time. It would be much faster if already in the cache or in the database.

At /index or / you can find basic information about the caches and the Database you are using

You can append:
    
    - /asteroids/dates?fromDate={{fromDate}}&toDate={{toDate}} for list of asteroids during a date range (Example http://localhost:8080/asteroids/dates?fromDate=2021-01-01&toDate=2021-01-06)
    
    -/asteroids/largest?year={{year}} for description of the largest asteroids in one year(Example http://localhost:8080/asteroids/largest?year=2022)
    
The date should be in the format "YYYY-MM-DD".
You can't request a date range longer than 7 days unless some dates are already present in the cache or in the database

Example, in sequence:


        Request 1: fromDate=2021-01-01&toDate=2021-01-10 throws error
        Request 2: fromDate=2021-01-01&toDate=2021-01-05 works and puts data in the cache and database
        Request 3: fromDate=2021-01-06&toDate=2021-01-11 works and puts data in the cache and database
        Request 3: fromDate=2021-01-12&toDate=2021-01-18 works and puts data in the cache and database
        Request 4: fromDate=2021-01-19&toDate=2021-01-26 works and puts data in the cache and database, the cache is now full and the LFU elements are evicted
        Request 4: fromDate=2020-12-30&toDate=2021-01-07 works, takes the days in the december from NASA, the first 6 days of January from the Db, and the last day from the cache
    
Example URLs : 
        http://localhost:8080/asteroids/largest?year=2021
        http://localhost:8080/asteroids/dates?fromDate=2022-01-01&toDate=2022-01-07
        https://asteroidspark.herokuapp.com/asteroids/dates?fromDate=2022-01-01&toDate=2022-01-07
        https://asteroidspark.herokuapp.com/asteroids/largest?year=2021
        
     
Unit tests are availble in https://github.com/ferocemarcello/NasaApis/tree/main/src/test/java. Integration tests would require to start the app and hit the app to see the results.


A postgres db is available on heroku.

Example of Responses are available here:

- https://github.com/ferocemarcello/NasaApis/blob/main/datesExampleResponse.json
- https://github.com/ferocemarcello/NasaApis/blob/main/yearLargestExampleResponse.json


Other 4 APIs are available:



/dates/closeCache :  closes the cache for the dates and returns the previous size

/years/closeCache :  closes the cache for the years and returns the previous size

/dates/cacheSize :   returns the current size of the cache for the dates

/dates/cacheSize :   returns the current size of the cache for the years

/dates/dbEntries : returns information about the current entries for the table dates
/years/dbEntries : returns information about the current entries for the table dates
/connectToDb : connects the application to the database
/disconnectDb : disconnects the application from the database
