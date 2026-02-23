# Dockerfile for Local Development and EMR Testing
FROM openjdk:11-jdk-slim

WORKDIR /app

# Install Scala and SBT
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install SBT
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40604AE" | apt-key add && \
    apt-get update && apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

# Copy project files
COPY . /app/

# Cache SBT dependencies
RUN sbt update || true

# Build the project
RUN sbt clean assembly

# Set environment variables
ENV SPARK_ENV=local
ENV LOG_LEVEL=INFO

# Default command
CMD ["/bin/bash"]

