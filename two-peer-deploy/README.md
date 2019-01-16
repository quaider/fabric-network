

总体部署与单机差别不大，需要解决的是夸机器docker的通讯问题。

fabric中docker的通讯使用的是域名(证书绑定)，因此最简单的解决方案是 在docker容器内部，将其需要连接的其他
容器 做一个地址和域名的映射。

``` yaml
# 添加主机名映射，将会在容器中的/etc/hosts创建记录   ip  hostname
extra_hosts: 
  # - "hostname:ip"
  - "orderer.cnabs.com:192.168.8.131"
  - "peer0.org1.cnabs.com:192.168.8.132"
```

也可以直接将其加载宿主机的 `/etc/hosts` 中，然后使用如下方式应用到docker中

``` yaml
volumes:
  - /etc/hosts:/etc/hosts
```

多peer节点时，当某一个peer节点进行了创建通道的操作并成功后，order会返还一个创世区块，如 channel.block，然后该节点 可以使用 `peer channel join -b cnabs.block` 加入仅通道中；

同一通道的创建只能进行一次，因此无法通过上述操作来获取 order 返还的创世区块，此时 可以采用如下方式来
获取创世区块，然后 利用该区块加入到通道中
``` shell
# 获取区块 peer channel fetch <newest|oldest|config|(number)> [outputfile] [flags]
peer channel fetch 0 -o orderer.cnabs.com:7050 -c cnabs
# 在本地生成了 cnabs_0.block，不太确定 fetch参数是否正确，但测试中节点还是加入了通道
```