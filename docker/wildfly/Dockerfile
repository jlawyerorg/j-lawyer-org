# Use Eclipse Temurin JDK 17 on Ubuntu
FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="j-dimension <info@j-lawyer.org>"

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION=26.1.3.Final
ENV WILDFLY_SHA1=b9f52ba41df890e09bb141d72947d2510caf758c
ENV JBOSS_HOME=/opt/jboss/wildfly

# Create jboss user and group
RUN groupadd -r jboss -g 1000 && \
    useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    mariadb-client \
    locales \
    && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -f UTF-8 en_US.UTF-8

# Create data directories
RUN mkdir -p /opt/jboss/j-lawyer-data/templates \
    /opt/jboss/j-lawyer-data/emailtemplates \
    /opt/jboss/j-lawyer-data/mastertemplates \
    /opt/jboss/j-lawyer-data/archivefiles \
    /opt/jboss/j-lawyer-data/searchindex \
    /opt/jboss/j-lawyer-data/faxqueue \
    /opt/jboss/j-lawyer-data/letterheads \
    && chmod -R 777 /opt/jboss/j-lawyer-data

# Download and install WildFly
RUN cd /tmp \
    && curl -LO https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.tar.gz \
    && sha1sum wildfly-${WILDFLY_VERSION}.tar.gz | grep ${WILDFLY_SHA1} \
    && tar xf wildfly-${WILDFLY_VERSION}.tar.gz \
    && mv wildfly-${WILDFLY_VERSION} ${JBOSS_HOME} \
    && rm wildfly-${WILDFLY_VERSION}.tar.gz \
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

RUN chown -R jboss:0 /opt/jboss
RUN chmod -R g+rw /opt/jboss

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

COPY ./startup.sh /opt/jboss/startup.sh
RUN chmod 777 /opt/jboss/startup.sh

# MariaDB JDBC driver
RUN mkdir -p /opt/jboss/wildfly/modules/org/mariadb/driver/main
COPY ./mariadb/driver/main/ /opt/jboss/wildfly/modules/org/mariadb/driver/main/
RUN chown -R jboss:0 /opt/jboss/wildfly/modules/org/mariadb

USER jboss

ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

COPY j-lawyer-data/ /opt/jboss/j-lawyer-data/

COPY ./standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY ./standalone.conf /opt/jboss/wildfly/bin/standalone.conf
COPY ./j-lawyer-server.ear /opt/jboss/wildfly/standalone/deployments/j-lawyer-server.ear


# Expose the ports we're interested in
EXPOSE 8080

VOLUME /opt/jboss/j-lawyer-data



ENTRYPOINT ["/opt/jboss/startup.sh"]

# CMD ["/tmp/sleep.sh"]

# Set the default command to run on boot
# This will boot WildFly in the standalone mode and bind to all interface
# CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
