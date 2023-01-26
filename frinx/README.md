# Our stuff

## calculateDurationOfWorkflowExecution.sh 

Script to calculate duration of a wf execution. Useful for performance testing.

Example usage

```
./calDuration.sh `curl -X POST http://localhost:8080/api/workflow -d '{ "name": "MainDummy", "input": {"number_of_sub_workflows": 70, "wait_time": 5}}' -H "Content-type: application/json"`
```
