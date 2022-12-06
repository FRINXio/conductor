import time

from conductor.FrinxConductorWrapper import FrinxConductorWrapper


def _register_workers(conductor) -> None:
    conductor.register('echo', {
        "description": 'echo',
        "timeoutSeconds": 600,
        "responseTimeoutSeconds": 600,
        "inputKeys": [
            "msg"
        ],
        "outputKeys": [
            "msg"
        ]
    }, echo)


def echo(task):
    print("Echo executed!")
    time.sleep(1)
    return {'status': 'COMPLETED', 'output': {'msg': str(task["inputData"]["msg"])},
            'logs': ["Echoed"]}


def main():
    conductor = FrinxConductorWrapper(
        server_url="http://localhost:8080/api",
        headers={"Content-Type": "application/json"},
        max_thread_count=1
    )

    _register_workers(conductor)

    print("Starting workers's threads (this blocks the main thread)")
    conductor.start_workers()


if __name__ == '__main__':
    main()
