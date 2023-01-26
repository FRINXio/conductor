echo $1

while [ true ]
do
	status=`curl http://localhost:8080/api/workflow/$1 | jq '.["status"]'`
	echo "$status"
	if [[ "$status" = "\"RUNNING\"" ]]; then
	  	echo "waiting"
        	sleep 1
		continue
	fi

	if [[ "$status" = "\"COMPLETED\"" ]]; then
                echo "completed"
                break
        fi


        if [[ "$status" == "\"FAILED\"" ]]; then
                echo "FAILED"
                exit 1
        fi

	echo "Unexpected status"
	exit 1
done

endTime=`curl http://localhost:8080/api/workflow/$1 | jq '.["endTime"]'`
startTime=`curl http://localhost:8080/api/workflow/$1 | jq '.["startTime"]'`

echo "$(($endTime-$startTime))"
