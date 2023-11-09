# Chango Client

This is chango client used to insert data to [Chango](https://chango-admin-oci-ui.cloudchef-labs.com) which is SQL Data Lakehouse Cloud Service.


## Chango CLI
Chango CLI is a command line interface to upload CSV, JSON and Excel which are located on local to Chango.

### Build Executable Jar
```
mvn -e clean install package;
```

### Install Chango CLI

```
cp target/chango-client-*-executable.jar ~/bin/chango;
chmod +x ~/bin/chango;
```


## Chango Client Library for Java
This java library is used to insert JSON streaming events to Chango.

### Install Java Client Library
Add the following maven dependency.

```
<dependency>
  <groupId>co.cloudcheflabs.chango</groupId>
  <artifactId>chango-client</artifactId>
  <version>2.0.1</version>
</dependency>
```
