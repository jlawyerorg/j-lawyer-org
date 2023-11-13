# Use latest jboss/base-jdk:11 image as the base
FROM jboss/base-jdk:8
MAINTAINER j-dimension "info@j-lawyer.org"

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION 16.0.0.Final
ENV WILDFLY_SHA1 287c21b069ec6ecd80472afec01384093ed8eb7d
ENV JBOSS_HOME /opt/jboss/wildfly

USER root

RUN mkdir -p /opt/jboss/j-lawyer-data/templates
RUN mkdir -p /opt/jboss/j-lawyer-data/emailtemplates
RUN mkdir -p /opt/jboss/j-lawyer-data/mastertemplates
RUN mkdir -p /opt/jboss/j-lawyer-data/archivefiles
RUN mkdir -p /opt/jboss/j-lawyer-data/searchindex
RUN mkdir -p /opt/jboss/j-lawyer-data/faxqueue
RUN chmod -R 777 /opt/jboss/j-lawyer-data
RUN yum -y install mysql

RUN localedef -i en_US -f UTF-8 en_US.UTF-8

# ugly as hell
# ADD sleep.sh /tmp/sleep.sh
# RUN chmod +x /tmp/sleep.sh

# Add the WildFly distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd $HOME \
    && curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

RUN chown -R jboss:0 /opt/jboss
RUN chmod -R g+rw /opt/jboss

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

COPY ./startup.sh /opt/jboss/startup.sh
RUN chmod 777 /opt/jboss/startup.sh


USER jboss

ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

COPY j-lawyer-data/ /opt/jboss/j-lawyer-data/

COPY ./standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY ./j-lawyer-server.ear /opt/jboss/wildfly/standalone/deployments/j-lawyer-server.ear
COPY ./mysql/main/* /opt/jboss/wildfly/modules/system/layers/base/com/mysql/main/


# Expose the ports we're interested in
EXPOSE 8080

VOLUME /opt/jboss/j-lawyer-data



ENTRYPOINT ["/opt/jboss/startup.sh"]

# CMD ["/tmp/sleep.sh"]

# Set the default command to run on boot
# This will boot WildFly in the standalone mode and bind to all interface
# CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
