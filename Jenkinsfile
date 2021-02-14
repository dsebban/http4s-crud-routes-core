pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                // Get some code from a GitHub repository
                git(
                url: 'git@github.com:dsebban/http4s-crud-routes-core.git',
                credentialsId: 'dccf6e43094de082da443f771fd5941392efc787',
                branch: "master"
                )

                // To run Maven on a Windows agent, use
                // bat "mvn -Dmaven.test.failure.ignore=true clean package"
                publishChecks detailsURL: 'https://jenkins.eu.vchost.co', name: 'stage reporter', status: 'IN_PROGRESS', summary: 'Checking if need to faila', text: 'ss', title: 'bb'
            }

        }
    }
}