
FROM  rabbitmq:3.7.26-management-alpine
LABEL maintainer="Artem Labazin <artem.labazin@infobip.com>"

HEALTHCHECK --interval=10s --timeout=3s --retries=1 \
  CMD rabbitmq-diagnostics ping --quiet --silent || exit 1

RUN wget -O /opt/rabbitmq/plugins/accept-0.3.5.ez https://github.com/deadtrickster/prometheus_rabbitmq_exporter/releases/download/v3.7.9.1/accept-0.3.5.ez \
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
