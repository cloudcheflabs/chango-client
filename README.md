# Chango Client

This is chango client used to insert data to [Chango](https://chango-admin-oci-ui.cloudchef-labs.com) which is SQL Data Lakehouse Cloud Service.


## Chango CLI
Chango CLI is a command line interface to upload CSV, JSON and Excel which are located on local and on S3 bucket to Chango.

### Build Executable Jar
```
mvn -e clean install package;
```

### Install Chango CLI

```
cp target/chango-client-*-executable.jar ~/bin/chango;
chmod +x ~/bin/chango;
```

### Use Chango CLI

Please, see [Chango CLI](https://mykidong.atlassian.net/wiki/x/DoDjgw) for more details.




## Chango Client Library for Java
This java library is used to insert JSON streaming events to Chango.

### Install Java Client Library
Add the following maven dependency.

```
TODO: ...
```

Or, download the following jar and add it to your classpath.
```
curl -L -O https://github.com/cloudcheflabs/chango-client/releases/download/1.1.0/chango-client-1.1.0-executable.jar;
```

### Use Chango Client Library for Java

Please, see [Chango Client API Library](https://mykidong.atlassian.net/wiki/x/KQDfgw) for more details.

