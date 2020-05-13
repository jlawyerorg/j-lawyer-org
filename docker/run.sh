sudo mkdir -p /var/docker_data/j-lawyer-data/
sudo mkdir -p /var/docker_data/j-lawyer-data/templates
sudo mkdir -p /var/docker_data/j-lawyer-data/emailtemplates
sudo mkdir -p /var/docker_data/j-lawyer-data/mastertemplates
sudo mkdir -p /var/docker_data/j-lawyer-data/archivefiles
sudo mkdir -p /var/docker_data/j-lawyer-data/searchindex
sudo mkdir -p /var/docker_data/j-lawyer-data/faxqueue
sudo chmod -R 777 /var/docker_data/j-lawyer-data
docker-compose up