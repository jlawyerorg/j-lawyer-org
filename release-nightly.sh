#!/bin/bash

./clean.sh
./build-fast.sh
rm ../j-lawyer-releases/j-lawyer-client/lib/*.jar
rm ../j-lawyer-releases/j-lawyer-client/j-lawyer-client.jar
rm ../j-lawyer-releases/j-lawyer-server/wildfly/standalone/deployments/j-lawyer-server.ear
rm ../j-lawyer-releases/j-lawyer-server/wildfly/standalone/deployments/j-lawyer-server.ear.deployed
cp ./j-lawyer-client/dist/j-lawyer-client.jar ../j-lawyer-releases/j-lawyer-client/
cp ./j-lawyer-client/dist/lib/*.jar ../j-lawyer-releases/j-lawyer-client/lib/
cp ./j-lawyer-server/dist/j-lawyer-server.ear ../j-lawyer-releases/j-lawyer-server/wildfly/standalone/deployments/


rm ../j-lawyer-client-installer/media/*.*
rm ../j-lawyer-client-installer/media/*
~/bin/install4j8/bin/install4jc ../j-lawyer-client-installer/j-lawyer-client-installer.install4j
rm ../j-lawyer-server-installer/media/*.*
rm ../j-lawyer-server-installer/media/*
~/bin/install4j8/bin/install4jc ../j-lawyer-server-installer/j-lawyer-server-installer.install4j

rm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/*.*
rm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/*
rm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/*.*
rm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/*

cp ../j-lawyer-client-installer/media/*.dmg ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/
cp ../j-lawyer-client-installer/media/*.exe ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/
cp ../j-lawyer-client-installer/media/*.sh ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/
cp ../j-lawyer-client-installer/media/*.deb ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/
cp ../j-lawyer-client-installer/media/*.rpm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/
cp ../j-lawyer-client-installer/media/sha256sums ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Client/sha256sums-client.txt

cp ../j-lawyer-server-installer/media/*.dmg ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/
cp ../j-lawyer-server-installer/media/*.exe ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/
cp ../j-lawyer-server-installer/media/*.sh ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/
# cp ../j-lawyer-server-installer/media/*.deb ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/
# cp ../j-lawyer-server-installer/media/*.rpm ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/
cp ../j-lawyer-server-installer/media/sha256sums ~/Nextcloud/dev/j-lawyer-releases/j-lawyer-released/Vorschau/Server/sha256sums-server.txt