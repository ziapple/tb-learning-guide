--
-- Copyright © 2016-2020 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE KEYSPACE IF NOT EXISTS tb-learning
WITH replication = {
	'class' : 'SimpleStrategy',
	'replication_factor' : 1
};

CREATE TABLE IF NOT EXISTS tb-learning.device (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    label text,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS tb-learning.device_by_tenant_and_name AS
    SELECT *
    from tb-learning.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND name IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, name, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS tb-learning.device_by_tenant_and_search_text AS
    SELECT *
    from tb-learning.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC, customer_id DESC);

