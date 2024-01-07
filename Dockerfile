FROM gradle:8.5.0-jdk21 as TEMP_BUILD_IMAGE

ENV APP_BUILD=/usr/app
WORKDIR $APP_BUILD

COPY build.gradle.kts settings.gradle.kts $APP_BUILD
COPY gradle $APP_BUILD/gradle
COPY --chown=gradle:gradle . /home/gradle/src

USER root

COPY . .

RUN export VERSION=$(git rev-parse --short HEAD)
RUN gradle clean shadowjar

FROM gcr.io/distroless/java21-debian12

ENV ARITFACT_NAME=Screenshitter.jar
ENV APP_HOME=/usr/app

WORKDIR $APP_HOME

COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/$ARTIFACT_NAME .

CMD ["Screenshitter.jar"]