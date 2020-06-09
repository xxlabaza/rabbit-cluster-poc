#!/usr/bin/env bash

CONFIG_FILE="/etc/rabbitmq/rabbitmq.conf"


function create_node_line () {
  local INDEX="${1}";
  local NODE_HOST=`echo ${2} | sed 's/ *$//g'`; # trim the host
  echo "cluster_formation.classic_config.nodes.${INDEX} = rabbit@${NODE_HOST}";
}

function append_node_lines () {
  local COUNT=1
  while IFS=',' read -ra HOSTS; do
    for ITEM in "${HOSTS[@]}"; do
      local LINE=$(create_node_line ${COUNT} ${ITEM});
      echo "${LINE}" >> "${CONFIG_FILE}"

      COUNT=`expr ${COUNT} + 1`;
    done
  done <<< "${X_RABBITMQ_HOSTS}"
}


# check the ${X_RABBITMQ_HOSTS} line in the ${CONFIG_FILE}
# remove it, if exists, and generate the node hosts lines
if grep -q '${X_RABBITMQ_HOSTS}' "${CONFIG_FILE}"; then
  sed -i -e "s/\${X_RABBITMQ_HOSTS}/ /g" "${CONFIG_FILE}"
  # generate the host lines and append them to the ${CONFIG_FILE}
  append_node_lines
else
  echo "There is no \${X_RABBITMQ_HOSTS} in ${CONFIG_FILE}";
fi

# execute default image's entrypoint
exec docker-entrypoint.sh ${@}
