#!/bin/bash

echo "_START_" >  ~/macheteChangeLog.txt
cat .git/machete > ~/macheteChangeLog.txt
### Set initial time of file
LTIME=`stat -f %m .git/machete`

while true
do
   ATIME=`stat -f %m .git/machete`

   if [[ "$ATIME" != "$LTIME" ]]
   then
       date +"%T.%3N" >> ~/macheteChangeLog.txt
       cat .git/machete >> ~/macheteChangeLog.txt
       LTIME=$ATIME
   fi
done
