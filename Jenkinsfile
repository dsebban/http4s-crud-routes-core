pipeline {
    agent {
        docker { image 'hseeberger/scala-sbt:8u181_2.12.8_1.2.8"' }
    }
    stages {
        stage('Test') {
            steps {
                sh "sbt test"
            }
        }
    }
}