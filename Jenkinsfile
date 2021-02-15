pipeline {
    agent any
      k8s.jnlpMultiPod(
                    IMAGE2: "art.sentinelone.net/docker-remote/hseeberger/scala-sbt:8u181_2.12.8_1.2.8",
                    REQUEST_CPU: "4",
                    BASE_CONTAINER_REQUEST_MEMORY: "3Gi",
                    REQUEST_MEMORY: "10Gi"
            ) { label ->
                node(label) {
                        stage('checkout') {
                            checkout scm
                        }
                        container('container2') {
                            stage('project build') {
                                // publishChecks detailsURL: 'https://jc.s1.guru/job/builds/job/star/job/build-star-spark-app-event-based/job/add-sbt/', name: 'stage reporter', status: 'IN_PROGRESS', summary: 'Checking if need to faila', text: 'ss', title: 'bb'
                                sh("sbt test")
                            }
                        }
    
}
            }
}