FROM maven:3.6.3-jdk-11

WORKDIR /usr/src

COPY . .

RUN mvn clean deploy