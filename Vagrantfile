VAGRANTFILE_API_VERSION = "2"

$script = <<SCRIPT
apt-get update
dpkg -l | grep postgresql || apt-get install -y postgresql
dpkg -l | grep openjdk-7-jdk || apt-get install -y openjdk-7-jdk
[ -d "/home/vagrant/bin" ] || sudo -u vagrant mkdir /home/vagrant/bin
ls /home/vagrant/bin/lein || sudo -u vagrant wget -P /home/vagrant/bin https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein >/dev/null 2>&1
chmod +x /home/vagrant/bin/lein
SCRIPT


Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.define :alpha do |config|
    config.vm.box = "precise64"
    config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/precise/current/precise-server-cloudimg-amd64-vagrant-disk1.box"

    config.vm.hostname = "alpha"

    config.vm.network "forwarded_port", guest: 3000, host: 3030
    config.vm.network "private_network", ip: "192.168.179.222"

    config.vm.provision :shell, inline: $script
  end
end
