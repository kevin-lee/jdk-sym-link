# jdk-sym-link

JDK symbolic link creator for macOS.

## How to install

### Native App
It should work on **Apple Silicon** Mac. For **Intel** Mac, use the [JVM app](#jvm-app) please.
```shell
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/kevin-lee/jdk-sym-link/main/.scripts/install.sh)" 
```


### JVM App
If the native one doesn't work (e.g. Intel Macs), try this JVM one.

> **NOTE**
> 
> It requires JVM, but this tools is to create a symlink for Java JDK.
> 
> So you should already have JVM anyway. 😁
```shell
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/kevin-lee/jdk-sym-link/main/.scripts/install-jvm.sh)" 
```
