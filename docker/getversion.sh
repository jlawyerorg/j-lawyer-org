
#!/bin/bash
 
# get latest filename in db migrations
# jlversionfile=`ls -1 ../j-lawyer-server-entities/src/java/db/migration | sort -r | head -1`
jlversionfile=`cd ../j-lawyer-server-entities/src/java/db/migration/ && ls -1 -r *.{java,sql} | head -1`
 
# cut at double underscore
jlversion=${jlversionfile%__*}
 
# cut first char V
jlversion=${jlversion:1}
 
# replace underscores by dots
jlversion=`echo "$jlversion" | tr _ .`
 
# tadaaa!
echo $jlversion
 
