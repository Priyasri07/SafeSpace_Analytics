# WSL Ubuntu Setup Guide

## Install Apache Pig 0.17.0
  cd ~
  wget https://archive.apache.org/dist/pig/pig-0.17.0/pig-0.17.0.tar.gz
  tar -xzf pig-0.17.0.tar.gz
  echo 'export PIG_HOME=$HOME/pig-0.17.0' >> ~/.bashrc
  echo 'export PATH=$PATH:$PIG_HOME/bin' >> ~/.bashrc
  source ~/.bashrc
  pig --version

## Install Apache Sqoop 1.4.7
  cd ~
  wget https://archive.apache.org/dist/sqoop/1.4.7/sqoop-1.4.7.bin__hadoop-2.6.0.tar.gz
  tar -xzf sqoop-1.4.7.bin__hadoop-2.6.0.tar.gz
  echo 'export SQOOP_HOME=$HOME/sqoop-1.4.7.bin__hadoop-2.6.0' >> ~/.bashrc
  echo 'export PATH=$PATH:$SQOOP_HOME/bin' >> ~/.bashrc
  source ~/.bashrc
  sqoop version

## Install MongoDB 7.0
  # Follow official MongoDB WSL guide:
  # https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/
  sudo apt-get install gnupg curl -y
  curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor --yes
  echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
  sudo apt-get update
  sudo apt-get install -y mongodb-org
  sudo service mongod start
  mongod --version

## Install MySQL 8.0
  sudo apt update
  sudo apt install mysql-server -y
  sudo service mysql start
  mysql --version

## Extract Hive 4.0.0 (already downloaded)
  cd ~
  tar -xzf apache-hive-4.0.0-bin.tar.gz
  mv apache-hive-4.0.0-bin hive-4.0.0
  echo 'export HIVE_HOME=$HOME/hive-4.0.0' >> ~/.bashrc
  echo 'export PATH=$PATH:$HIVE_HOME/bin' >> ~/.bashrc
  source ~/.bashrc
  hive --version
