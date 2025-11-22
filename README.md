
# 🚀 Цель проекта:
- Разработать и развернуть в Minikube Java-приложение с ИИ-моделью для анализа тональности текста.

# Используемый стек технологий:
- Java 17, Spring Boot 3.5.7
- ONNX Runtime для инференса модели
- Docker, Kubernetes (Minikube)
- Prometheus + Grafana для мониторинга

## Предварительные требования
bash
## Установите необходимые инструменты
- Java 17
- Maven 3.9+
- Docker
- Minikube
- kubectl
  
## Локальный запуск
bash
### Клонируйте репозиторий
- git clone <укажите repository-url>
- cd sentiment-ai-project

### Соберите приложение
- mvn clean package -DskipTests

### Запустите приложение
- java -jar target/sentiment-ai-project-0.0.1.jar

### Приложение будет доступно по адресу: http://localhost:8080

# 📡API Документация
## Анализ тональности текста
http
- GET /api/sentiment?text=Your text here

#### Пример запроса:

bash
- curl "http://localhost:8080/api/sentiment?text=I%20love%20this%20product%20it%20is%20amazing"

#### Пример ответа:

json
{
  "text": "I love this product, it's amazing!",
  "sentiment": "positive",
  "confidence": 0.92,
  "modelUsed": true
}

## Проверка здоровья приложения
http
- curl "http://localhost:8080/api/health"
  
#### Ответ:

json
{
  "service": "Sentiment Analysis API",
  "modelStatus": "LOADED",
    "status": "UP"
}

## Информация о модели
http
- curl "http://localhost:8080/api/model/info"

#### Пример ответа:

json
{
   "numInputs":0,
   "inputSize":128,
   "modelPath":"model.onnx",
   "modelLoaded":false,
   "numOutputs":0
}
  
# ☸️ Развертывание в Kubernetes
## 1. Запуск Minikube кластера
bash
- minikube start --cpus=4 --memory=8192mb --nodes=2
- minikube addons enable ingress
- minikube addons enable metrics-server

## 2. Сборка Docker образа
bash
### Используем Docker демон Minikube
- eval $(minikube docker-env)

### Сборка образа
- docker build -t sentiment-ai-app:1.0.0 .

### Проверка образа
- docker images | grep sentiment-ai-app
  
## 3. Развертывание приложения
bash
### Применение всех конфигураций
- kubectl apply -f kubernetes/deployment.yaml
- kubectl apply -f kubernetes/service.yaml
- kubectl apply -f kubernetes/ingress.yaml
- kubectl apply -f kubernetes/hpa.yaml

### Проверка развертывания
- kubectl get all
  
## 4. Проверка работы
bash
### Получение URL приложения
- minikube service sentiment-ai-service --url

### Тестирование API
- curl "http://<SERVICE-IP>/api/sentiment?text=Hello world"
  
# 📊 Мониторинг
## Установка Prometheus и Grafana
bash
### Добавление Helm репозиториев
- helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
- helm repo update

### Установка стека мониторинга
helm install prometheus prometheus-community/kube-prometheus-stack

### Проброс портов для доступа
- kubectl port-forward svc/prometheus-grafana 3000:80 &
- kubectl port-forward svc/prometheus-kube-prometheus-prometheus 9090:9090 &

### Доступ к интерфейсам
- Grafana: http://localhost:3000 (admin/prom-operator)
- Prometheus: http://localhost:9090

# Метрики приложения
- Приложение предоставляет метрики через Spring Boot Actuator:
bash
## Просмотр всех метрик
- curl http://localhost:8080/actuator/prometheus

## Ключевые метрики:
- sentiment_analysis_requests_total
- sentiment_analysis_duration_milliseconds
- sentiment_analysis_requests_successful
  
# 🔧 Разработка

## Основные компоненты
### SentimentModel
java
// Анализ тональности с использованием ONNX модели
- SentimentResult result = sentimentModel.analyzeWithModel(text);

### ActuatorConfig
java
// Кастомные метрики для мониторинга
- sentiment_analysis_requests_total
- sentiment_analysis_duration_milliseconds
  
# 📈 Автомасштабирование

## Horizontal Pod Autoscaler
yaml

kubernetes/hpa.yaml
minReplicas: 3
maxReplicas: 10
metrics:
- type: Resource
  resource:
    name: cpu
    target:
      type: Utilization
      averageUtilization: 50
  
## Тестирование масштабирования
bash
#### Создание нагрузки
- kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- \
  /bin/sh -c "while sleep 0.01; do wget -q -O- http://sentiment-ai-service/api/sentiment?text=load; done"

#### Наблюдение за HPA
- kubectl get hpa -w

# 🔍 Анализ трендов
## Ключевые технологии
- ONNX Runtime - кроссплатформенная инференс-библиотека
- Spring Boot Actuator - мониторинг и метрики
- Kubernetes HPA - автоматическое масштабирование
- Prometheus + Grafana - observability стек

## Производительность
- Время ответа: < 100ms
- Размер образа: ~150MB
- Потребление памяти: 256-512MB
- Реплики: 3 (автоматически)

## 🐛 Устранение неисправностей
- Распространенные проблемы
- Модель не загружается

## Логи и диагностика
bash
### Просмотр логов
- kubectl logs deployment/sentiment-ai-app -f

### Детальная информация о подах
- kubectl describe pods -l app=sentiment-ai

### Проверка событий кластера
- kubectl get events --sort-by=.metadata.creationTimestamp

#### 👥 Автор - Давлетшина Алла 
