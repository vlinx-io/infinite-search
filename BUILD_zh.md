# 构建项目

## 前置准备
1. OpenAI API
2. Google Search API
3. Java 21

把相应的信息填入到 `application-dev.properties`


## Build

```shell
./gradlew build -x test
```

## Run

```shell
java -jar build/libs/infinite-search-$version.jar
```

打开浏览器访问 http://localhost:8605