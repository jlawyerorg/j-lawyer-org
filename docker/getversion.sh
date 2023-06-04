
#!/bin/bash
 
# get latest filename in db migrations
# jlversionfile=`ls -1 ../j-lawyer-server-entities/src/java/db/migration | sort -r | head -1`

# does not seem to work with double-digit in the last version component
# jlversionfile=`cd ../j-lawyer-server-entities/src/java/db/migration/ && ls -1 -r *.{java,sql} | head -1`

# does not work when run on github actions - would return the checkout date
# jlversionfile=`cd ../j-lawyer-server-entities/src/java/db/migration/ && ls -1 -t *.{java,sql} | head -1`
 
# cut at double underscore
jlversion=${jlversionfile%__*}
 
# cut first char V
jlversion=${jlversion:1}
 
# replace underscores by dots
jlversion=`echo "$jlversion" | tr _ .`
 
# tadaaa! 
# echo $jlversion
echo 2.4.0.2
 
