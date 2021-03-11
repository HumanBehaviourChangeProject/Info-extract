#keygen steps
ssh-keygen -t rsa -b 4096 -C "debasis.ganguly1@ie.ibm.com"
eval "\$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa

#clone git repo
git clone git@github.ibm.com:Dublin-Research-Lab/hbcpIE.git 

# copy softwares
scp ~/softwares/jdk-8u171-linux-x64.tar.gz dganguly@104.214.234.232:~/
scp ~/softwares/apache-tomcat-8.5.31.tar.gz dganguly@104.214.234.232:~/
