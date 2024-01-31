# Build infinite-search

## Prerequisites
1. OpenAI API
2. Google Search API
3. Java 21

Fill in the corresponding information in `application-dev.properties`


## Build

```shell
./gradlew build -x test
```

## Run

```shell
java -jar build/libs/infinite-search-$version.jar
```

Open browser and visit http://localhost:8605