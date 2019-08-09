# data2services-download-java

## Docker build

```shell
docker build -t data2services-download-java .
```

## Docker run

```shell
docker run -it --rm data2services-download-java -h

docker run -it --rm -v /data:/data data2services-download-java -ds pharmgkb -dp "/data/cwl-repository/input/pharmgkb/" -dcsv /data/datasets.csv
```

## Parameters

```shell
java -jar data2services-download-java-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -ds pharmgkb \ # first col of the CSV (to ddl)
  -dp "/data/cwl-repository/input/pharmgkb/" \ # where to ddl
  -dcsv ../datasets.csv
```

