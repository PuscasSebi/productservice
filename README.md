# productservice


log insight filter 


fields @timestamp, @message, @logStream, @log
| sort @timestamp desc
| limit 10000
| filter (contextMap.requestId ='0f8d4a40-c24b-4eec-b62b-bd16c774cbd7' or requestId = '0f8d4a40-c24b-4eec-b62b-bd16c774cbd7')