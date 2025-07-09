FROM ubuntu:latest
LABEL authors="tymoteuszbolalek"

ENTRYPOINT ["top", "-b"]