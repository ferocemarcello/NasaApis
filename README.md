# spondAssignment



# Aim of the task

The aim of this task is to assess the way you approach problems, including your coding style, expertise and willingness to experiment, as well as provide us with a common ground for a technical interview.

We'd love to see what kind of solution you come up with and how you approach problem solving. 

There is no hard time limit set for this task, but generally one evening should be the goal. Due to time constraints, we don't expect all the bells and whistles and you’re encouraged to focus on your core strengths and things that you think are important for production service. 

The NASA APIs require you to sign up here (it’s free and takes less than a minute), this will allow you to execute approximately 1000 API calls per hour. You can generate an API key at [NASA APIs](https://api.nasa.gov).

Your feedback is important to us, so let us know what you think about the task — before you started or after you’re done ☺️


# Description

Imagine a customer has asked you to build a tool that can provide data on nearby asteroids using [NASA Asteroids API](https://api.nasa.gov). The tool is required to have the following features:



1. Show 10 asteroids that passed the closest to Earth between two user-provided dates.
2. Show characteristics of the largest asteroid (estimated diameter) passing Earth during a user-provided year.

Using Java, implement a RESTful API service to handle such queries. The service should employ some kind of caching to avoid extra external API calls.

Some general things to consider:



* Database modeling. Should data be persisted in the database? 
* API design. How should it be presented in the API? Will it be easy to include more queries or extend the current ones to cater for new feature requests?
* Would it be easier for other developers to get up-and-running if you use docker?
* Testing approach. How would you approach it in your implementation?

Final result should consist of:



1. Source code with instructions on how to run it in a git repository we can access (Github, Bitbucket etc.)
2. A service deployed to a cloud provider of your choice using IaC approach.
    1. This is optional — only do it if you would like to demonstrate your DevOps skills.
    
To run the server, use the jar available in the root folder.
run it from the terminal with the following command:

java -jar spondAssignment-1.0-SNAPSHOT-jar-with-dependencies.jar {port-number} {cache-size-for-asteroids-in-date-range} {cache-size-for-largest-asteroid-description-in-year} {nasa-api-key}

Once the server is up and running, go to the host and port displayed by the program(E.g http://localhost:8080/)
You can append:
    - /asteroids/dates?fromDate={{fromDate}}&toDate={{toDate}} for list of asteroids during a date range
    - /asteroids/largest?year={{year}} for description of the largest asteroids in one year
