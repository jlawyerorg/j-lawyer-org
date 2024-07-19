# By default, build on JDK 17
ARG jdk=17
FROM eclipse-temurin:${jdk}-centos7
MAINTAINER j-dimension "info@j-lawyer.org"

# LABEL org.opencontainers.image.source=https://github.com/jboss-dockerfiles/wildfly org.opencontainers.image.title=wildfly org.opencontainers.imag.url=https://github.com/jboss-dockerfiles/wildfly org.opencontainers.image.vendor=WildFly

WORKDIR /opt/jboss

RUN groupadd -r jboss -g 1000 && useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss && \
    chmod 755 /opt/jboss

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION 26.1.3.Final
ENV WILDFLY_SHA1 b9f52ba41df890e09bb141d72947d2510caf758c
ENV JBOSS_HOME /opt/jboss/wildfly

USER root

RUN mkdir -p /opt/jboss/j-lawyer-data/templates
RUN mkdir -p /opt/jboss/j-lawyer-data/emailtemplates
RUN mkdir -p /opt/jboss/j-lawyer-data/mastertemplates
RUN mkdir -p /opt/jboss/j-lawyer-data/archivefiles
RUN mkdir -p /opt/jboss/j-lawyer-data/searchindex
RUN mkdir -p /opt/jboss/j-lawyer-data/faxqueue
RUN mkdir -p /opt/jboss/j-lawyer-data/letterheads
RUN chmod -R 777 /opt/jboss/j-lawyer-data
RUN yum -y install mariadb

RUN localedef -i en_US -f UTF-8 en_US.UTF-8

# Add the WildFly distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd $HOME \
    && curl -L -O https://github.com/wildfly/wildfly/releases/download/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

COPY ./startup.sh /opt/jboss/startup.sh
RUN chmod 777 /opt/jboss/startup.sh


USER jboss

ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

COPY j-lawyer-data/ /opt/jboss/j-lawyer-data/

COPY ./standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY ./standalone.conf /opt/jboss/wildfly/bin/standalone.conf
COPY ./j-lawyer-server.ear /opt/jboss/wildfly/standalone/deployments/j-lawyer-server.ear
COPY ./mysql/driver8/main/* /opt/jboss/wildfly/modules/system/layers/base/com/mysql/driver8/main/


# Expose the ports we're interested in
EXPOSE 8080

VOLUME /opt/jboss/j-lawyer-data



ENTRYPOINT ["/opt/jboss/startup.sh"]

# CMD ["/tmp/sleep.sh"]

# Set the default command to run on boot
# This will boot WildFly in standalone mode and bind to all interfaces
# CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]