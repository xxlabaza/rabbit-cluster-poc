
FROM  rabbitmq:3.7.26-management-alpine
LABEL maintainer="Artem Labazin <artem.labazin@infobip.com>"

HEALTHCHECK --interval=10s --timeout=5s --retries=1 \
  CMD rabbitmq-diagnostics ping --quiet --silent || exit 1

ENV LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8 \
    LC_CTYPE=en_US.UTF-8 \
    LC_COLLATE=en_US.UTF-8 \
    LC_MESSAGES=en_US.UTF-8 \
    LC_NUMERIC=en_US.UTF-8 \
    LC_TIME=en_US.UTF-8 \
    LC_MONETARY=en_US.UTF-8 \
    LC_PAPER=en_US.UTF-8 \
    LC_IDENTIFICATION=en_US.UTF-8 \
    LC_NAME=en_US.UTF-8 \
    LC_ADDRESS=en_US.UTF-8 \
    LC_TELEPHONE=en_US.UTF-8 \
    LC_MEASUREMENT=en_US.UTF-8 \
    TZ=Europe/Moscow

RUN apk add --no-cache --no-progress tzdata \
    && echo "$TZ" > /etc/timezone \
    && cp "/usr/share/zoneinfo/$TZ" /etc/localtime \
    \
    && wget -O /opt/rabbitmq/plugins/accept-0.3.5.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/accept-0.3.5.ez \
    && wget -O /opt/rabbitmq/plugins/prometheus-4.3.0.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/prometheus-4.3.0.ez \
    && wget -O /opt/rabbitmq/plugins/prometheus_cowboy-0.1.7.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/prometheus_cowboy-0.1.7.ez \
    && wget -O /opt/rabbitmq/plugins/prometheus_httpd-2.1.10.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/prometheus_httpd-2.1.10.ez \
    && wget -O /opt/rabbitmq/plugins/prometheus_process_collector-1.4.3.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/prometheus_process_collector-1.4.3.ez \
    && wget -O /opt/rabbitmq/plugins/prometheus_rabbitmq_exporter-3.7.9.1.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/prometheus_rabbitmq_exporter-3.7.9.1.ez \
    \
    && rabbitmq-plugins enable --offline \
       rabbitmq_top \
       prometheus_rabbitmq_exporter

COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
COPY definitions.json /etc/rabbitmq/additional-definitions.json
COPY custom-docker-entrypoint.sh /usr/local/bin/

ENTRYPOINT ["custom-docker-entrypoint.sh"]
CMD ["rabbitmq-server"]
