package container

import (
	"bufio"
	"io/ioutil"
	"os"
	"strconv"
	"strings"

	"github.com/subutai-io/base/agent/agent/utils"
	"github.com/subutai-io/base/agent/config"
	cont "github.com/subutai-io/base/agent/lib/container"
	"github.com/subutai-io/base/agent/lib/gpg"
	lxc "gopkg.in/lxc/go-lxc.v2"
)

type Container struct {
	Id         string        `json:"id"`
	Name       string        `json:"name"`
	Hostname   string        `json:"hostname"`
	Status     string        `json:"status,omitempty"`
	Arch       string        `json:"arch"`
	Interfaces []utils.Iface `json:"interfaces"`
	Parent     string        `json:"templateName,omitempty"`
	Vlan       int           `json:"vlan,omitempty"`
	Pk         string        `json:"publicKey,omitempty"`
}

func GetCredentials(name, container string) (uid int, gid int) {
	path := config.Agent.LxcPrefix + container + "/rootfs/etc/passwd"
	u, g := parsePasswd(path, name)
	uid, _ = strconv.Atoi(u)
	gid, _ = strconv.Atoi(g)
	return uid, gid
}

func parsePasswd(path, name string) (uid string, gid string) {
	file, _ := os.Open(path)
	defer file.Close()
	scanner := bufio.NewScanner(file)
	scanner.Split(bufio.ScanLines)

	for scanner.Scan() {
		if strings.Contains(scanner.Text(), name) {
			arr := strings.Split(scanner.Text(), ":")
			if len(arr) > 3 {
				return arr[2], arr[3]
			}
		}
	}
	return "", ""
}

func GetActiveContainers(details bool) []Container {
	contArr := []Container{}

	for _, c := range cont.Containers() {
		hostname, _ := ioutil.ReadFile(config.Agent.LxcPrefix + c + "/rootfs/etc/hostname")
		configpath := config.Agent.LxcPrefix + c + "/config"

		container := Container{
			Id:         gpg.GetFingerprint(c),
			Name:       c,
			Hostname:   strings.TrimSpace(string(hostname)),
			Status:     cont.State(c),
			Arch:       strings.ToUpper(cont.GetConfigItem(configpath, "lxc.arch")),
			Interfaces: GetContainerIfaces(c),
			Parent:     cont.GetConfigItem(configpath, "subutai.parent"),
		}
		if details {
			container.Pk = gpg.GetContainerPk(c)
		}

		contArr = append(contArr, container)
	}
	return contArr
}

func GetContainerIfaces(name string) []utils.Iface {
	iface := new(utils.Iface)

	c, err := lxc.NewContainer(name, config.Agent.LxcPrefix)
	if err != nil {
		return []utils.Iface{*iface}
	}

	iface.InterfaceName = "eth0"
	listip, _ := c.IPAddress(iface.InterfaceName)
	iface.Ip = strings.Join(listip, " ")

	return []utils.Iface{*iface}
}
