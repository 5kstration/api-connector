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
        KUBECONFIG_ID    = 'k8s-kubeconfig'
        K8S_NAMESPACE    = 'service'
        K8S_DEPLOYMENT   = 'api-connector'
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

        stage('Deploy to Kubernetes') {
            steps {
                echo '[Deploy] Apply Kubernetes manifests and roll out API-Connector'
                withCredentials([file(credentialsId: "${KUBECONFIG_ID}", variable: 'KUBECONFIG')]) {
                    sh '''
                        set -e
                        kubectl apply -f k8s/deployment.yaml
                        kubectl apply -f k8s/istio.yaml
                        
                        # kubectl-argo-rollouts ?§Ïπò ?ïÏù∏ Î∞??§Ïö¥Î°úÎìú
                        if ! command -v kubectl-argo-rollouts &> /dev/null; then
                            curl -sLO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
                            chmod +x ./kubectl-argo-rollouts-linux-amd64
                            ARGO_CMD="./kubectl-argo-rollouts-linux-amd64"
                        else
                            ARGO_CMD="kubectl-argo-rollouts"
                        fi
                        
                        # Argo Rollouts ?¥Î?ÏßÄ ?ÖÎç∞?¥Ìä∏
                        $ARGO_CMD set image ${K8S_DEPLOYMENT} \
                          ${K8S_DEPLOYMENT}=${IMAGE_FULL_NAME} \
                          -n ${K8S_NAMESPACE}
                        
                        # Rollout ?ÅÌÉú ?ïÏù∏
                        if ! $ARGO_CMD -n ${K8S_NAMESPACE} status ${K8S_DEPLOYMENT} --timeout=360s; then
                            echo "?ö® Rollout timed out! Fetching debug information..."
                            kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
                            
                            echo "--- Pod Details ---"
                            kubectl describe pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
                            
                            echo "--- Pod Logs ---"
                            kubectl logs -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} --all-containers --tail=50 || true
                            
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                echo '[Health] Check Kubernetes deployment and pod state'
                withCredentials([file(credentialsId: "${KUBECONFIG_ID}", variable: 'KUBECONFIG')]) {
                    sh '''
                        set -e
                        kubectl get rollout ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
                    '''
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
            echo '[Success] API-Connector Kubernetes deployment completed'
            sh """
            curl -H "Content-Type: application/json" \\
                 -d '{"content": "??**Î∞∞Ìè¨ ?±Í≥µ**: ${env.JOB_NAME} [ÎπåÎìú #${env.BUILD_NUMBER}] Î∞∞Ìè¨Í∞Ä Ïπ¥ÎÇòÎ¶?Canary) Î∞©Ïãù?ºÎ°ú ?àÏ†Ñ?òÍ≤å ?ÑÎ£å?òÏóà?µÎãà?? ??"}' \\
                 ""
            """
        }
        failure {
            echo '[Failure] API-Connector pipeline failed'
            sh """
            curl -H "Content-Type: application/json" \\
                 -d '{"content": "?ö® **Î∞∞Ìè¨ ?§Ìå®**: ${env.JOB_NAME} [ÎπåÎìú #${env.BUILD_NUMBER}] ?êÎü¨ Î∞úÏÉù! ?åÏù¥?ÑÎùº??Î°úÍ∑∏Î•??ïÏù∏?¥Ï£º?∏Ïöî."}' \\
                 ""
            """
        }
    }
}
