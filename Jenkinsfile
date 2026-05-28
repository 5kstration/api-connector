pipeline {
    agent {
        label 'onprem-agent'
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        ansiColor('xterm')
    }

    environment {
        // GitLab Container Registry
        REGISTRY_URL     = 'registry.gitlab.com'
        IMAGE_NAME       = '5kstration/api-connector'
        IMAGE_TAG        = "${BUILD_NUMBER}"
        IMAGE_FULL_NAME  = "${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}"
        IMAGE_LATEST     = "${REGISTRY_URL}/${IMAGE_NAME}:latest"

        // Jenkins credentials
        REGISTRY_CRED_ID = 'gitlab-registry-credentials'
        DEPLOY_SSH_ID    = 'onprem-api-connector-ssh-key'
        DEPLOY_HOST_ID   = 'onprem-api-connector-host'

        // On-premise deploy target
        DEPLOY_USER      = 'deploy'
        DEPLOY_PATH      = '/opt/moneylog/api-connector'
    }

    stages {
        stage('Checkout SCM') {
            steps {
                echo '[Checkout] GitLab repository checkout'
                checkout scm
            }
        }

        stage('Backend Test & Build') {
            steps {
                echo '[Build] Run tests and create Spring Boot JAR'
                sh '''
                    if [ -f "./gradlew" ] && [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
                      chmod +x ./gradlew
                      ./gradlew clean test bootJar --no-daemon
                    else
                      gradle clean test bootJar --no-daemon
                    fi
                '''
            }
        }

        stage('Docker Login') {
            steps {
                echo '[Docker] Login to container registry'
                withCredentials([usernamePassword(credentialsId: "${REGISTRY_CRED_ID}", usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        echo "${REGISTRY_PASSWORD}" | docker login "${REGISTRY_URL}" \
                          --username "${REGISTRY_USERNAME}" \
                          --password-stdin
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo '[Docker] Build and push API-Connector image'
                sh '''
                    docker build -t "${IMAGE_FULL_NAME}" .
                    docker tag "${IMAGE_FULL_NAME}" "${IMAGE_LATEST}"
                    docker push "${IMAGE_FULL_NAME}"
                    docker push "${IMAGE_LATEST}"
                '''
            }
        }

        stage('Deploy to On-Premise Server') {
            steps {
                echo '[Deploy] Pull image and restart docker compose service on on-premise server'
                withCredentials([
                    usernamePassword(credentialsId: "${REGISTRY_CRED_ID}", usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD'),
                    string(credentialsId: "${DEPLOY_HOST_ID}", variable: 'DEPLOY_HOST')
                ]) {
                    sshagent(credentials: ["${DEPLOY_SSH_ID}"]) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no "${DEPLOY_USER}@${DEPLOY_HOST}" "
                              set -e
                              echo '${REGISTRY_PASSWORD}' | docker login '${REGISTRY_URL}' \
                                --username '${REGISTRY_USERNAME}' \
                                --password-stdin
                              cd '${DEPLOY_PATH}'
                              export API_CONNECTOR_IMAGE='${IMAGE_LATEST}'
                              docker compose pull api-connector
                              docker compose up -d api-connector
                            "
                        '''
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                echo '[Health] Check API-Connector actuator health'
                withCredentials([string(credentialsId: "${DEPLOY_HOST_ID}", variable: 'DEPLOY_HOST')]) {
                    sshagent(credentials: ["${DEPLOY_SSH_ID}"]) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no "${DEPLOY_USER}@${DEPLOY_HOST}" "
                              set -e
                              for i in \$(seq 1 20); do
                                if curl -fsS http://localhost:8081/actuator/health > /dev/null; then
                                  echo 'API-Connector health check success'
                                  exit 0
                                fi
                                sleep 3
                              done
                              echo 'API-Connector health check failed'
                              docker compose -f '${DEPLOY_PATH}/docker-compose.yml' logs --tail=100 api-connector || true
                              exit 1
                            "
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo '[Cleanup] Remove local Docker image cache'
            sh '''
                docker rmi "${IMAGE_FULL_NAME}" || true
                docker rmi "${IMAGE_LATEST}" || true
            '''
        }
        success {
            echo '[Success] API-Connector on-premise deployment completed'
        }
        failure {
            echo '[Failure] API-Connector pipeline failed'
        }
    }
}
