I want to build an tinyURL service which will take a large URL and convert it into a small or tiny URL

Features
- An user should be able to create unique tiny URL for a given URL
- An user should be able to get the already created tiny URL for the given URL
- An user should be forwarded to the actual URL when the user tries to navigate to tiny URL

UI
- User should be able to see all the tiny URL and given URL so far they have created
- User should be able to enter a URL and create an tiny URL
- User should be able to enter a URL and get the tiny URL already created ( system should notufy if it was created or already exist)
- User should be able to navigate to the tiny URL ( System forwards to the actual URL)

Caching
System should cache the recent tiny URL and given URL mappings


Logging
- All create and get requests should be logged
- all navigation requests to be logged
- all cache/hit misses to be logged

Implementation
- backend: Java, spring based microservice
- frontend - React + Tailwind UI
- Database - mysql
- cache - Redis

