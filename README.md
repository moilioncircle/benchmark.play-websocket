#Play 2.4.x Websocket演示及压力测试


## 演示说明

涉及的技术点：Actor，Websocket，Json

心跳演示，模拟了`客户端`通过`网关服务器`向`业务服务器`发送心跳的过程。

支付演示，模拟了第三方支付的过程，参与者有`客户端`，‘支付服务’，`第三方支付`。

`客户端`向`支付服务`发送支付请求，然后`客户端`定向到`第三方支付`页面支付。
`支付服务`轮询`第三方支付`获得结果，反馈给`客户端`

压力测试：通过apache的ab进行单机的简单压力测试。


## 使用说明

系统要求 java8，
测试过的浏览器有chrome或firefox

```
git clone https://github.com/moilioncircle/play-websocket-sample.git`
cd play-websocket-sample
./activator dist
ls -l target/universal/play-websocket-sample-1.0.0.zip
```

把上步生成的zip文件，解压到目标目录，如play-websocket-sample/，
可以执行 `play-websocket-sample` 启动服务器，
在浏览器打开（本机） http://127.0.0.1:9000

也可把以下便捷脚本复制到play-websocket-sample/目录下，给执行权限。

``` bash
#!/bin/bash
cd $(dirname $(readlink -f $0))
pwd
export JAVA_OPTS="-server -Xms512m -Xmx4096m"

case $1 in
    start)
        if [ -f RUNNING_PID ];then
            echo "already running"
        else
            nohup bin/play-websocket-sample >/dev/null 2>&1 &
            echo "running"
        fi
      ;;
    stop)
        if [ -f RUNNING_PID ];then
            kill -9 `cat RUNNING_PID`
            rm -rf RUNNING_PID
            echo "stoped"
        fi
      ;;
    test)
        echo ab -k -c1000 -n35000 http://127.0.0.1:9000/hello-world
        ab -k -c1000 -n35000 http://127.0.0.1:9000/hello-world
      ;;
    status)
        if [ -f RUNNING_PID ];then
            ps -f `cat RUNNING_PID`
        else
            echo "not running"
        fi
      ;;
    *)
      echo "start|stop|status|test"
      ;;
esac
```

## 单机测试 4核8G

开启压力测试，需要调整系统参数 /etc/sysctl.conf，自行搜索吧。
比较理想的压力测试是，在内网服务器A和B上分别运行服务和测试。
打开`-k`指标高，`-c`超过1000，可能瓶颈在ab。

安装 ab

`sudo apt-get install apach2-utls` `yum install httpd-tools`

```
Server Software:        
Server Hostname:        127.0.0.1
Server Port:            9000

Document Path:          /hello-world
Document Length:        27 bytes

Concurrency Level:      1000
Time taken for tests:   1.888 seconds
Complete requests:      35000
Failed requests:        0
Write errors:           0
Keep-Alive requests:    35000
Total transferred:      6103920 bytes
HTML transferred:       947160 bytes
Requests per second:    18540.11 [#/sec] (mean)
Time per request:       53.937 [ms] (mean)
Time per request:       0.054 [ms] (mean, across all concurrent requests)
Transfer rate:          3157.57 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    3  58.2      0    1006
Processing:     0   13   8.7     12      81
Waiting:        0   13   8.7     12      81
Total:          0   17  59.1     12    1040
```